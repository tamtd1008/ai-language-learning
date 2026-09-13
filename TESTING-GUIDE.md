# Checklist kiểm thử API — SYSTEM, AI, LEARN

Test theo đúng thứ tự bên dưới vì các module phụ thuộc dữ liệu lẫn nhau (Vocabulary cần Topic tồn tại, Session cần AiConfig đã active, gửi tin nhắn cần Session đang active...). Dùng Insomnia/Postman, base URL `http://localhost:8080`.

**Chuẩn bị:** đăng nhập lấy JWT của cả 2 tài khoản, dùng suốt các bước dưới:
- `POST /api/auth/login` với `admin` / mật khẩu trong `.env` → lưu lại làm `{{admin_token}}`
- `POST /api/auth/login` với 1 user thường (đăng ký trước nếu chưa có qua `/api/auth/register`) → lưu làm `{{user_token}}`

M��i request bên dưới (trừ login) cần header `Authorization: Bearer <token>`.

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
| 16 | Session | PUT | `/api/sessions/{id}/end` |
| 17 | AI hội thoại | POST | `/api/sessions/{id}/messages` (session đã end) |
| 18 | Learner Level | GET | `/api/learner-level/me` |
| 19 | Learner Level | PUT | `/api/learner-level/me` |
| 20 | Vocabulary | POST | `/api/vocabulary` |
| 21 | Vocabulary | GET | `/api/vocabulary` |
| 22 | Vocabulary | GET | `/api/vocabulary?topicId=&level=` |
| 23 | Vocabulary | PUT | `/api/vocabulary/{id}` |
| 24 | Vocabulary | DELETE | `/api/vocabulary/{id}` |
| 25 | AI hạ tầng | POST | `/api/admin/ai/test-chat` |

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

> Giữ lại ít nhất 1 topic **active** (tạo thêm topic khác nếu cần) để dùng cho bước 3 và 5.

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

> **Chưa** end session ở bước này — để dành phiên đang `ACTIVE` cho mục 4 bên dưới.

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
| 4.7 | GET | `/api/sessions/{id}/messages` | user | — | So sánh nội dung `systemPrompt` thực tế (xem log server nếu cần) — nếu session có gắn topic, câu trả lời của AI nên xoay quanh chủ đề đó |
| 4.8 | PUT | `/api/sessions/{id}/end` | user | — | 200, `status:"ENDED"` |
| 4.9 | POST | `/api/sessions/{id}/messages` | user | `{"content":"..."}` (gửi sau khi đã end) | 400 `Session has already ended` |
| 4.10 | PUT | `/api/sessions/{id}/end` | user | gọi lại lần 2 | 400 `Session has already ended` |

---

## 5. LEARN-01 — Learner Level

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 5.1 | GET | `/api/learner-level/me` | user (tài khoản mới, chưa từng set) | — | 200, `level: null` (không phải lỗi) |
| 5.2 | PUT | `/api/learner-level/me` | user | `{"level":"B1"}` | 200, `level:"B1"`, `source:"SELF_ASSESSED"` |
| 5.3 | GET | `/api/learner-level/me` | user | — | 200, thấy `B1` vừa set |
| 5.4 | PUT | `/api/learner-level/me` | user | `{"level":"Z9"}` | 400 `Level must be one of: A1, A2, B1, B2, C1, C2` |
| 5.5 | PUT | `/api/learner-level/me` | user | `{"level":"c1"}` (chữ thường) | 200 — parse không phân biệt hoa/thường |

---

## 6. LEARN-02 — Vocabulary

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 6.1 | POST | `/api/vocabulary` | admin | `{"word":"itinerary","meaning":"lịch trình","example":"Here is our travel itinerary.","pronunciation":"/aɪˈtɪnəreri/","level":"B1","topicId":<id topic active>}` | 201 |
| 6.2 | POST | `/api/vocabulary` | admin | `{"word":"passport","meaning":"hộ chiếu"}` (không `topicId`, không `level`) | 201 — cả 2 field optional |
| 6.3 | POST | `/api/vocabulary` | admin | `topicId: 999999` | 404 `Topic not found` |
| 6.4 | GET | `/api/vocabulary` | user | — | 200, thấy cả 2 từ vừa tạo |
| 6.5 | GET | `/api/vocabulary?topicId=<id>` | user | — | 200, chỉ thấy từ có gắn topic đó (`itinerary`, không có `passport`) |
| 6.6 | GET | `/api/vocabulary?level=B1` | user | — | 200, chỉ thấy từ level B1 |
| 6.7 | PUT | `/api/vocabulary/{id}` | admin | đổi `meaning` | 200, cập nhật đúng |
| 6.8 | DELETE | `/api/vocabulary/{id}` | admin | — | 200, từ biến mất khỏi `GET /api/vocabulary` nhưng còn trong `GET /api/vocabulary/admin/all` |

---

## 7. Hạ tầng AI — test provider trực tiếp (không qua session)

| # | Method | Endpoint | Token | Body | Kỳ vọng |
|---|---|---|---|---|---|
| 7.1 | POST | `/api/admin/ai/test-chat` | admin | `{"message":"Xin chào"}` | Nếu `AI_PROVIDER=ollama` và Ollama **chưa chạy**: 502 `AI service is currently unavailable...` |
| 7.2 | (Sau khi chạy `ollama serve` + `ollama pull qwen3:4b`) | cùng request 7.1 | admin | — | 200, `provider:"ollama"`, `reply` ngắn gọn, **không** còn phần `<think>` |
| 7.3 | POST | `/api/admin/ai/test-chat` | **user** | — | 403 |

> Muốn test nhanh không cần chờ Ollama: đổi tạm `AI_PROVIDER=mock` trong `.env`, restart app, gọi lại 7.1 → `reply` sẽ là `[MOCK] Bạn vừa nói: "..."`.

---

## Ghi chú khi test

- Tất cả response lỗi đều có dạng `{"success": false, "message": "...", "timestamp": "..."}` — kiểm tra đúng `message`, không chỉ đúng status code.
- Field gửi tin nhắn tên là **`content`**, không phải `message` — gửi sai tên field sẽ bị validate chặn (400 `Content is required`) thay vì lỗi rõ ràng hơn.
- Sau khi restart app (nhất là sau khi đổi `.env`), nhớ đăng nhập lại lấy token mới nếu bạn đổi `JWT_SECRET`.
- Nếu muốn test lại từ đầu sạch sẽ: xóa volume Docker (`docker compose down -v`) rồi `docker compose up -d` lại — Liquibase sẽ tạo lại toàn bộ schema, `AdminSeeder` tự tạo lại tài khoản admin.
