# AuctionSystem — Hệ thống đấu giá trực tuyến

## Checklist (những mục trong README này)
- [x] Mô tả dự án
- [x] Công nghệ & Yêu cầu (prerequisites)
- [x] Cấu trúc dự án (directory tree)
- [x] Hướng dẫn khởi chạy (Server → Client)
- [x] Câu lệnh chạy (Windows / macOS / Linux)
- [x] Lưu ý quan trọng (cross-platform)
- [x] Danh sách chức năng đã hoàn thành
- [x] Chú ý khi chạy test / debug

---

## Mô tả dự án
`AuctionSystem` là một ứng dụng đấu giá trực tuyến bao gồm:
- Server quản lý phiên đấu giá, lưu giữ dữ liệu người dùng, sản phẩm, phòng đấu giá, giao dịch và tự động hóa (auto-bidding).
- Client JavaFX cho phép người dùng (seller, bidder, admin) tương tác với hệ thống: đăng ký/đăng nhập, tham gia phòng đấu giá, đặt giá, quản lý ví, cấu hình auto-bid.

Phạm vi hệ thống:
- Hỗ trợ tạo sản phẩm, tạo phòng đấu giá, cập nhật trạng thái phòng (PENDING → ACTIVE → FINISHED), đặt giá thủ công, auto-bid, quản lý ví (nạp/rút tiền), lưu lịch sử giao dịch và lịch sử giá.

---

## Công nghệ & Yêu cầu
- Ngôn ngữ: Java
- Build: Apache Maven
- GUI Client: JavaFX
- Database: SQLite (file DB: `myDatabase.db` và `database_test.db` trong thư mục gốc)
- Logging: SLF4J + Logback
- Unit tests: JUnit 5

Yêu cầu phần mềm (khuyến nghị):
- JDK: đề nghị JDK >= 17 (kiểm tra `pom.xml`; project có thể sử dụng Java 17 hoặc cao hơn)
- Maven: 3.6+
- Hệ điều hành: Windows / macOS / Linux (bash/PowerShell/cmd)

Trước khi chạy:
- Cài đặt JDK và thiết lập `JAVA_HOME`.
- Cài Maven (`mvn` có trong PATH).
- Kiểm tra file DB `myDatabase.db` và `database_test.db`

---

## Cấu trúc dự án (tóm tắt)
Dưới đây là cây thư mục rút gọn (không liệt kê toàn bộ file):

```text
AuctionSystem/                        <-- root project
├─ myDatabase.db
├─ database_test.db
├─ pom.xml                             <-- top-level pom
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ client/
│  │  │  │  ├─ ClientApp.java          <-- JavaFX Application (GUI)
│  │  │  │  └─ Main.java               <-- wrapper/entry
│  │  │  ├─ server/
│  │  │  │  ├─ AppServer.java          <-- Server entry point
│  │  │  │  ├─ ClientHandler.java
│  │  │  │  └─ dao/
│  │  │  │      └─ ConnectDatabase.java
│  │  └─ resources/
│  │     └─ logback.xml
├─ src/test/
│  └─ java/
│     └─ server/dao/AuctionSystemDaoTest.java
└─ target/
```

Các lớp entry point:
- Server: `server.AppServer` (mặc định lắng nghe cổng 8080)
- Client: `client.ClientApp` (JavaFX), có thể dùng `client.Main` làm wrapper

Tập test quan trọng: `src/test/java/server/dao/AuctionSystemDaoTest.java`

---

## Hướng dẫn khởi chạy (thứ tự: Server → Client)

1. Build project (dùng Maven)
   - Từ thư mục gốc dự án (`AuctionSystem`):
   ```bash
   mvn clean package
   ```
   - Sau khi quá trình build `BUILD SUCCESS` sẽ sinh ra 2 file `.jar` quan trọng:
     - AuctionSystem-1.0-SNAPSHOT-server.jar (File chạy Server)
	 - AuctionSystem-1.0-SNAPSHOT-client.jar (File chạy Client)
   
3. Chạy Server (phải chạy trước Client)
     - Câu lệnh khởi động:
	 ```bash
	 java -jar AuctionSystem-1.0-SNAPSHOT-server.jar
	 ```
  
4. Chạy Client (sau khi Server đã khởi động)
   	 - Window:
	 ```bash
	 java -jar AuctionSystem-1.0-SNAPSHOT-client.jar
	 ```
   	 - Mac/Linux:
	 ```bash
	 java --enable-native-access=ALL-UNNAMED -jar AuctionSystem-1.0-SNAPSHOT-client.jar
	 ```
---

## Lưu ý quan trọng — Cross-platform và khác biệt lệnh
- Classpath separator:
  - Windows: `;`
  - macOS / Linux: `:`
- Trên PowerShell: cách đặt chuỗi có thể khác (dùng dấu nháy kép `"..."`), nếu gặp lỗi hãy chạy trong Git Bash hoặc cmd.
- Nếu dùng IDE (IntelliJ IDEA):
  - Import project bằng `pom.xml` (Maven).
  - Tạo Run Configuration cho `server.AppServer` (Application) và `client.ClientApp` (Application).
  - Đảm bảo `Project SDK` trùng với phiên bản JDK đã cài.

---

## Chạy unit tests
- Chạy tất cả tests:
```bash
mvn test
```

---

## Chức năng (đã hoàn thành) — tóm tắt
Dựa trên mã nguồn và test hiện có, hệ thống đã triển khai các chức năng chính sau:
- Quản lý người dùng: đăng ký, đăng nhập, lấy role, session client
- Sản phẩm & phòng đấu giá: thêm sản phẩm, tạo phòng, cập nhật trạng thái, lấy thông tin phòng
- Đấu giá: đặt giá, lấy giá hiện tại, lấy giao dịch giá mới nhất, lịch sử đặt giá, đếm số người tham gia
- Auto-bid: lưu/get/check/remove auto-bid settings
- Ví & giao dịch: cập nhật số dư, nạp/rút tiền, lịch sử giao dịch
- Kết nối DB: `ConnectDatabase` (SQLite JDBC singleton)

---

## Cấu hình & Tùy chỉnh nhanh
- Port server: chỉnh trong `src/main/java/server/AppServer.java` (mặc định 8080).
- Đường dẫn DB: chỉnh trong `src/main/java/server/dao/ConnectDatabase.java`.
- Logging: chỉnh `src/main/resources/logback.xml`.
- Java/JavaFX version: kiểm tra trong `pom.xml`. Nếu gặp lỗi phiên bản Java, hãy cài đúng JDK hoặc điều chỉnh `maven.compiler.release` trong `pom.xml`.

---

## Troubleshooting (Vấn đề thường gặp)
- Lỗi JavaFX ClassNotFound: đảm bảo JavaFX jars có trong `target/dependency` hoặc dùng `--module-path` + `--add-modules`.
- Lỗi SQLite locked: kiểm tra tiến trình khác đang dùng `myDatabase.db`; đóng process hoặc xóa file `.db-wal`/`.db-shm` nếu cần (chỉ khi an toàn).
- Port 8080 đang bận: đổi port trong `AppServer.java` hoặc đóng tiến trình chiếm port.

---

## Gợi ý phát triển / chạy nhanh trong IDE
- IntelliJ IDEA:
  - Open → chọn `pom.xml` để import Maven project.
  - Set Project SDK (JDK >= 17).
  - Tạo Run/Debug configuration cho:
	- Main class: `server.AppServer` (Server)
	- Main class: `client.ClientApp` (Client — JavaFX)
  - Run Server trước, sau đó run Client.

---

## Link báo cáo và video demo
[phttps://drive.google.com/drive/folders/1PMHOwlx5A5Alv1g_E6o-bJHmyYi1c-Ix?usp=sharing](https://drive.google.com/drive/folders/1PMHOwlx5A5Alv1g_E6o-bJHmyYi1c-Ix?usp=sharing)

