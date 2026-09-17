# AI Language Learning – Backend

Hệ thống học ngoại ngữ ứng dụng AI, tương tác chủ yếu qua giọng nói, được thiết kế để sau này kết hợp với frontend và robot giao tiếp vật lý. Repo này là **backend API**, xây dựng độc lập trước, frontend và robot sẽ tích hợp vào sau qua REST API.

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
- **Database**: PostgreSQL 16, quản lý schema bằng Liquibase
- **AI**: kiến trúc provider trung lập (`AIProvider`) — mặc định chạy **Ollama + Qwen 3 4B** (local, không cần API key), có sẵn adapter **Gemini** (cloud) và **Mock** (test)
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
- **Ngữ cảnh hội thoại (AI-03) không nằm ở model**: mỗi lần gọi AI, hệ thống tự load lại toàn bộ lịch sử tin nhắn của session từ DB rồi gửi kèm — bản thân `AIProvider`/Ollama hoàn toàn stateless giữa các lần gọi.
- **`SpeechToTextProvider`/`TextToSpeechProvider` interface**: cùng pattern với `AIProvider` — đổi provider (Azure/Mock) chỉ cần đổi `SPEECH_PROVIDER`, không sửa code. Kết quả nhận diện giọng nói (STT-03) không lưu vào bảng riêng — văn bản sau khi transcribe được lưu thẳng vào `conversation_messages` giống tin nhắn gõ tay, không phân biệt nguồn gốc.
- **Pronunciation Assessment (SPEECH-01→04) dùng chung endpoint với STT**: chỉ thêm header `Pronunciation-Assessment` (JSON base64) vào cùng request nhận diện giọng nói của Azure, không phải API riêng biệt. Lời khuyên cải thiện (SPEECH-05) tái dùng `AIProvider` để biến điểm số thô thành phản hồi tự nhiên, không phải chuỗi mẫu dựng sẵn.
- **Soft delete**: các danh mục dùng chung (chủ đề, từ vựng) dùng cờ `active` thay vì xóa cứng, tránh phá dữ liệu lịch sử đã tham chiếu tới chúng.

## Cài đặt & chạy dự án

### Yêu cầu

- Java 21
- Docker (chạy PostgreSQL)
- [Ollama](https://ollama.com) (nếu muốn dùng tính năng AI với provider mặc định)
- Tài khoản Azure AI Speech (chỉ cần nếu muốn dùng thật STT/TTS — mặc định `SPEECH_PROVIDER=mock` nên không bắt buộc để chạy app)
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

   Xem database qua giao diện web (tùy chọn): `docker compose --profile tools up -d` rồi mở pgAdmin tại `http://localhost:5050`.

3. **(Tùy chọn) Chuẩn bị Ollama nếu muốn dùng AI:**

   ```bash
   ollama serve
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
- [x] Hạ tầng Speech: `SpeechToTextProvider`/`TextToSpeechProvider` interface, Azure AI Speech (mặc định khi có key), Mock (test)
- [x] STT-01 → STT-04: Thu âm (client-side), chuyển giọng nói thành văn bản, lưu kết quả (tái dùng bảng tin nhắn), xử lý hội thoại bằng giọng nói
- [x] TTS-01 → TTS-04: Chuyển văn bản thành giọng nói, chọn giọng đọc, chọn ngôn ngữ, phát âm thanh phản hồi AI
- [x] SPEECH-01 → SPEECH-05: Đánh giá, chấm điểm, độ trôi chảy, phát hiện lỗi phát âm theo từng từ, hướng dẫn cải thiện (AI-generated)
- [ ] SCORE-01 → SCORE-04
- [ ] PROGRESS-01 → PROGRESS-06
- [ ] LEARN-03 → LEARN-08: Ngữ pháp, nghe, nói, phát âm, hội thoại theo chủ đề, đề xuất nội dung
- [ ] ROBOT-01 → ROBOT-08

## Cấu trúc thư mục

```
ai-language-learning/
├── docker-compose.yml          # PostgreSQL + pgAdmin (tùy chọn)
├── .env.example                 # Mẫu biến môi trường
└── backend/
    └── src/main/
        ├── java/com/example/backend/
        │   ├── ai/              # AIProvider interface + implementation (Ollama/Gemini/Mock)
        │   ├── common/           # ApiResponse
        │   ├── config/           # SecurityConfig, AdminSeeder, AIProviderConfig
        │   ├── controller/       # REST controllers
        │   ├── dto/              # request/response DTO
        │   ├── entity/           # JPA entity
        │   ├── exception/        # Custom exception + GlobalExceptionHandler
        │   ├── repository/       # Spring Data JPA repository
        │   ├── security/         # JWT filter/service
        │   └── service/          # Business logic
        └── resources/
            ├── application.properties
            └── db/changelog/     # Liquibase changelog (1 file / bảng)
```

---

*Người thực hiện: Tưởng Đức Tâm*
