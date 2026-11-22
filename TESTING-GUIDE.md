# Kiểm thử Robot MVP

Khởi động và đăng nhập theo [README](README.md). Dùng `-MockAi` để kiểm tra thu âm, phát tiếng và Gazebo. Chuyển sang Ollama khi kiểm tra chất lượng câu trả lời và chủ đề.

Trong PowerShell, kiểm tra ba dịch vụ trước khi thử:

```powershell
docker compose --profile local-speech ps -a
```

PostgreSQL, Whisper và Piper cần ở trạng thái `healthy`. Mở [trang demo](http://localhost:8080/demo/index.html).

| Hạng mục | Thao tác | Kết quả mong đợi |
|---|---|---|
| Đăng nhập và gửi văn bản | Đăng nhập, nhập `Hello, my name is Tam.` rồi bấm **Gửi**. | Trang hiện câu đã gửi và câu đáp. Với mock, câu đáp có nhãn `[MOCK]`. |
| Chép lời | Bấm **Thu âm**, nói *I usually play football on Sundays*, rồi bấm **Dừng và gửi**. | Trang hiện **Whisper nghe được…** và điền văn bản vào ô nhập. Có thể sửa trước khi bấm **Gửi**. |
| Phát tiếng | Có câu đáp rồi bấm **Nghe câu trả lời của robot**. | Piper đọc nội dung hiển thị ở dòng **Piper đọc câu này**. Nếu trình duyệt chặn tự phát, dùng nút phát trên thanh âm thanh. |
| Phiên mới | Sau vài lượt, bấm **Bắt đầu hội thoại mới**. | Vẫn đăng nhập, khung hội thoại trống, bản chép lời và âm thanh cũ được dọn khỏi trang. |
| Chủ đề | Chọn **Ăn uống**, bấm **Bắt đầu hội thoại mới** và gửi *I’d like a cup of coffee, please*. Sau đó đổi lựa chọn sang **Đời sống hằng ngày** trước khi mở phiên mới. | Tiêu đề và câu gợi ý thay đổi khi mở phiên mới. Với Ollama, câu đáp nên bám tình huống gọi món. Việc đổi lựa chọn không đổi chủ đề của phiên đang mở. |
| Ngữ cảnh với Ollama | Trong cùng phiên, nói tên và môn thể thao bạn thích. Hỏi *Do you remember my name and what sport I like?* | Robot dùng thông tin trong phiên để trả lời. Nếu Whisper chép sai tên, sửa bản chép lời trước khi gửi để kiểm tra Qwen. |
| Gazebo và ROS 2 | Chạy các tiến trình trong [hướng dẫn Gazebo](README.md#chạy-robot-trong-gazebo-trên-wsl-2), rồi thu âm và phát một câu đáp trên web. | Bridge báo `LISTENING` khi thu âm, `SPEAKING` khi phát. Robot gật đầu lúc phát và trở về tư thế nghỉ khi trạng thái chuyển về `IDLE`. |

## Nghiệm thu Gazebo

1. Xác nhận mô hình xuất hiện trong Gazebo và mô phỏng đang chạy. Đặt cửa sổ web và Gazebo cạnh nhau để quan sát đồng thời.
2. Kiểm tra HTTP 200 từ WSL theo README. Chạy cả hai bridge. Sau khi đăng nhập web và bridge Python bằng cùng tài khoản, kiểm tra terminal hiện `Robot status: IDLE`.
3. Chọn **Ăn uống**, mở phiên mới và thu câu *I’d like a cup of coffee, please*. Kiểm tra bản chép lời, sửa nếu cần rồi bấm **Gửi**.
4. Bấm **Nghe câu trả lời của robot**. Kiểm tra đồng thời ba dấu hiệu: tiếng Piper phát, terminal báo `SPEAKING` và đầu robot gật. Khi phát xong, terminal trở về `IDLE` và đầu robot ngừng gật.
5. Mở phiên **Đời sống hằng ngày**. Kiểm tra tiêu đề đổi, hội thoại trống và robot vẫn ở tư thế nghỉ. Có thể ghi lại video từ lúc thu âm đến khi robot ngừng gật để minh họa kết quả.

## Nếu có lỗi

- **Compose báo lỗi hoặc dịch vụ chưa healthy.** Chạy `docker compose --profile local-speech logs --tail 60 postgres whisper piper` để xác định dịch vụ lỗi. Lần đầu có thể cần chờ tải model.
- **Trang báo lỗi HTTP 500.** Xem `Get-Content .\backend-demo.log -Tail 100` ở thư mục dự án và tìm lỗi trùng thời điểm thao tác.
- **Không mở được microphone.** Kiểm tra quyền microphone cho trang localhost và thử gửi văn bản để kiểm tra riêng phần hội thoại.
- **Gazebo không gật đầu.** Kiểm tra bridge đăng nhập đúng tài khoản, nhận được `SPEAKING` và dùng đúng địa chỉ backend.

- **Windows trả HTTP 200 nhưng WSL không kết nối được.** Thử IP Windows theo README và xác nhận HTTP 200 trước khi chạy bridge Python.
- **Bridge báo `SPEAKING` nhưng robot đứng yên.** Kiểm tra Gazebo đã chạy mô phỏng và terminal `ros_gz_bridge` còn hoạt động. Trong Ubuntu đã nạp ROS 2, chạy `ros2 topic echo /language_robot/head` khi đang phát câu đáp. Nếu góc thay đổi mà robot đứng yên, kiểm tra bridge ROS 2 → Gazebo và mô hình đang mở.
