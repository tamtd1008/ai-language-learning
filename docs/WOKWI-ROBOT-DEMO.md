# Demo robot hỗ trợ học ngoại ngữ trên Wokwi

## 1. Chức năng đầu tiên: đăng ký robot và báo trạng thái

Mục tiêu: ESP32 mô phỏng kết nối backend ai-language-learning thật. Nhấn nút để chuyển `IDLE → LISTENING → SPEAKING → IDLE`. Mỗi lần đổi trạng thái, backend lưu lại và LED trên mạch thay đổi.

Đây là demo quản lý thiết bị: LISTENING/SPEAKING chỉ biểu thị trạng thái giả lập, chưa thu âm, gọi AI hay phát giọng nói. Không cần Ollama hoặc dịch vụ speech cho demo này. Chưa dùng WebSocket nên khi tắt mô phỏng, backend không tự nhận biết OFFLINE.

Các API được sử dụng sẵn trong dự án:

| Thao tác | API |
|---|---|
| Đăng nhập lấy JWT | POST /api/auth/login |
| Đăng ký robot | POST /api/robots/register |
| Cập nhật trạng thái | PUT /api/robots/{id}/status |
| Kiểm tra kết quả | GET /api/robots |

## 2. Chuẩn bị backend

Chạy PostgreSQL và backend theo README của dự án. Backend phải truy cập được ở `http://localhost:8080`. Nạp cấu hình môi trường của dự án như bình thường. Dùng tài khoản đã đăng ký trên ứng dụng.

Mở PowerShell trên máy và chạy đoạn sau, thay tên tài khoản:

```powershell
$credential = Get-Credential -UserName 'tai-khoan-cua-ban' -Message 'Dang nhap AI Language Learning'
$loginBody = @{
  username = $credential.UserName
  password = $credential.GetNetworkCredential().Password
} | ConvertTo-Json
$login = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/auth/login' `
  -ContentType 'application/json' -Body $loginBody
$robotDemoToken = $login.data.token
$robotDemoToken | Set-Clipboard
```

JWT được chép vào clipboard để dán vào code ở bước 5. Dùng tài khoản demo; không chia sẻ hoặc lưu công khai dự án Wokwi còn chứa JWT. Khi JWT hết hạn, đăng nhập lại và thay token.

## 3. Cho Wokwi truy cập backend

Chọn một trong hai cách:

### Cách A: Private IoT Gateway

Wokwi web yêu cầu gói trả phí cho Private Gateway.

1. Tải và chạy Wokwi IoT Gateway theo tài liệu bên dưới.
2. Trong trình soạn thảo Wokwi, nhấn F1, chọn `Enable Private Wokwi IoT Gateway`.
3. Đặt `BASE_URL` trong code thành `http://host.wokwi.internal:8080`.

### Cách B: URL HTTPS trỏ đến backend

Nếu không có Private Gateway, dùng backend đã triển khai có URL HTTPS, hoặc tunnel HTTPS tới cổng 8080 của máy. Tunnel phải chuyển được các request `/api/...` và header Authorization. Đặt `BASE_URL` thành URL đó, không có dấu `/` cuối, ví dụ `https://dia-chi-backend-cua-ban`.

Code HTTPS bên dưới cần `ROOT_CA` là chứng chỉ CA gốc PEM phù hợp với URL bạn sử dụng. Lấy từ nhà cung cấp chứng chỉ/tunnel. Không dùng chứng chỉ ví dụ hoặc tắt xác minh TLS. Với Private Gateway dùng HTTP, không cần điền ROOT_CA.

Không đặt BASE_URL là localhost: localhost bên trong ESP32 giả lập không phải máy chạy backend. Public Gateway mặc định không truy cập được mạng nội bộ.

Tài liệu: https://docs.wokwi.com/guides/esp32-wifi

## 4. Tạo mạch Wokwi

1. Mở https://wokwi.com/projects/new/esp32.
2. Mở tab `diagram.json`, thay toàn bộ bằng nội dung sau.
3. Ba LED dùng điện trở 220 ohm; nút nối GPIO 18 xuống GND, dùng INPUT_PULLUP.

```json
{
  "version": 1,
  "author": "AI Language Learning",
  "editor": "wokwi",
  "parts": [
    { "type": "wokwi-esp32-devkit-v1", "id": "esp", "top": 0, "left": 0, "attrs": {} },
    { "type": "wokwi-pushbutton", "id": "btn", "top": 20, "left": 220, "attrs": { "color": "blue" } },
    { "type": "wokwi-led", "id": "idle", "top": 110, "left": 220, "attrs": { "color": "green" } },
    { "type": "wokwi-led", "id": "listen", "top": 170, "left": 220, "attrs": { "color": "blue" } },
    { "type": "wokwi-led", "id": "speak", "top": 230, "left": 220, "attrs": { "color": "yellow" } },
    { "type": "wokwi-resistor", "id": "r1", "top": 110, "left": 140, "attrs": { "value": "220" } },
    { "type": "wokwi-resistor", "id": "r2", "top": 170, "left": 140, "attrs": { "value": "220" } },
    { "type": "wokwi-resistor", "id": "r3", "top": 230, "left": 140, "attrs": { "value": "220" } }
  ],
  "connections": [
    [ "esp:TX0", "$serialMonitor:RX", "", [] ],
    [ "esp:RX0", "$serialMonitor:TX", "", [] ],
    [ "esp:18", "btn:1.l", "blue", [] ],
    [ "btn:2.l", "esp:GND.1", "black", [] ],
    [ "esp:25", "r1:1", "green", [] ],
    [ "r1:2", "idle:A", "green", [] ],
    [ "idle:C", "esp:GND.1", "black", [] ],
    [ "esp:26", "r2:1", "blue", [] ],
    [ "r2:2", "listen:A", "blue", [] ],
    [ "listen:C", "esp:GND.1", "black", [] ],
    [ "esp:27", "r3:1", "orange", [] ],
    [ "r3:2", "speak:A", "orange", [] ],
    [ "speak:C", "esp:GND.1", "black", [] ]
  ],
  "dependencies": {}
}
```

## 5. Thêm thư viện và code

Trong Library Manager, thêm `ArduinoJson`. Hoặc tạo file `libraries.txt` chứa:

```text
ArduinoJson
```

Thay nội dung `sketch.ino` bằng code sau. Sửa BASE_URL và TOKEN trước khi chạy. TOKEN chỉ là JWT, không bao gồm chữ Bearer.

```cpp
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <ArduinoJson.h>
#include <time.h>

const char* BASE_URL = "http://host.wokwi.internal:8080";
const char* TOKEN = "DAN_JWT_VAO_DAY";
const char* DEVICE_ID = "language-robot-wokwi-01";
const char* ROOT_CA = R"PEM(
)PEM";

const int BUTTON = 18;
const int LED_IDLE = 25;
const int LED_LISTEN = 26;
const int LED_SPEAK = 27;
const char* STATES[] = {"IDLE", "LISTENING", "SPEAKING"};
long robotId = 0;
int stateIndex = 0;
bool ready = false;
bool lastReading = HIGH;
bool stableButton = HIGH;
unsigned long changedAt = 0;

void showState(int index) {
  digitalWrite(LED_IDLE, index == 0);
  digitalWrite(LED_LISTEN, index == 1);
  digitalWrite(LED_SPEAK, index == 2);
}

bool connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;
  WiFi.begin("Wokwi-GUEST", "", 6);
  unsigned long started = millis();
  Serial.println("Dang ket noi Wi-Fi...");
  while (WiFi.status() != WL_CONNECTED && millis() - started < 15000) {
    delay(100);
  }
  return WiFi.status() == WL_CONNECTED;
}

bool requestApi(const char* method, String path, String body,
                JsonDocument& result) {
  if (!connectWiFi()) {
    Serial.println("Loi Wi-Fi. Thu lai bang nut nhan.");
    return false;
  }
  WiFiClient plainClient;
  WiFiClientSecure secureClient;
  HTTPClient http;
  String url = String(BASE_URL) + path;
  bool opened;
  if (url.startsWith("https://")) {
    if (strlen(ROOT_CA) < 50) {
      Serial.println("Hay dien ROOT_CA cho URL HTTPS.");
      return false;
    }
    // TLS can thoi gian dung de kiem tra han chung chi.
    configTime(0, 0, "pool.ntp.org", "time.google.com");
    struct tm timeInfo;
    if (!getLocalTime(&timeInfo, 10000)) {
      Serial.println("Chua dong bo duoc thoi gian TLS.");
      return false;
    }
    secureClient.setCACert(ROOT_CA);
    opened = http.begin(secureClient, url);
  } else {
    opened = http.begin(plainClient, url);
  }
  if (!opened) return false;
  http.setTimeout(10000);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("Authorization", String("Bearer ") + TOKEN);
  int code = http.sendRequest(method, body);
  String response = http.getString();
  http.end();
  Serial.printf("%s %s -> HTTP %d\n", method, path.c_str(), code);
  if (code < 200 || code >= 300) {
    Serial.println("Request that bai. Kiem tra URL, JWT va backend.");
    return false;
  }
  DeserializationError error = deserializeJson(result, response);
  if (error || !result["success"].as<bool>()) {
    Serial.println("Phan hoi API khong hop le.");
    return false;
  }
  return true;
}

bool registerRobot() {
  JsonDocument body;
  body["deviceId"] = DEVICE_ID;
  String payload;
  serializeJson(body, payload);
  JsonDocument response;
  if (!requestApi("POST", "/api/robots/register", payload, response))
    return false;
  robotId = response["data"]["id"].as<long>();
  if (robotId <= 0) return false;
  stateIndex = 0;
  showState(stateIndex);
  Serial.printf("Dang ky thanh cong. Robot ID: %ld | IDLE\n", robotId);
  return true;
}

bool setRobotState(int index) {
  JsonDocument body;
  body["status"] = STATES[index];
  body["sessionId"] = nullptr;
  String payload;
  serializeJson(body, payload);
  JsonDocument response;
  String path = "/api/robots/" + String(robotId) + "/status";
  if (!requestApi("PUT", path, payload, response)) return false;
  if (response["data"]["status"].as<String>() != STATES[index])
    return false;
  stateIndex = index;
  showState(index); // Chi doi LED sau khi backend xac nhan.
  Serial.printf("Backend da luu: %s\n", STATES[index]);
  return true;
}

void setup() {
  Serial.begin(115200);
  pinMode(BUTTON, INPUT_PULLUP);
  pinMode(LED_IDLE, OUTPUT);
  pinMode(LED_LISTEN, OUTPUT);
  pinMode(LED_SPEAK, OUTPUT);
  showState(-1);
  ready = registerRobot();
  Serial.println("Nhan nut de doi trang thai, hoac thu lai neu dang ky loi.");
}

void loop() {
  bool reading = digitalRead(BUTTON);
  if (reading != lastReading) {
    changedAt = millis();
    lastReading = reading;
  }
  if (millis() - changedAt >= 50 && reading != stableButton) {
    stableButton = reading;
    if (stableButton == LOW) {
      if (!ready) ready = registerRobot();
      else if (!setRobotState((stateIndex + 1) % 3))
        Serial.println("Chua doi trang thai. Nhan nut de thu lai.");
    }
  }
  delay(5);
}
```

## 6. Chạy và chứng minh demo thành công

1. Bật backend và cấu hình kết nối ở bước 3.
2. Nhấn Run trên Wokwi, mở Serial Monitor ở 115200 baud.
3. Chờ thông báo đăng ký thành công và đèn xanh lá sáng.
4. Nhấn rồi thả nút: đèn xanh dương sáng, Serial báo `Backend da luu: LISTENING`.
5. Chạy PowerShell dưới đây để xác nhận backend thực sự đã nhận trạng thái:

```powershell
$robotHeaders = @{ Authorization = "Bearer $robotDemoToken" }
$robots = Invoke-RestMethod -Method Get `
  -Uri 'http://localhost:8080/api/robots' -Headers $robotHeaders
$robots.data | Where-Object deviceId -eq 'language-robot-wokwi-01' |
  Format-List id, deviceId, status, lastSeenAt
```

6. Nhấn nút lần hai: đèn vàng sáng và status thành SPEAKING. Chạy lại đoạn kiểm tra.
7. Nhấn lần ba: trở về IDLE và đèn xanh lá sáng.

| Tình huống | Kết quả cần có |
|---|---|
| Khởi động | Đăng ký thành công, IDLE |
| Nhấn/thả một lần | Chỉ chuyển một trạng thái |
| Backend không hoạt động | Báo lỗi, không đổi LED sang trạng thái chưa lưu |
| Backend hoạt động trở lại | Nhấn nút thử lại được |
| Chạy lại cùng deviceId và tài khoản | Dùng lại robot, không tạo bản ghi trùng |

## 7. Lỗi thường gặp

- HTTP 401/403: JWT hết hạn hoặc sai; lấy lại token và chạy lại mô phỏng. Kiểm tra quyền sở hữu robot.
- HTTP 409: deviceId đã thuộc tài khoản khác; dùng tài khoản đúng hoặc đổi DEVICE_ID.
- HTTP 404: kiểm tra BASE_URL và đường dẫn API; nếu robot bị xóa, khởi động lại để đăng ký.
- HTTP âm: chưa truy cập được backend; kiểm tra Gateway, tunnel, chứng chỉ và thời gian TLS.
- Không có phản ứng khi nhấn: kiểm tra dây GPIO 18 và nút 1.l/2.l; nhấn rồi thả sau khi request trước kết thúc.
- Tắt Wokwi nhưng backend vẫn báo IDLE/SPEAKING: giới hạn của demo REST. Bổ sung WebSocket hoặc cơ chế timeout ở giai đoạn sau.

## 8. Lộ trình tiếp theo

1. **Demo hiện tại:** đăng ký thiết bị, cập nhật trạng thái, kiểm tra dữ liệu trên backend.
2. **Hội thoại văn bản thật:** tạo session bằng POST /api/sessions; nhập câu từ Serial; POST /api/sessions/{id}/messages; hiển thị câu AI trả lời. AI chạy ở backend.
3. **Màn hình và biểu cảm:** thêm OLED và servo, biểu diễn chờ/nghe/trả lời/lỗi.
4. **WebSocket:** kết nối /ws/robots/{deviceId}, gửi JWT trong header Authorization lúc handshake. Trạng thái gửi dưới dạng text thuần, không phải JSON/STOMP. Backend hiện có sendCommand nhưng cần kiểm tra/bổ sung nơi phát lệnh để demo điều khiển từ web.
5. **Robot thật có giọng nói:** microphone thu WAV/PCM 16 kHz mono → POST /api/sessions/{id}/voice-messages → lấy audio phản hồi qua GET /api/sessions/{id}/messages/{messageId}/audio → giải mã và phát ra loa. Không coi buzzer là loa TTS.

## 9. Phạm vi kiểm chứng của tài liệu

Các đường dẫn API, trường deviceId/status/sessionId, data.id và data.token đã được đối chiếu với mã nguồn backend. Bộ code và sơ đồ được cung cấp để chạy theo hướng dẫn; chưa được biên dịch hoặc chạy trực tiếp trên Wokwi trong lần soạn tài liệu này. Địa chỉ backend và JWT thật cần được điền tại máy của bạn.
