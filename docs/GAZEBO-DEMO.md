# Chạy demo luyện tiếng Anh

Trên Windows, mở Docker Desktop ở chế độ Linux containers. Mở PowerShell tại
thư mục dự án. Dùng JDK 21 và chạy:

```powershell
.\start-demo.ps1 -MockAi
```

Lần đầu nếu chưa có `.env`, script sẽ tạo từ `.env.example` rồi dừng để bạn
điền cấu hình. Nếu dữ liệu Docker trống, Compose tạo lại volume và tải lại model
Whisper, giọng Piper. Liquibase tạo schema khi Spring Boot khởi động.

Mở [trang demo](http://localhost:8080/demo/index.html), đăng nhập bằng tài khoản
đã tạo hoặc tài khoản demo được cấu hình trong `.env`. Cho phép microphone,
nói “My name is Linh. I like music”, rồi bấm **Dừng và gửi**. Kiểm tra bản chép
lời, sửa trong ô nhập nếu cần và bấm **Gửi**. Bấm **Nghe câu trả lời của robot**
để Piper đọc câu trả lời hiển thị trên màn hình. Chế độ `-MockAi` nhắc lại câu
vừa gửi với tiền tố `[MOCK]`. Whisper và Piper vẫn chạy thật.

Nếu trang báo lỗi HTTP 500, xem ngoại lệ tương ứng trong `backend-demo.log`
ở thư mục dự án hoặc cửa sổ PowerShell đang chạy backend.

Để dùng AI hội thoại thật với cấu hình Ollama mặc định, mở Ollama và chuẩn bị
model `qwen3:1.7b` bằng `ollama pull qwen3:1.7b`, sau đó chạy
`.\start-demo.ps1` không kèm `-MockAi`. Nếu đã chọn Gemini trong `.env`,
điền khóa API ở backend và dùng cùng lệnh khởi chạy.

## Robot mô phỏng

Dùng Ubuntu 24.04 với ROS 2 Jazzy, Gazebo Harmonic và gói `ros-jazzy-ros-gz`.
Mở ba terminal Ubuntu ở thư mục dự án. Trong mỗi terminal, nạp
`source /opt/ros/jazzy/setup.bash`. Sau đó chạy mỗi lệnh ở một terminal:

```bash
gz sim -r gazebo/language_robot.sdf
ros2 run ros_gz_bridge parameter_bridge '/language_robot/head@std_msgs/msg/Float64]gz.msgs.Double'
python3 gazebo/robot_bridge.py
```

Đăng nhập bridge bằng cùng tài khoản đã dùng trên web. Nếu Ubuntu chạy trên
máy khác hoặc WSL dùng mạng NAT, đặt `ROBOT_BACKEND_URL` thành địa chỉ backend
mà Ubuntu truy cập được trước khi chạy bridge. Ví dụ:

```bash
ROBOT_BACKEND_URL=http://192.168.1.10:8080 python3 gazebo/robot_bridge.py
```

Robot gật đầu trong lúc web phát âm thanh. Bạn có thể thử riêng khớp cổ với
Gazebo và `ros_gz_bridge` đang chạy, trước khi mở `robot_bridge.py`:

```bash
ros2 topic pub --once /language_robot/head std_msgs/msg/Float64 '{data: 0.33}'
```
