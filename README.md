# AI Language Learning · Robot MVP

Trang web luyện giao tiếp tiếng Anh với robot mô phỏng. Nhập câu hoặc thu âm để Whisper chép lời, gửi câu cho Qwen qua Ollama, rồi nghe Piper đọc câu trả lời. Bạn có thể chọn chủ đề khi mở phiên mới. Gazebo mô phỏng robot gật đầu trong lúc phát tiếng.

## Chạy trên Windows

Cần Git, JDK 21, Docker Desktop ở chế độ Linux containers và trình duyệt có quyền dùng microphone. Maven Wrapper có sẵn trong repo. Cần Internet khi tải image, model và dependency lần đầu.

1. Checkout nhánh `demo/robot-mvp`. Trong PowerShell tại thư mục dự án, tạo `.env` từ `.env.example` nếu chưa có:

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   ```

   Đặt `JWT_SECRET` thành một chuỗi ngẫu nhiên dài và cấu hình `ADMIN_USERNAME`, `ADMIN_PASSWORD` cho tài khoản demo. Spring Boot tạo tài khoản khi khởi động nếu tài khoản đó chưa tồn tại.

2. Mở Docker Desktop rồi chạy:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\start-demo.ps1 -MockAi
   ```

   Script khởi động PostgreSQL, Whisper và Piper rồi chạy Spring Boot. Lần đầu, Docker tải model Whisper và giọng Piper. `-MockAi` nhắc lại câu đã gửi với nhãn `[MOCK]`. Whisper và Piper vẫn hoạt động trong chế độ này.

3. Mở [trang demo](http://localhost:8080/demo/index.html) và đăng nhập. Để luyện theo chủ đề, chọn **Đời sống hằng ngày** hoặc **Ăn uống**, rồi bấm **Bắt đầu hội thoại mới**. Nhập câu hoặc bấm **Thu âm**, nói một câu tiếng Anh, rồi bấm **Dừng và gửi**. Kiểm tra bản chép lời, sửa nếu cần và bấm **Gửi**. Bấm **Nghe câu trả lời của robot** để phát tiếng.

## Dùng Qwen cho hội thoại

Mở Ollama trên máy, để `AI_PROVIDER=ollama` trong `.env` và tải model hội thoại nếu chưa có:

```powershell
ollama pull qwen3:1.7b
```

Dừng Spring Boot bằng `Ctrl+C`, rồi chạy lại lệnh khởi động bỏ `-MockAi`. Lần gửi đầu có thể mất thêm thời gian để Ollama nạp model.

## Nối robot Gazebo

Trên Ubuntu 24.04 có ROS 2 Jazzy, Gazebo Harmonic và `ros-jazzy-ros-gz`, chạy mô phỏng, ROS bridge và script Python theo [hướng dẫn Gazebo](docs/GAZEBO-DEMO.md#robot-mô-phỏng). Đăng nhập bridge bằng cùng tài khoản web và đặt `ROBOT_BACKEND_URL` nếu backend chạy trên máy hoặc môi trường mạng khác. Khi web phát tiếng, robot gật đầu. Khi phát xong, robot về tư thế nghỉ.

Xem [hướng dẫn kiểm thử MVP](TESTING-GUIDE.md) để chạy thử từng bước.
