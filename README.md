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

## Chấm điểm và nghe kết quả

1. Chạy demo với Ollama, bỏ `-MockAi`, rồi gửi vài lượt hội thoại.
2. Bấm **Kết thúc và chấm điểm**. Trang giữ lại đoạn chat và hiển thị điểm ngữ pháp, từ vựng, hội thoại và điểm tổng. Điểm tổng là trung bình cộng của ba điểm thành phần, theo thang 100.
3. Đọc nhận xét tiếng Việt và mức CEFR tham khảo. Nếu nhận xét lỗi sau khi đã có điểm, bấm **Thử lấy lại nhận xét**. Nếu bước chấm điểm lỗi, bấm **Thử chấm lại**.
4. Bấm **Nghe kết quả bằng tiếng Anh**. Piper đọc các điểm, mức CEFR và lời khuyên ngắn nếu AI trả về. Khi bridge đang chạy, robot gật đầu lúc phát tiếng.

Điểm dựa trên văn bản đã gửi, kể cả bản chép lời đã sửa. Tính năng này chưa chấm phát âm, ngữ điệu hay tốc độ nói.

## Chạy robot trong Gazebo trên WSL 2

Cần Ubuntu 24.04 trên WSL 2, ROS 2 Jazzy, Gazebo Harmonic và gói `ros-jazzy-ros-gz`. Giữ Spring Boot chạy trên Windows. Mở ba terminal Ubuntu WSL. Trong mỗi terminal chạy:

```bash
source /opt/ros/jazzy/setup.bash
cd /mnt/d/Desktop/TTCD/ai-language-learning-publish
```

Đổi đường dẫn nếu repo nằm ở thư mục khác. Trong terminal 1, mở mô hình:

```bash
gz sim -r gazebo/language_robot.sdf
```

Trong terminal 2, truyền lệnh khớp cổ từ ROS 2 sang Gazebo:

```bash
ros2 run ros_gz_bridge parameter_bridge '/language_robot/head@std_msgs/msg/Float64]gz.msgs.Double'
```

Trong terminal 3, thử kết nối từ WSL tới Spring Boot:

```bash
export ROBOT_BACKEND_URL=http://localhost:8080
curl -fI --connect-timeout 5 "$ROBOT_BACKEND_URL/demo/index.html"
```

Nếu PowerShell trên Windows truy cập được trang nhưng WSL không kết nối được qua localhost, thử IP của Windows khi WSL chạy ở chế độ NAT:

```bash
WIN_HOST=$(ip route show default | awk 'NR==1 {print $3}')
export ROBOT_BACKEND_URL="http://$WIN_HOST:8080"
curl -fI --connect-timeout 5 "$ROBOT_BACKEND_URL/demo/index.html"
```

Khi curl trả HTTP 200, vẫn trong terminal 3, chạy:

```bash
python3 gazebo/robot_bridge.py
```

Đăng nhập bằng cùng tài khoản dùng trên web. Nếu bridge báo `WAITING`, đăng nhập trang demo để đăng ký robot. Gửi một câu và bấm **Nghe câu trả lời của robot**. Khi có tiếng, bridge báo `SPEAKING` và robot gật đầu. Khi phát xong, robot trở về tư thế nghỉ. Giữ cả ba terminal chạy trong lúc demo. Để xoay góc nhìn quanh robot, giữ nút giữa chuột rồi kéo trong cửa sổ 3D.

Xem [hướng dẫn kiểm thử MVP](TESTING-GUIDE.md) để nghiệm thu và xử lý lỗi theo từng đoạn kết nối.
