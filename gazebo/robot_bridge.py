"""Read the demo robot state from Spring Boot and animate its Gazebo neck."""

import getpass
import json
import os
import urllib.request

import rclpy
from rclpy.node import Node
from std_msgs.msg import Float64


BASE_URL = os.environ.get("ROBOT_BACKEND_URL", "http://localhost:8080").rstrip("/")
DEVICE_ID = "gazebo-language-robot"


def request(path, *, token=None, payload=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    data = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode()
    call = urllib.request.Request(BASE_URL + path, data=data, headers=headers)
    with urllib.request.urlopen(call, timeout=2) as response:
        result = json.load(response)
    if not result["success"]:
        raise RuntimeError(result.get("message", "Backend request failed"))
    return result["data"]


class RobotBridge(Node):
    def __init__(self, token):
        super().__init__("language_robot_bridge")
        self.token = token
        self.publisher = self.create_publisher(Float64, "/language_robot/head", 10)
        self.last_status = None
        self.angle = 0.0
        self.create_timer(0.4, self.update)

    def update(self):
        try:
            robots = request("/api/robots", token=self.token)
            status = next(
                (robot["status"] for robot in robots if robot["deviceId"] == DEVICE_ID),
                "WAITING",
            )
        except (OSError, ValueError, RuntimeError) as error:
            status = "DISCONNECTED"
            if status != self.last_status:
                self.get_logger().warning(str(error))

        if status != self.last_status:
            self.get_logger().info(f"Robot status: {status}")
            self.last_status = status
        self.angle = (0.33 if self.angle <= 0 else -0.1) if status == "SPEAKING" else 0.0
        self.publisher.publish(Float64(data=self.angle))


def main():
    username = input("Tài khoản demo: ")
    password = getpass.getpass("Mật khẩu: ")
    try:
        token = request("/api/auth/login", payload={"username": username, "password": password})["token"]
    except (OSError, ValueError, RuntimeError) as error:
        raise SystemExit(f"Không đăng nhập được backend tại {BASE_URL}: {error}") from error
    rclpy.init()
    node = RobotBridge(token)
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.try_shutdown()


if __name__ == "__main__":
    main()
