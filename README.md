# AI Language Learning

Hệ thống học ngoại ngữ ứng dụng AI, tương tác bằng văn bản và giọng nói, gồm backend API và frontend web. Robot giao tiếp vật lý có thể kết nối qua REST API và WebSocket.

## Mục lục

- [Tổng quan](#tổng-quan)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc](#kiến-trúc)
- [Cài đặt & chạy dự án](#cài-đặt--chạy-dự-án)
- [Danh sách API](#danh-sách-api)
- [Trạng thái các chức năng](#trạng-thái-các-chức-năng)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)

## Tổng quan

Mục tiêu của hệ thống là giúp người học luyện ngoại ngữ thông qua hội thoại với AI bằng giọng nói: nói chuyện với AI, được sửa lỗi phát âm/ngữ pháp, luyện tập theo chủ đề, và theo dõi tiến bộ theo thời gian. Hệ thống được chia thành các module độc lập:

| Nhóm | Mô tả |
|---|---|
| AUTH | Đăng ký, đăng nhập, quản lý tài khoản, phân quyền |
| SYSTEM | Quản lý phiên hội thoại, chủ đề, cấu hình AI |
| LEARN | Xác định trình độ, quản lý từ vựng, luyện ngữ pháp/nghe/nói/phát âm/hội thoại |
| AI | Hội thoại với AI, trả lời câu hỏi, sửa lỗi, đánh giá |
| STT / TTS | Chuyển giọng nói ↔ văn bản |
| SPEECH | Đánh giá và chấm điểm phát âm |
| SCORE | Chấm điểm ngữ pháp, từ vựng, hội thoại |
| PROGRESS | Lưu lịch sử học tập, theo dõi tiến bộ |
| ROBOT | Kết nối và điều khiển robot giao tiếp |

Xem chi tiết trạng thái từng chức năng ở [phần bên dưới](#trạng-thái-các-chức-năng).

## Công nghệ sử dụng

- **Backend**: Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA
- **Frontend**: React, TypeScript, Vite
- **Database**: PostgreSQL 16, quản lý schema bằng Liquibase
- **AI**: kiến trúc provider trung lập (`AIProvider`) — Ollama local dùng **Qwen 3 1.7B cho hội thoại** và **4B cho tác vụ AI có cấu trúc**, có sẵn adapter Gemini (cloud) và Mock (test)
- **Hạ tầng**: Docker Compose (PostgreSQL + pgAdmin tùy chọn)
- **Khác**: Lombok, BCrypt, jjwt, Bean Validation (`@Valid`)

## Kiến trúc

Mỗi chức năng được tổ chức theo mô hình phân lớp nhất quán:

```
Controller → Service → Repository → PostgreSQL
```

Một số quyết định thiết kế đáng chú ý:

- **`ApiResponse<T>`**: toàn bộ response API có cùng cấu trúc `{ success, message, data, timestamp }`.
- **`GlobalExceptionHandler`**: xử lý lỗi tập trung — exception nghiệp vụ (404/409/400/401), lỗi validate, lỗi xác thực, lỗi từ AI provider (502) đều trả về đúng mã HTTP và cấu trúc thống nhất, không có `RuntimeException` trả lỗi 500 mặc định.
- **Liquibase thay `ddl-auto=update`**: schema database được quản lý bằng changelog (`db/changelog/`), có lịch sử rõ ràng, không để Hibernate tự sinh bảng.
- **`AIProvider` interface**: các service AI không phụ thuộc trực tiếp vào Ollama/Gemini cụ thể — đổi provider chỉ cần đổi biến môi trường `AI_PROVIDER`, không sửa code.
- **Ngữ cảnh hội thoại (AI-03) không nằm ở model**: mỗi lần gọi AI, hệ thống gửi tối đa 8 tin nhắn gần nhất; nội dung suy luận bị chặn không được phát lại trong ngữ cảnh hoặc trả về lịch sử.
- **`SpeechToTextProvider`/`TextToSpeechProvider` interface**: STT, TTS và chấm phát âm có thể cấu hình provider riêng. Hỗ trợ Azure/Mock; STT local qua whisper.cpp và TTS local qua Piper. Kết quả nhận diện giọng nói (STT-03) không lưu vào bảng riêng — văn bản sau khi transcribe được lưu thẳng vào `conversation_messages` giống tin nhắn gõ tay, không phân biệt nguồn gốc.
- **Pronunciation Assessment (SPEECH-01→04) dùng chung endpoint với STT**: chỉ thêm header `Pronunciation-Assessment` (JSON base64) vào cùng request nhận diện giọng nói của Azure, không phải API riêng biệt. Lời khuyên cải thiện (SPEECH-05) tái dùng `AIProvider` để biến điểm số thô thành phản hồi tự nhiên, không phải chuỗi mẫu dựng sẵn.
- **Điểm tổng hợp (SCORE-04) không để AI tự cộng**: AI chỉ chấm 3 điểm thành phần (ngữ pháp/từ vựng/hội thoại), `overallScore` là trung bình cộng do code tính — tránh trường hợp AI cộng/làm tròn sai, đảm bảo luôn nhất quán với 3 điểm kia.
- **PROGRESS-01/02 không cần code riêng**: lịch sử học tập/hội thoại đã tự động được lưu từ lúc các module trước ghi dữ liệu (session, tin nhắn, điểm số) — `ProgressService` chỉ đọc lại và tổng hợp, không có bảng hay logic lưu trữ mới nào cho 2 mục này.
- **Bài tập LEARN-03/04 không lưu DB**: sinh đề và chấm đều stateless — lúc chấm, client tự gửi lại nguyên văn câu hỏi/đoạn văn kèm câu trả lời, AI chấm trực tiếp thay vì so với đáp án lưu sẵn. Đơn giản hơn, và sinh đề với chấm đề không bao giờ lệch pha nhau.
- **LEARN-08 không để AI chọn ID**: AI chỉ viết đoạn lời khuyên bằng lời — chủ đề/từ vựng đề xuất cụ thể luôn đến từ query SQL thật (topic/từ chưa từng dùng/học), tránh rủi ro AI bịa ra ID không tồn tại trong DB.
- **Robot dùng chung JWT với user thường**, không có hệ thống credential riêng — đơn giản hóa, đổi lại robot "là" tài khoản đã đăng ký nó, không phải 1 danh tính độc lập.
- **Kênh WebSocket (ROBOT-06) dùng giao thức text thuần**, không phải JSON/STOMP — vì phía robot là 1 script Python nhỏ, không phải client REST khác. Handshake tự xác thực JWT và truy vấn quyền sở hữu robot trực tiếp trong DB; kiểm thử hồi quy bao phủ thiếu/sai token, thiết bị không tồn tại, đúng chủ sở hữu và sai chủ sở hữu.


- **Soft delete**: các danh mục dùng chung (chủ đề, từ vựng) dùng cờ `active` thay vì xóa cứng, tránh phá dữ liệu lịch sử đã tham chiếu tới chúng.

## Cài đặt & chạy dự án

### Yêu cầu

- Java 21
- Docker (chạy PostgreSQL)
- [Ollama](https://ollama.com) (nếu muốn dùng tính năng AI với provider mặc định)
- Docker (cần thêm nếu muốn dùng STT/TTS local qua whisper.cpp/Piper)
- Tài khoản Azure AI Speech (chỉ cần nếu muốn dùng STT/TTS/chấm phát âm Azure — mặc định `SPEECH_PROVIDER=mock`)
- Maven (dùng kèm `mvnw` có sẵn trong repo, không cần cài riêng)

### Các bước

1. **Clone repo và tạo file cấu hình:**

   ```bash
   git clone <repo-url>
   cd ai-language-learning
   cp .env.example .env
   ```

   Mở `.env` và điền giá trị thật, đặc biệt là `JWT_SECRET` (chuỗi ngẫu nhiên dài, ví dụ `openssl rand -base64 48`).

2. **Chạy PostgreSQL bằng Docker Compose:**

   ```bash
   docker compose up -d
   ```

   Để bật nhận dạng và phát giọng nói hoàn toàn cục bộ, thêm vào `.env`:

   ```dotenv
   SPEECH_STT_PROVIDER=whisper
   SPEECH_TTS_PROVIDER=piper
   SPEECH_ASSESSMENT_PROVIDER=mock
   ```

   Sau đó khởi động hai dịch vụ speech (lần đầu sẽ tải model Piper và Whisper):

   ```powershell
   docker compose --profile local-speech up -d piper whisper
   ```

   Piper cung cấp TTS tại `http://localhost:5001`; whisper.cpp cung cấp STT tại `http://localhost:5002`. Model được giữ trong Docker volumes qua các lần chạy. Giọng Piper mặc định là English. Đổi `PIPER_VOICE` để dùng model giọng khác. Chấm phát âm vẫn ở chế độ `mock` (điểm minh họa), vì whisper.cpp không cung cấp chấm lỗi phát âm như Azure.

   Xem database qua giao diện web (tùy chọn): `docker compose --profile tools up -d` rồi mở pgAdmin tại `http://localhost:5050`.

3. **(Tùy chọn) Chuẩn bị Ollama nếu muốn dùng AI:**

   ```bash
   ollama serve
   ollama pull qwen3:1.7b
   ollama pull qwen3:4b
   ```

   Không có Ollama vẫn chạy được toàn bộ API khác bình thường — chỉ các endpoint gọi AI mới cần.

4. **Chạy backend:**

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   Nhớ nạp các biến trong `.env` vào môi trường chạy (IntelliJ Run Configuration → Environment variables, hoặc export thủ công trước khi chạy `mvnw`).

   Ứng dụng chạy tại `http://localhost:8080`. Khi khởi động lần đầu, Liquibase tự tạo schema và một tài khoản `ADMIN` mặc định (`ADMIN_USERNAME`/`ADMIN_PASSWORD` trong `.env`) được tự động seed sẵn — đăng nhập ngay bằng `POST /api/auth/login`, không cần đăng ký thủ công.

5. **Chạy frontend trong cửa sổ terminal khác:**

   ```bash
   cd frontend
   pnpm install
   pnpm dev
   ```

   Mở địa chỉ Vite hiển thị trong terminal (mặc định `http://localhost:5173`). Frontend proxy `/api` và `/ws` đến backend `http://localhost:8080`. Nếu backend chạy trên máy khác, tạo `frontend/.env.local` theo `frontend/.env.example`, đặt `VITE_BACKEND_URL` thành địa chỉ máy backend và khởi động lại Vite. Cần để backend chạy trước khi đăng nhập. Tính năng AI cần provider AI hoạt động; hội thoại giọng nói yêu cầu quyền truy cập microphone.

## Danh sách API

Tất cả response đều có dạng `{ success, message, data, timestamp }`. Cột **Quyền** ghi `Public` (không cần đăng nhập), `User` (cần JWT), hoặc `Admin` (cần JWT + role ADMIN).

### AUTH

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Đăng ký tài khoản |
| POST | `/api/auth/login` | Public | Đăng nhập, nhận JWT |
| POST | `/api/auth/logout` | User | Đăng xuất |
| GET | `/api/users/me` | User | Xem thông tin cá nhân |
| PUT | `/api/users/me` | User | Cập nhật thông tin cá nhân |
| PUT | `/api/users/me/password` | User | Đổi mật khẩu |
| GET | `/api/admin/users` | Admin | Danh sách toàn bộ user |

### SYSTEM

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/topics` | User | Danh sách chủ đề đang hoạt động |
| GET | `/api/topics/{id}` | User | Chi tiết 1 chủ đề |
| GET | `/api/topics/admin/all` | Admin | Toàn bộ chủ đề, kể cả đã ẩn |
| POST | `/api/topics` | Admin | Tạo chủ đề |
| PUT | `/api/topics/{id}` | Admin | Sửa chủ đề |
| DELETE | `/api/topics/{id}` | Admin | Ẩn chủ đề (soft delete) |
| GET | `/api/admin/ai-configs` | Admin | Danh sách cấu hình AI |
| GET | `/api/admin/ai-configs/{id}` | Admin | Chi tiết 1 cấu hình |
| POST | `/api/admin/ai-configs` | Admin | Tạo cấu hình AI |
| PUT | `/api/admin/ai-configs/{id}` | Admin | Sửa cấu hình AI |
| PUT | `/api/admin/ai-configs/{id}/activate` | Admin | Kích hoạt cấu hình (chỉ 1 cấu hình active tại 1 thời điểm) |
| DELETE | `/api/admin/ai-configs/{id}` | Admin | Xóa cấu hình AI |
| POST | `/api/sessions` | User | Bắt đầu phiên hội thoại mới |
| PUT | `/api/sessions/{id}/end` | User | Kết thúc phiên hội thoại |
| GET | `/api/sessions/me` | User | Danh sách phiên của tôi |
| GET | `/api/sessions/{id}` | User | Chi tiết 1 phiên (chỉ chủ sở hữu) |

### LEARN

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/learner-level/me` | User | Xem trình độ hiện tại |
| PUT | `/api/learner-level/me` | User | Tự đánh giá trình độ (CEFR A1–C2) |
| GET | `/api/vocabulary` | User | Danh sách từ vựng (lọc theo `topicId`, `level`) |
| GET | `/api/vocabulary/{id}` | User | Chi tiết 1 từ |
| GET | `/api/vocabulary/admin/all` | Admin | Toàn bộ từ vựng, kể cả đã ẩn |
| POST | `/api/vocabulary` | Admin | Thêm từ vựng |
| PUT | `/api/vocabulary/{id}` | Admin | Sửa từ vựng |
| DELETE | `/api/vocabulary/{id}` | Admin | Ẩn từ vựng (soft delete) |

### AI

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/sessions/{id}/messages` | User | Gửi tin nhắn trong phiên, nhận lại câu trả lời của AI (AI-01, AI-02) — tự điều chỉnh độ khó theo trình độ người học (AI-04) |
| GET | `/api/sessions/{id}/messages` | User | Xem lịch sử tin nhắn của phiên (chỉ chủ sở hữu) |
| POST | `/api/ai/grammar-check` | User | Sửa lỗi ngữ pháp cho 1 câu/đoạn văn bất kỳ, độc lập với session (AI-05) |
| POST | `/api/ai/suggest-expression` | User | Gợi ý cách diễn đạt tự nhiên/đa dạng hơn cho 1 câu (AI-06) |
| POST | `/api/ai/analyze-answer` | User | Phân tích câu trả lời cho 1 câu hỏi — độ liên quan, ngữ pháp, từ vựng (AI-07) |
| POST | `/api/sessions/{id}/evaluate` | User | Đánh giá tổng thể 1 phiên hội thoại đã kết thúc (AI-08) |
| POST | `/api/admin/ai/test-chat` | Admin | Test thủ công provider AI đang active (Ollama/Gemini/Mock), không qua session |

### STT / TTS / SPEECH

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/speech/transcribe` | User | Upload audio (WAV/PCM/16kHz/mono), nhận lại văn bản — độc lập với session (STT-02) |
| POST | `/api/speech/synthesize` | User | Văn bản → audio (MP3), chọn giọng/ngôn ngữ tùy ý — độc lập với session (TTS-01/02/03) |
| POST | `/api/sessions/{id}/voice-messages` | User | Gửi tin nhắn bằng giọng nói: transcribe → lưu như tin nhắn thường → nhận lại câu trả lời text của AI (STT-03, STT-04) |
| GET | `/api/sessions/{id}/messages/{messageId}/audio` | User | Lấy audio (MP3) của 1 tin nhắn đã lưu trong phiên — thường dùng để phát câu trả lời của AI (TTS-04) |
| POST | `/api/speech/assess-pronunciation` | User | Upload audio + câu tham chiếu (`referenceText`), nhận lại điểm accuracy/fluency/completeness/overall, lỗi phát âm theo từng từ, và lời khuyên cải thiện do AI sinh ra (SPEECH-01→05) |

### SCORE

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/sessions/{id}/score` | User | Chấm điểm ngữ pháp/từ vựng/hội thoại cho 1 phiên đã kết thúc, tính điểm tổng hợp (SCORE-01→04). Gọi lại sẽ chấm lại, ghi đè điểm cũ |
| GET | `/api/sessions/{id}/score` | User | Xem điểm đã chấm của 1 phiên (404 nếu chưa chấm lần nào) |

### PROGRESS

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| PUT | `/api/progress/vocabulary/{vocabularyId}` | User | Đánh dấu 1 từ là `LEARNING` hoặc `LEARNED` (PROGRESS-04) |
| GET | `/api/progress/vocabulary` | User | Danh sách từ đang theo dõi, lọc được theo `status` |
| DELETE | `/api/progress/vocabulary/{vocabularyId}` | User | Bỏ theo dõi 1 từ |
| GET | `/api/progress/summary` | User | Thống kê tổng hợp: tổng số phiên, tổng tin nhắn đã gửi, điểm trung bình, trình độ hiện tại, số từ đã học, và lịch sử điểm theo thời gian (PROGRESS-05, 06) |

### LEARN nâng cao (LEARN-03 → LEARN-08)

Body của các endpoint sinh đề (`GenerateExerciseRequest`) đều nhận `{"level": "B1", "topicId": 3}` — cả 2 field tùy chọn: thiếu `level` sẽ dùng trình độ hiện tại của người học (LEARN-01), thiếu cả 2 sẽ mặc định `A2`.

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/learn/grammar/exercise` | User | Sinh 1 câu hỏi ngữ pháp theo trình độ/chủ đề (LEARN-03) |
| POST | `/api/learn/grammar/exercise/check` | User | Chấm câu trả lời cho câu hỏi ngữ pháp vừa sinh |
| POST | `/api/learn/listening/exercise` | User | Sinh 1 đoạn văn ngắn + câu hỏi hiểu bài (LEARN-04). Lấy audio đoạn văn qua `/api/speech/synthesize` |
| POST | `/api/learn/listening/exercise/check` | User | Chấm câu trả lời cho bài nghe hiểu |
| POST | `/api/learn/speaking/prompt` | User | Sinh 1 đề bài luyện nói (LEARN-05). Chấm bằng cách ghi âm → `/api/speech/transcribe` → `/api/ai/analyze-answer` |
| POST | `/api/learn/pronunciation/prompt` | User | Sinh 1 câu luyện phát âm theo trình độ (LEARN-06). Chấm bằng `/api/speech/assess-pronunciation` |
| GET | `/api/learn/recommendations` | User | Đề xuất chủ đề/từ vựng nên học tiếp + lời khuyên cá nhân hóa dựa trên tiến độ thật (LEARN-08) |

> **LEARN-07** (hội thoại theo chủ đề) không có endpoint riêng — chính là `POST /api/sessions` kèm `topicId` (đã có từ SYSTEM-01).

### ROBOT

Robot xác thực bằng **JWT user thường** (không có cơ chế credential riêng) — tài khoản nào đăng ký robot thì tài khoản đó sở hữu và điều khiển robot đó.

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/robots/register` | User | Đăng ký (hoặc làm mới) 1 robot bằng `deviceId` — gọi lại nhiều lần với cùng `deviceId` không lỗi (ROBOT-01) |
| GET | `/api/robots` | User | Danh sách robot của tôi |
| GET | `/api/robots/{id}` | User | Xem trạng thái 1 robot (ROBOT-07) |
| PUT | `/api/robots/{id}/status` | User | Đổi trạng thái + session đang relay (ROBOT-08) |
| WS | `/ws/robots/{deviceId}` | User (JWT qua header `Authorization` lúc handshake) | Kênh realtime — robot gửi trạng thái dạng text thuần (`IDLE`/`LISTENING`/`SPEAKING`...), server có thể đẩy lệnh xuống (ROBOT-06) |

> **ROBOT-02→05** (thu âm, gửi lên server, nhận phản hồi AI, phát âm thanh) không có API riêng — robot chỉ là 1 client gọi lại đúng `POST /api/sessions/{id}/voice-messages` và `GET /api/sessions/{id}/messages/{messageId}/audio` đã có từ STT-04/TTS-04.

## Trạng thái các chức năng

- [x] AUTH-01 → AUTH-06: Đăng ký, đăng nhập, đăng xuất, quản lý cá nhân, đổi mật khẩu, phân quyền
- [x] SYSTEM-01 → SYSTEM-03: Phiên hội thoại, chủ đề, cấu hình AI
- [x] LEARN-01, LEARN-02: Xác định trình độ, quản lý từ vựng
- [x] Hạ tầng AI: `AIProvider` interface, Ollama (mặc định), Gemini (cloud), Mock (test)
- [x] AI-01, AI-02, AI-03: Hội thoại với AI, trả lời câu hỏi, duy trì ngữ cảnh (dựa trên lịch sử tin nhắn lưu trong DB)
- [x] AI-04: Điều chỉnh độ khó theo trình độ người học (LEARN-01)
- [x] AI-05: Sửa lỗi ngữ pháp (endpoint độc lập, không cần session)
- [x] AI-06: Gợi ý cách diễn đạt tự nhiên/đa dạng hơn
- [x] AI-07: Phân tích câu trả lời (độ liên quan, ngữ pháp, từ vựng, đề xuất cải thiện)
- [x] AI-08: Đánh giá kết quả học tập của 1 phiên hội thoại đã kết thúc (định tính — chưa có điểm số, xem SCORE)
- [~] Hạ tầng Speech: adapter Azure/Mock và local Piper/whisper.cpp đã có; cần bật Docker profile `local-speech` để dùng STT/TTS local. Chấm phát âm cần Azure để có điểm thật.
- [~] STT-01 → STT-04: luồng API/thu âm đã nối; Mock chỉ trả văn bản placeholder, chưa nhận dạng lời nói thật.
- [~] TTS-01 → TTS-04: API và adapter Azure đã có; Mock trả bytes placeholder, không phải MP3 phát được.
- [~] SPEECH-01 → SPEECH-05: adapter chấm phát âm Azure đã có; kết quả Mock không đại diện chất lượng chấm thực tế.
- [x] SCORE-01 → SCORE-04: Chấm điểm ngữ pháp, từ vựng, hội thoại (AI), điểm tổng hợp (tính trong code, không nhờ AI cộng)
- [x] PROGRESS-01 → PROGRESS-06: Lưu lịch sử học tập/hội thoại (tự động, không cần API riêng), theo dõi điểm số theo thời gian, theo dõi từ vựng đã học, thống kê tổng hợp
- [x] LEARN-03 → LEARN-08: Luyện ngữ pháp, nghe, nói, phát âm (sinh đề + chấm theo trình độ), hội thoại theo chủ đề (dùng lại SYSTEM-01), đề xuất nội dung học tập
- [~] ROBOT-01 → ROBOT-08: REST và WebSocket đã triển khai; handshake có kiểm thử hồi quy. Cần kiểm tra thêm với script/phần cứng robot thật; các luồng audio phụ thuộc Speech provider thật.

> Các dấu `[x]` phản ánh chức năng đã có triển khai; những mục `[~]` còn phụ thuộc dịch vụ ngoài hoặc phần cứng để xác nhận chất lượng end-to-end. Backend hiện chỉ có kiểm thử tự động tập trung cho một số luồng WebSocket và kiểm tra khởi động ứng dụng; xem [TESTING-GUIDE.md](TESTING-GUIDE.md) để chạy kiểm thử API thủ công.

## Cấu trúc thư mục

```
ai-language-learning/
├── docker-compose.yml          # PostgreSQL + pgAdmin (tùy chọn)
├── .env.example                 # Mẫu biến môi trường
├── frontend/                    # React + TypeScript + Vite
└── backend/
    └── src/main/
        ├── java/com/example/backend/
        │   ├── ai/              # AIProvider interface + implementation (Ollama/Gemini/Mock)
        │   ├── common/           # ApiResponse
        │   ├── config/           # SecurityConfig, AdminSeeder, AIProviderConfig, SpeechProviderConfig, WebSocketConfig
        │   ├── controller/       # REST controllers
        │   ├── dto/              # request/response DTO
        │   ├── entity/           # JPA entity
        │   ├── exception/        # Custom exception + GlobalExceptionHandler
        │   ├── repository/       # Spring Data JPA repository
        │   ├── security/         # JWT filter/service
        │   ├── service/          # Business logic
        │   ├── speech/           # Speech providers (Azure, Mock, Piper, whisper.cpp)
        │   └── ws/               # RobotWebSocketHandler + handshake auth (ROBOT-06)
        └── resources/
            ├── application.properties
            └── db/changelog/     # Liquibase changelog (1 file / bảng)
```

---

*Người thực hiện: Tưởng Đức Tâm*
