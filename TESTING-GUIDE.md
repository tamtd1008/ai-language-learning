# Checklist kiểm thử API — SYSTEM, AI, LEARN

Test theo đúng thứ tự bên dưới vì các module phụ thuộc dữ liệu lẫn nhau (Vocabulary cần Topic tồn tại, Session cần AiConfig đã active, gửi tin nhắn cần Session đang active...). Dùng Insomnia/Postman, base URL `http://localhost:8080`.

**Chuẩn bị:** đăng nhập lấy JWT của cả 2 tài khoản, dùng suốt các bước dưới:
- `POST /api/auth/login` với `admin` / mật khẩu trong `.env` → lưu lại làm `{{admin_token}}`
- `POST /api/auth/login` với 1 user thường (đăng ký trước nếu chưa có qua `/api/auth/register`) → lưu làm `{{user_token}}`

Mọi request bên dưới (trừ login) cần header `Authorization: Bearer <token>`.

## Danh sách nhanh (theo đúng thứ tự test)

| # | Nhóm | Method | Endpoint |
|---|---|---|---|
| 1 | Topic | POST | `/api/topics` |
| 2 | Topic | GET | `/api/topics` |
| 3 | Topic | GET | `/api/topics/{id}` |
| 4 | Topic | PUT | `/api/topics/{id}` |
| 5 | Topic | GET | `/api/topics/admin/all` |
| 6 | Topic | DELETE | `/api/topics/{id}` |
| 7 | AI Config | POST | `/api/admin/ai-configs` |
| 8 | AI Config | GET | `/api/admin/ai-configs` |
| 9 | AI Config | PUT | `/api/admin/ai-configs/{id}/activate` |
| 10 | AI Config | DELETE | `/api/admin/ai-configs/{id}` |
| 11 | Session | POST | `/api/sessions` |
| 12 | Session | GET | `/api/sessions/me` |
| 13 | Session | GET | `/api/sessions/{id}` |
| 14 | AI hội thoại | POST | `/api/sessions/{id}/messages` |
| 15 | AI hội thoại | GET | `/api/sessions/{id}/messages` |
| 16 | AI-04 | PUT | `/api/learner-level/me` (đổi trình độ, gửi lại tin nhắn để so sánh) |
| 17 | AI-05 | POST | `/api/ai/grammar-check` |
| 18 | AI-06 | POST | `/api/ai/suggest-expression` |
| 19 | AI-07 | POST | `/api/ai/analyze-answer` |
| 20 | Session | PUT | `/api/sessions/{id}/end` |
| 21 | AI-08 | POST | `/api/sessions/{id}/evaluate` |
| 22 | AI hội thoại | POST | `/api/sessions/{id}/messages` (session đã end → phải lỗi) |
| 23 | Learner Level | GET | `/api/learner-level/me` |
| 24 | Learner Level | PUT | `/api/learner-level/me` |
| 25 | Vocabulary | POST | `/api/vocabulary` |
| 26 | Vocabulary | GET | `/api/vocabulary` |
| 27 | Vocabulary | GET | `/api/vocabulary?topicId=&level=` |
| 28 | Vocabulary | PUT | `/api/vocabulary/{id}` |
| 29 | Vocabulary | DELETE | `/api/vocabulary/{id}` |
| 30 | AI hạ tầng | POST | `/api/admin/ai/test-chat` |
| 31 | STT | POST | `/api/speech/transcribe` |
| 32 | TTS | POST | `/api/speech/synthesize` |
| 33 | STT | POST | `/api/sessions/{id}/voice-messages` |
| 34 | TTS | GET | `/api/sessions/{id}/messages/{messageId}/audio` |
| 35 | SPEECH | POST | `/api/speech/assess-pronunciation` |

Chi tiết từng bước, kèm body mẫu và kỳ vọng, ở các mục bên dưới.

---

## 1. SYSTEM-02 — Topic

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 1.1 | POST | `/api/topics` | admin | `{"name":"Travel","description":"Talking about trips","level":"A2"}` | 201, trả về topic có `id`, `active: true` |
| 1.2 | POST | `/api/topics` | admin | body trùng `"name":"Travel"` | 409 `Topic name already exists` |
| 1.3 | POST | `/api/topics` | **user** | body hợp lệ bất kỳ | 403 (không phải ADMIN) |
| 1.4 | GET | `/api/topics` | user | — | 200, thấy topic vừa tạo (chỉ liệt kê `active=true`) |
| 1.5 | GET | `/api/topics/{id}` | user | — | 200, đúng dữ liệu |
| 1.6 | GET | `/api/topics/999999` | user | — | 404 `Topic not found` |
| 1.7 | PUT | `/api/topics/{id}` | admin | đổi `description` | 200, dữ liệu cập nhật đúng |
| 1.8 | GET | `/api/topics/admin/all` | admin | — | 200, thấy cả topic active lẫn inactive |
| 1.9 | GET | `/api/topics/admin/all` | **user** | — | 403 |
| 1.10 | DELETE | `/api/topics/{id}` | admin | — | 200, sau đó `GET /api/topics` **không** còn thấy topic này nhưng `GET /api/topics/admin/all` vẫn thấy (`active:false`) |

> Giữ lại ít nhất 1 topic **active** (tạo thêm topic khác nếu cần) để dùng cho bước 3 và 8.

---

## 2. SYSTEM-03 — AI Config

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 2.1 | POST | `/api/admin/ai-configs` | admin | `{"name":"default-conversation","provider":"ollama","model":"qwen3:4b","systemPrompt":"You are a friendly English tutor.","temperature":0.7,"maxTokens":1024}` | 201, `active: false` |
| 2.2 | GET | `/api/admin/ai-configs` | admin | — | 200, thấy config vừa tạo |
| 2.3 | GET | `/api/admin/ai-configs` | **user** | — | 403 (toàn bộ nhóm này nằm dưới `/api/admin/**`) |
| 2.4 | PUT | `/api/admin/ai-configs/{id}/activate` | admin | — | 200, `active: true` |
| 2.5 | POST | `/api/admin/ai-configs` | admin | tạo thêm 1 config thứ 2, rồi activate config thứ 2 | config đầu tiên tự động `active:false`, chỉ config thứ 2 `active:true` (kiểm tra lại bằng `GET /api/admin/ai-configs`) |
| 2.6 | DELETE | `/api/admin/ai-configs/{id}` | admin | xóa config **không** active | 200 |

> Sau bước này phải có **đúng 1** config đang `active:true` — cần cho bước 3 (session sẽ tự gắn config này, và `systemPrompt` của nó sẽ được dùng ở bước 4).

---

## 3. SYSTEM-01 — Conversation Session (tạo phiên)

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 3.1 | POST | `/api/sessions` | user | `{"topicId": <id topic active ở bước 1>}` | 201, `status:"ACTIVE"`, `aiConfigName` đúng config đang active |
| 3.2 | GET | `/api/sessions/me` | user | — | 200, thấy session vừa tạo |
| 3.3 | GET | `/api/sessions/{id}` | user | id session ở 3.1 | 200 |
| 3.4 | GET | `/api/sessions/{id}` | **admin** (token khác chủ session) | cùng id ở 3.3 | 404 `Session not found` (cố tình trả 404 thay vì 403 — xem README) |

> **Chưa** end session ở bước này — để dành phiên đang `ACTIVE` cho mục 4, 5 bên dưới.

---

## 4. AI-01 / AI-02 / AI-03 — Hội thoại với AI

Dùng session `ACTIVE` vừa tạo ở mục 3. Nếu chưa cài Ollama, đổi tạm `AI_PROVIDER=mock` trong `.env` để test luồng mà không cần chờ model — chỉ cần biết `reply` có trả về đúng cấu trúc, nội dung `[MOCK] Bạn vừa nói: "..."` không quan trọng bằng việc luồng chạy thông.

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 4.1 | POST | `/api/sessions/{id}/messages` | user | `{"content":"Hello! My name is Tam."}` | 201, trả về tin nhắn của **AI** (`role:"ASSISTANT"`), không phải tin nhắn vừa gửi |
| 4.2 | GET | `/api/sessions/{id}/messages` | user | — | 200, thấy đúng 2 tin nhắn theo thứ tự: `USER` ("Hello!...") rồi `ASSISTANT` (câu trả lời) |
| 4.3 | POST | `/api/sessions/{id}/messages` | user | `{"content":"What did I just tell you my name was?"}` | 201 — **AI-03**: nếu dùng Ollama/Gemini thật, câu trả lời phải nhắc đúng tên "Tam" đã gửi ở 4.1 (chứng minh AI thấy được lịch sử, không chỉ tin nhắn mới nhất) |
| 4.4 | GET | `/api/sessions/{id}/messages` | user | — | 200, giờ có 4 tin nhắn theo đúng thứ tự thời gian |
| 4.5 | POST | `/api/sessions/{id}/messages` | user | body rỗng `{}` | 400 `Content is required` |
| 4.6 | POST | `/api/sessions/{id}/messages` | **admin** (không phải chủ session) | body hợp lệ | 404 `Session not found` (giống 3.4 — không lộ session người khác tồn tại hay không) |

> Giữ session này ở trạng thái `ACTIVE`, có ít nhất vài tin nhắn — dùng tiếp cho mục 5, 6.

---

## 5. AI-04 — Điều chỉnh độ khó theo trình độ

AI-04 không có endpoint riêng — chỉ ảnh hưởng đến `systemPrompt` gửi cho AI trong `/api/sessions/{id}/messages`. Cách test: đặt trình độ, gửi tin nhắn, so sánh độ phức tạp câu trả lời.

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 5.1 | PUT | `/api/learner-level/me` | user | `{"level":"A1"}` | 200 |
| 5.2 | POST | `/api/sessions` | user | `{}` (session mới) | 201 |
| 5.3 | POST | `/api/sessions/{id}/messages` | user | `{"content":"Can you explain how photosynthesis works?"}` | 201 — nếu dùng Ollama/Gemini thật, câu trả lời nên dùng từ vựng/ngữ pháp đơn giản (A1) |
| 5.4 | PUT | `/api/learner-level/me` | user | `{"level":"C1"}` | 200 |
| 5.5 | POST | `/api/sessions/{id}/messages` | user | cùng câu hỏi như 5.3, session **mới** | 201 — câu trả lời nên tự nhiên/phức tạp hơn rõ rệt so với 5.3 |

> Với `AI_PROVIDER=mock` bước này không có ý nghĩa (mock luôn echo lại nguyên văn) — chỉ test được với Ollama/Gemini thật.

---

## 6. AI-05 / AI-06 / AI-07 / AI-08 — Sửa lỗi, gợi ý, phân tích, đánh giá

**AI-05, AI-06, AI-07** độc lập, không cần session. **AI-08** cần 1 session đã **ENDED** có tin nhắn — dùng session từ mục 4.

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 6.1 | POST | `/api/ai/grammar-check` | user | `{"text":"She don't like coffee."}` | 200, `hasErrors:true`, `corrected:"She doesn't like coffee."`, `explanation` bằng tiếng Việt |
| 6.2 | POST | `/api/ai/grammar-check` | user | `{"text":"She doesn't like coffee."}` (câu đã đúng) | 200, `hasErrors:false`, `corrected` = nguyên văn |
| 6.3 | POST | `/api/ai/grammar-check` | user | body rỗng `{}` | 400 `Text is required` |
| 6.4 | POST | `/api/ai/grammar-check` | user | text > 2000 ký tự | 400 `Text must be at most 2000 characters` |
| 6.5 | POST | `/api/ai/suggest-expression` | user | `{"text":"I want to go to the store."}` | 200, `suggestions` có 2-3 phần tử, mỗi phần tử có `text` khác câu gốc và `note` tiếng Việt |
| 6.6 | POST | `/api/ai/suggest-expression` | user | body rỗng `{}` | 400 `Text is required` |
| 6.7 | POST | `/api/ai/analyze-answer` | user | `{"question":"What did you do last weekend?","answer":"I go to the park and play football with friend."}` | 200, `onTopic:true`, `feedback` chỉ ra lỗi ngữ pháp ("go"→"went", thiếu mạo từ...), `suggestedImprovement` là câu đã sửa |
| 6.8 | POST | `/api/ai/analyze-answer` | user | `{"question":"What did you do last weekend?","answer":"Blue is my favorite color."}` | 200, `onTopic:false` — câu trả lời không liên quan đến câu hỏi |
| 6.9 | POST | `/api/ai/analyze-answer` | user | thiếu field `question` | 400 `Question is required` |
| 6.10 | POST | `/api/sessions/{id}/evaluate` | user | id session ở mục 4, vẫn đang **ACTIVE** (chưa end) | 400 `Only ended sessions can be evaluated - end the session first` |
| 6.11 | PUT | `/api/sessions/{id}/end` | user | id session ở mục 4 | 200 (kết thúc session để test bước tiếp theo) |
| 6.12 | POST | `/api/sessions/{id}/evaluate` | user | cùng id vừa end | 200, có `summary`, `strengths`, `areasToImprove`, `estimatedLevel` là 1 trong A1-C2 |
| 6.13 | POST | `/api/sessions/{id}/evaluate` | user | id 1 session **ENDED** khác, chưa từng gửi tin nhắn nào | 400 `Session has no messages to evaluate` |
| 6.14 | POST | `/api/sessions/{id}/evaluate` | **admin** (không phải chủ session) | id session của user khác | 404 `Session not found` |

> Nếu model trả về không đúng định dạng JSON yêu cầu (hay gặp với model nhỏ chạy local), các endpoint AI-05/06/07/08 vẫn trả **200** nhưng field chính (`explanation`/`note`/`feedback`/`summary`) sẽ là nguyên văn phản hồi thô của AI — không phải lỗi hệ thống. Xem log server (`AI response was not valid JSON...`) để xác nhận.

---

## 7. LEARN-01 — Learner Level

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 7.1 | GET | `/api/learner-level/me` | user (tài khoản mới, chưa từng set) | — | 200, `level: null` (không phải lỗi) |
| 7.2 | PUT | `/api/learner-level/me` | user | `{"level":"B1"}` | 200, `level:"B1"`, `source:"SELF_ASSESSED"` |
| 7.3 | GET | `/api/learner-level/me` | user | — | 200, thấy `B1` vừa set |
| 7.4 | PUT | `/api/learner-level/me` | user | `{"level":"Z9"}` | 400 `Level must be one of: A1, A2, B1, B2, C1, C2` |
| 7.5 | PUT | `/api/learner-level/me` | user | `{"level":"c1"}` (chữ thường) | 200 — parse không phân biệt hoa/thường |

---

## 8. LEARN-02 — Vocabulary

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 8.1 | POST | `/api/vocabulary` | admin | `{"word":"itinerary","meaning":"lịch trình","example":"Here is our travel itinerary.","pronunciation":"/aɪˈtɪnəreri/","level":"B1","topicId":<id topic active>}` | 201 |
| 8.2 | POST | `/api/vocabulary` | admin | `{"word":"passport","meaning":"hộ chiếu"}` (không `topicId`, không `level`) | 201 — cả 2 field optional |
| 8.3 | POST | `/api/vocabulary` | admin | `topicId: 999999` | 404 `Topic not found` |
| 8.4 | GET | `/api/vocabulary` | user | — | 200, thấy cả 2 từ vừa tạo |
| 8.5 | GET | `/api/vocabulary?topicId=<id>` | user | — | 200, chỉ thấy từ có gắn topic đó (`itinerary`, không có `passport`) |
| 8.6 | GET | `/api/vocabulary?level=B1` | user | — | 200, chỉ thấy từ level B1 |
| 8.7 | PUT | `/api/vocabulary/{id}` | admin | đổi `meaning` | 200, cập nhật đúng |
| 8.8 | DELETE | `/api/vocabulary/{id}` | admin | — | 200, từ biến mất khỏi `GET /api/vocabulary` nhưng còn trong `GET /api/vocabulary/admin/all` |

---

## 9. Hạ tầng AI — test provider trực tiếp (không qua session)

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 9.1 | POST | `/api/admin/ai/test-chat` | admin | `{"message":"Xin chào"}` | Nếu `AI_PROVIDER=ollama` và Ollama **chưa chạy**: 502 `AI service is currently unavailable...` |
| 9.2 | (Sau khi chạy `ollama serve` + `ollama pull qwen3:4b`) | cùng request 9.1 | admin | — | 200, `provider:"ollama"`, `reply` ngắn gọn, **không** còn phần `<think>` |
| 9.3 | POST | `/api/admin/ai/test-chat` | **user** | — | 403 |

> Muốn test nhanh không cần chờ Ollama: đổi tạm `AI_PROVIDER=mock` trong `.env`, restart app, gọi lại 9.1 → `reply` sẽ là `[MOCK] Bạn vừa nói: "..."`.

---

---

## 10. STT-01→04 / TTS-01→04 — Nhận diện & tổng hợp giọng nói

Mặc định `SPEECH_PROVIDER=mock` — các bước dưới vẫn chạy được nhưng `text`/audio trả về không phải kết quả thật. Muốn test thật, đổi `SPEECH_PROVIDER=azure` + điền `AZURE_SPEECH_KEY`, `AZURE_SPEECH_REGION` trong `.env` rồi restart app. Ghi âm sẵn 1 file `.wav` (PCM, 16kHz, mono) để test — hầu hết công cụ ghi âm online/Audacity export được định dạng này.

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 10.1 | POST | `/api/speech/transcribe` | user | multipart, field `audio` = file `.wav` | 200, `text` có nội dung (mock: `[MOCK] Transcribed N bytes of audio`) |
| 10.2 | POST | `/api/speech/transcribe` | user | không kèm file | 400 `Audio file is required` |
| 10.3 | POST | `/api/speech/synthesize` | user | `{"text":"Hello, how are you?"}` | 200, response là file audio (`Content-Type: audio/mpeg`), tải về mở nghe được (nếu dùng Azure thật) |
| 10.4 | POST | `/api/speech/synthesize` | user | `{"text":"Xin chào","languageCode":"vi-VN","voiceName":"vi-VN-HoaiMyNeural"}` | 200 — đổi giọng/ngôn ngữ qua request (TTS-02, TTS-03) |
| 10.5 | POST | `/api/speech/synthesize` | user | body rỗng `{}` | 400 `Text is required` |
| 10.6 | POST | `/api/sessions/{id}/voice-messages` | user | multipart, field `audio` = file `.wav`, session đang `ACTIVE` | 201, `transcript` có nội dung, `reply` là tin nhắn AI (giống `/messages` text) |
| 10.7 | GET | `/api/sessions/{id}/messages` | user | — | 200, tin nhắn vừa tạo ở 10.6 xuất hiện trong lịch sử như tin nhắn thường (STT-03: không có bảng riêng, dùng chung `conversation_messages`) |
| 10.8 | GET | `/api/sessions/{id}/messages/{messageId}/audio` | user | `messageId` là tin nhắn **ASSISTANT** vừa nhận ở 10.6 | 200, audio MP3 của câu trả lời AI |
| 10.9 | GET | `/api/sessions/{id}/messages/{messageId}/audio` | **admin** (không phải chủ session) | — | 404 `Message not found` |
| 10.10 | POST | `/api/sessions/{id}/voice-messages` | user | session đã `ENDED` | 400 `Session has already ended` (logic giống gửi tin nhắn text) |

> Nếu dùng Azure thật mà bị 502 `Speech service is currently unavailable`, kiểm tra: đúng region đã tạo resource Azure Speech chưa (`AZURE_SPEECH_REGION`), key còn hạn/đúng resource không, và file audio có đúng định dạng WAV/PCM/16kHz/mono không (Azure từ chối format khác mà không báo lỗi rõ ràng).

---

---

## 11. SPEECH-01→05 — Đánh giá phát âm

Cùng điều kiện provider như mục 10 (mặc định `mock` trả điểm cố định 90, muốn test thật cần `SPEECH_PROVIDER=azure` + key). Field tên là `referenceText` (không phải `text`).

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 11.1 | POST | `/api/speech/assess-pronunciation` | user | multipart: `audio`=file `.wav`, `referenceText`="This is a test sentence" | 200, có đủ `accuracyScore`, `fluencyScore`, `completenessScore`, `pronScore` (0-100), `words` là mảng theo từng từ trong `referenceText`, `feedback` bằng tiếng Việt |
| 11.2 | POST | `/api/speech/assess-pronunciation` | user | thiếu `referenceText` | 400 `Reference text is required` |
| 11.3 | POST | `/api/speech/assess-pronunciation` | user | thiếu file `audio` | 400 `Audio file is required` |
| 11.4 | (Chỉ test được với Azure thật) | đọc sai/thiếu vài từ so với `referenceText` khi ghi âm | user | — | `words` có ít nhất 1 phần tử với `errorType` khác `"None"` (`Mispronunciation`/`Omission`/`Insertion`), và `feedback` nhắc cụ thể từ bị lỗi |
| 11.5 | (Chỉ test được với Azure thật) | đọc đúng hoàn toàn, rõ ràng | user | — | các score đều cao (>80), `words` toàn `errorType:"None"`, `feedback` mang tính khích lệ + 1 mẹo nhỏ để tự nhiên hơn |

> Nếu dùng Azure thật mà bị 502, các nguyên nhân giống mục 10 (sai region, sai key, sai định dạng audio) — kiểm tra theo đúng thứ tự đó trước.

---

## Ghi chú khi test

- Tất cả response lỗi đều có dạng `{"success": false, "message": "...", "timestamp": "..."}` — kiểm tra đúng `message`, không chỉ đúng status code.
- Field gửi tin nhắn tên là **`content`**, không phải `message` — gửi sai tên field sẽ bị validate chặn (400 `Content is required`) thay vì lỗi rõ ràng hơn.
- Sau khi restart app (nhất là sau khi đổi `.env`), nhớ đăng nhập lại lấy token mới nếu bạn đổi `JWT_SECRET`.
- Nếu muốn test lại từ đầu sạch sẽ: xóa volume Docker (`docker compose down -v`) rồi `docker compose up -d` lại — Liquibase sẽ tạo lại toàn bộ schema, `AdminSeeder` tự tạo lại tài khoản admin.
