# QuanLyQuanCafe – Hệ thống quản lý quán cà phê

## 1. Tổng quan dự án

QuanLyQuanCafe là một ứng dụng **Spring Boot** mô phỏng hệ thống quản lý quán cà phê hiện đại, tập trung vào trải nghiệm quản trị trên web với giao diện dashboard đẹp, trực quan.

Các nghiệp vụ chính:
- Quản lý menu đồ uống / món ăn
- Quản lý đơn hàng tại quán (tại bàn / mang đi)
- Quản lý kho nguyên liệu (tồn kho, nhập hàng)
- Quản lý nhân viên, chấm công và tính lương cơ bản
- Quản lý đặt bàn (booking) cho khách
- Theo dõi doanh thu, báo cáo, xuất CSV
- Hệ thống khách hàng và đánh giá (customer & review)
- Tích hợp **trợ lý AI dùng Gemini** để tư vấn dựa trên dữ liệu thực trong hệ thống

---

## 2. Công nghệ & thư viện sử dụng

### Backend
- **Ngôn ngữ:** Java 21 (`<java.version>21</java.version>`)
- **Framework:** Spring Boot **3.5.11** (`spring-boot-starter-parent`)
- **Spring Boot Starters:**
  - `spring-boot-starter-web` – xây dựng REST API và MVC
  - `spring-boot-starter-thymeleaf` – render giao diện server-side bằng Thymeleaf
  - `spring-boot-starter-data-jpa` – thao tác database với JPA/Hibernate
  - `spring-boot-starter-security` – xác thực, phân quyền (ADMIN, nhân viên: CASHIER / SERVER / BARISTA / SECURITY, khách đăng ký: CUSTOMER)
  - `spring-boot-starter-oauth2-client` – đã khai báo, hiện cấu hình OAuth2 bị disable trong `SecurityConfig`
  - `spring-boot-starter-validation` – validate dữ liệu request
- **Bảo mật:**
  - Spring Security, cấu hình trong `config/SecurityConfig.java`
  - Đăng nhập bằng form login tùy biến (`/login`), đăng ký tài khoản `/register`
  - Mã hóa mật khẩu bằng `BCryptPasswordEncoder`
  - Phân quyền theo role: `ROLE_ADMIN`, `ROLE_CASHIER`, `ROLE_SERVER`, `ROLE_BARISTA`, `ROLE_SECURITY`, `ROLE_CUSTOMER` (khách có tài khoản app; khác với khách vãng lai / walk-in trên đơn hàng)
- **Truy cập dữ liệu:**
  - Spring Data JPA repositories dưới `repository/`
  - Entity cho các bảng: menu, đơn hàng, khách hàng, nhân viên, chấm công, doanh thu, kho, cài đặt, đặt bàn, v.v… trong `model/`
  - Tự động cập nhật schema: `spring.jpa.hibernate.ddl-auto=update`
- **Cơ sở dữ liệu:**
  - **MySQL** (`com.mysql:mysql-connector-j`)
  - Kết nối mặc định: `jdbc:mysql://localhost:3306/quanlyquancafe` với user `root` (cấu hình trong `application.properties`)
- **Công cụ hỗ trợ:**
  - `spring-boot-devtools` – reload nhanh khi phát triển
  - `lombok` – giảm boilerplate cho entity/service (được cấu hình làm annotation processor)

### AI / Tích hợp Gemini
- Sử dụng **Google Gemini API** thông qua REST HTTP client:
  - Endpoint cấu hình trong `ChatController` (`/api/chat/ai`)
  - Dùng `java.net.http.HttpClient` + `ObjectMapper` để gọi API `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- Cấu hình trong `application.properties`:
  - `gemini.api.key` – API key (nên chuyển sang biến môi trường khi triển khai thực tế)
  - `gemini.model` – model mặc định `gemini-2.5-flash`
- Chat AI sử dụng **dữ liệu thật từ DB** làm context:
  - Menu (`MenuItem`) nếu người dùng hỏi về món / đồ uống
  - Danh sách nhân viên (`Staff`) nếu người dùng hỏi về nhân sự

### Frontend (trong Spring MVC)
- **Template engine:** Thymeleaf (có `springsecurity6` extras)
- **UI / CSS / JS:**
  - **Bootstrap 5** – layout, responsive UI
  - **Bootstrap Icons** – icon cho dashboard
  - **Google Fonts (Inter)** – font chữ
  - CSS tùy biến: `src/main/resources/static/css/doanhthu.css`, `style.css`
  - JS tùy biến:
    - `static/js/admin-common.js` – xử lý logout confirm, logic chung
    - `static/js/dashboard.js`, `static/js/doanhthu.js` – vẽ chart, thao tác trên dashboard/doanh thu
  - **Chart.js** – vẽ biểu đồ doanh thu, đơn hàng (load từ CDN trong `Revenue.html`, `dashboard.html`)
- **Giao diện / trang chính:**
  - `index.html` – landing page + public menu + widget chat hỗ trợ (nhân viên & AI)
  - `login.html`, `register.html` – đăng nhập/đăng ký người dùng
  - `dashboard.html` – tổng quan số liệu (doanh thu, đơn, khách, best seller)
  - `Menu.html` – quản lý menu (CRUD món bằng REST `/api/menu/...`)
  - `Order.html` – quản lý đơn hàng (tạo/sửa, xem chi tiết, QR thanh toán)
  - `Inventory.html` – quản lý kho hàng, nhập kho thông qua `/api/inventory/...`
  - `Staff.html` – quản lý nhân viên, chấm công, tính lương
  - `Booking.html` & `BookingForm.html` – quản lý đặt bàn
  - `Revenue.html` – trang phân tích doanh thu, biểu đồ, thống kê
  - `Setting.html` – cấu hình quán, giờ mở cửa, thông tin thanh toán, tài khoản

---

## 3. Kiến trúc & module chức năng

### 3.1. Kiến trúc tổng thể

Dự án tuân theo mô hình **Layered Architecture**:
- **Controller layer** (`controller/`)
  - Controller MVC (trả về view Thymeleaf): `ProductController`, `BookingController`, `AuthController`, ...
  - REST API controllers (trả về JSON): `OrderController`, `MenuController`, `InventoryController`, `StaffController`, `AttendanceController`, `DailyRevenueController`, `CustomerController`, `ReportController`, `PaymentSettingController`, `ShopSettingController`, `ChatController`, ...
  - `GlobalControllerAdvice` cung cấp `@ModelAttribute("currentUser")` cho mọi view
- **Service layer** (`service/`)
  - Chứa business logic: `OrderService`, `MenuService`, `DailyRevenueService`, `StaffService`, `AttendanceService`, `InventoryService`, `PaymentSettingService`, `ShopSettingService`, v.v.
- **Repository layer** (`repository/`)
  - Các interface extends `JpaRepository` / `CrudRepository` cho từng Entity
- **Model / Entity layer** (`model/`)
  - Định nghĩa entity cho JPA, enum cho trạng thái: `OrderStatus`, `StaffStatus`, `StaffRole`, `InventoryStatus`, `AttendanceStatus`, ...

### 3.2. Một số module tiêu biểu

- **Quản lý Menu**
  - REST: `/api/menu/categories`, `/api/menu/items`, `/api/menu/items/hide`
  - Giao diện: `Menu.html` + JavaScript gọi API để thêm/sửa món
- **Quản lý Đơn hàng**
  - REST: `/api/orders/...` (tạo đơn, cập nhật, lọc theo trạng thái, theo khoảng thời gian, xem chi tiết items)
  - Service xử lý nghiệp vụ, tính tổng tiền, lưu `OrderItem`, cập nhật thống kê khách hàng
  - UI: `Order.html` với modal tạo đơn, thêm món từ menu, tính tổng tiền, xuất QR thanh toán
- **Quản lý Doanh thu & Báo cáo**
  - REST: `/api/revenue/...` (doanh thu ngày, summary 7 ngày)
  - REST: `/api/reports/orders-7-days` – xuất CSV đơn hàng 7 ngày gần nhất
  - UI: `Revenue.html`, `dashboard.html` dùng Chart.js để hiển thị
- **Quản lý Kho**
  - REST: `/api/inventory/...` – danh sách, lọc theo trạng thái, nhập hàng, cập nhật item
  - UI: `Inventory.html` render bảng kho và form nhập hàng bằng JS
- **Quản lý Nhân viên & Chấm công**
  - REST: `/api/staff/...` – danh sách, theo role/status, lương, lương tháng hiện tại, lấy thông tin nhân viên hiện tại
  - REST: `/api/attendance/...` – check-in, check-out, trạng thái trong ngày
  - UI: `Staff.html` – thống kê, danh sách nhân viên, nút chấm công, modal chi tiết/ chỉnh sửa
- **Đặt bàn (Booking)**
  - Form public: `/booking` (trên landing page) gửi dữ liệu vào `BookingController`
  - Admin: `Booking.html` + `BookingForm.html`, controller `BookingController`
- **Cài đặt hệ thống**
  - REST: `/api/settings/shop`, `/api/settings/payment` – thông tin quán & cấu hình thanh toán (ngân hàng, MoMo)
  - UI: `Setting.html` – tab Thông tin quán / Hoạt động / Thông báo / Thanh toán QR / Tài khoản
- **Chat & AI**
  - REST: `/api/chat/ai` – nhận câu hỏi, build context từ DB, gọi Gemini, trả lời tiếng Việt
  - Widget chat trên `index.html` cho phép chuyển giữa "Nhắn cho nhân viên" và "Hỏi AI Gemini"

---

## 4. Cấu hình & chạy dự án

### 4.1. Yêu cầu môi trường
- JDK **21**
- Maven (hoặc dùng script `mvnw`/`mvnw.cmd` đi kèm)
- MySQL đã được cài đặt & chạy trên `localhost:3306`

### 4.2. Chuẩn bị database
1. Tạo database:
   ```sql
   CREATE DATABASE quanlyquancafe CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Kiểm tra / chỉnh cấu hình trong `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/quanlyquancafe
   spring.datasource.username=root
   spring.datasource.password=YOUR_PASSWORD
   spring.jpa.hibernate.ddl-auto=update
   ```

### 4.3. Cấu hình Gemini (tùy chọn)
Để sử dụng trợ lý AI Gemini:
- **Khuyến nghị:** không commit API key vào `application.properties`, mà cấu hình qua biến môi trường khi deploy thực tế.
- Khi chạy local để thử nghiệm, bạn có thể:
  - Đặt trong `application.properties` (chỉ trên máy local):
    ```properties
    gemini.api.key=YOUR_GEMINI_API_KEY
    gemini.model=gemini-2.5-flash
    ```
  - Hoặc map từ biến môi trường nếu bạn chỉnh sửa `ChatController` để đọc từ `System.getenv`.

### 4.4. Build & run

Chạy bằng Maven wrapper (không cần cài Maven global):

- Trên Windows (cmd / PowerShell):
  ```bash
  mvnw.cmd spring-boot:run
  ```

- Hoặc nếu đã cài Maven:
  ```bash
  mvn spring-boot:run
  ```

Ứng dụng mặc định chạy trên: `http://localhost:8080`

Một số đường dẫn chính:
- Trang landing / public: `http://localhost:8080/`
- Trang đăng nhập: `http://localhost:8080/login`
- Dashboard: `http://localhost:8080/dashboard` (cần quyền ADMIN)
- Các trang quản trị khác: `/Menu`, `/Order`, `/Revenue`, `/Inventory`, `/Staff`, `/Booking`, `/Setting`

---

## 5. Test & chất lượng mã

- Sử dụng `spring-boot-starter-test` + `spring-security-test` cho unit/integration test (file test mẫu: `QuanLyQuanCafeApplicationTests.java`).
- Có thể chạy toàn bộ test bằng:
  ```bash
  mvn test
  ```

---

## 6. Hướng phát triển thêm

Một số ý tưởng mở rộng:
- Bổ sung phân quyền chi tiết hơn (theo chức danh nhân viên, chi nhánh)
- Hoàn thiện cấu hình thông báo, hoạt động trong trang Cài đặt
- Tối ưu hóa logic tính lương, thêm bảng chấm công chi tiết theo ca
- Tích hợp gửi email / SMS xác nhận đặt bàn
- Bổ sung caching, phân trang & filter nâng cao cho các bảng dữ liệu lớn
