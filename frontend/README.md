# Lingua frontend

Giao diện web React + TypeScript cho hệ thống học ngoại ngữ. Frontend hiện kết nối trực tiếp tới REST API backend và WebSocket qua proxy Vite.

## Yêu cầu

- Node.js 20.19+ hoặc 22.12+
- Backend đang chạy tại `http://localhost:8080`

## Chạy ở môi trường phát triển

```bash
pnpm install
pnpm dev
```

Mở URL được in ra bởi Vite (thường là `http://localhost:5173`). Vite chuyển tiếp `/api` và `/ws` tới backend. Mặc định backend ở `http://localhost:8080`; nếu backend chạy trên máy khác, tạo `frontend/.env.local` theo `.env.example` và đặt `VITE_BACKEND_URL` thành địa chỉ máy đó, ví dụ `http://192.168.1.20:8080`, rồi khởi động lại Vite. Hai máy cần ở cùng mạng và backend phải cho phép kết nối từ mạng LAN.

## Các luồng hiện có

- Đăng ký, đăng nhập, đăng xuất và phân biệt khu vực quản trị theo role trả về từ backend.
- Tổng quan phiên học, số tin nhắn, trình độ và tiến độ từ `/api/progress/summary`.
- Tạo session theo chủ đề, nhắn tin với AI, tải lịch sử và phát audio câu trả lời.
- Ghi âm trong trình duyệt, chuyển sang WAV PCM mono 16 kHz rồi gửi qua voice-messages.
- Kết thúc phiên, gọi đánh giá và chấm điểm phiên.
- Sinh/chấm bài tập ngữ pháp và nghe hiểu; tạo đề nói và câu luyện phát âm.
- Xem từ đang theo dõi, cập nhật trạng thái LEARNING/LEARNED.
- Trang quản trị để tạo/ẩn chủ đề, tạo/kích hoạt cấu hình AI và gửi tin nhắn thử tới provider.

## Ghi chú

- Backend phải có một AI config đang kích hoạt để tạo session.
- Ollama/Gemini cần thiết cho hội thoại và các bài tập AI. STT/TTS có thể chạy Mock để kiểm tra luồng hoặc cấu hình Azure để xử lý âm thanh thật.
- Trình duyệt chỉ cho phép micro trên HTTPS hoặc localhost. Người dùng cần cấp quyền microphone.
- JWT được giữ trong `sessionStorage` và tự mất khi đóng tab. Đây là lựa chọn phù hợp cho bản phát triển; khi triển khai Internet công khai nên đánh giá mô hình cookie `HttpOnly` và các biện pháp bảo vệ tương ứng.
