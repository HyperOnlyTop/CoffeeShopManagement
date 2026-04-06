# QuanLyQuanCafe – Hệ thống quản lý quán cà phê

## 1. Tổng quan dự án

QuanLyQuanCafe là một ứng dụng **Spring Boot** mô phỏng hệ thống quản lý quán cà phê hiện đại, tập trung vào trải nghiệm quản trị trên web với giao diện dashboard đẹp, trực quan.

Các nghiệp vụ chính:
- Quản lý menu đồ uống / món ăn
- Quản lý đơn hàng tại quán (tại bàn / mang đi)
- Quản lý kho nguyên liệu (tồn kho, nhập hàng)
- Quản lý nhân viên, chấm công và tính lương cơ bản
- Quản lý đặt bàn (booking) cho khách, gán bàn trực tiếp từ trang Quản lý bàn
- Hệ thống nhắc việc cho nhân viên theo mốc thời gian đặt bàn
- Theo dõi doanh thu, báo cáo, xuất CSV
- Hệ thống khách hàng và đánh giá (customer & review)
- Tích hợp **trợ lý AI dùng Gemini** cho tư vấn khách hàng (menu/thông tin quán), có ràng buộc không trả lời dữ liệu quản trị nhạy cảm
- Chat hỗ trợ **khách hàng <-> nhân viên** theo thời gian thực gần realtime (polling), có trang quản trị hội thoại cho nhân viên

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
- Chat AI sử dụng **dữ liệu menu thực** từ DB làm context theo ngữ cảnh hỏi đáp.
- AI được ràng buộc chỉ trả lời chủ đề khách hàng (menu, đồ uống, thông tin quán, gợi ý đồ uống), và từ chối các chủ đề nhạy cảm như:
  - thông tin quản trị/admin
  - nhân sự nội bộ
  - doanh thu/lợi nhuận/báo cáo tài chính
  - dữ liệu khách hàng khác, bảo mật hệ thống

### Frontend (trong Spring MVC)
- **Template engine:** Thymeleaf (có `springsecurity6` extras)
- **UI / CSS / JS:**
  - **Bootstrap 5** – layout, responsive UI
  - **Bootstrap Icons** – icon cho dashboard
  - **Google Fonts (Inter)** – font chữ
  - CSS tùy biến: `src/main/resources/static/css/doanhthu.css`, `style.css`
  - JS tùy biến:
    - `static/js/admin-common.js` – xử lý logout confirm, logic chung
    - `static/js/doanhthu.js` – chart và cập nhật dữ liệu trang doanh thu
    - `static/js/landing.js` – widget chat AI + chat nhân viên ở trang chủ
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

### 3.2. Module & flow hiện tại (rút gọn)

- **Dashboard (vận hành)**
  - Trang: `dashboard.html`
  - Tập trung số liệu vận hành trong ngày: đơn chờ xử lý, cảnh báo kho, lịch đặt bàn hôm nay, best seller, hoạt động gần đây
  - Không chứa phân tích tài chính sâu (phần đó nằm ở `Revenue.html`)

- **Doanh thu & báo cáo**
  - API: `/api/revenue/summary?days=...`, `/api/revenue/by-category?days=...`
  - API xuất báo cáo: `/api/reports/revenue?days=...` (CSV UTF-8 BOM, delimiter `;`, tương thích Excel)
  - Trang: `Revenue.html` (biểu đồ + doanh thu/giá vốn/lãi gộp theo kỳ và theo danh mục)

- **Menu**
  - API: `/api/menu/categories`, `/api/menu/items`, `/api/menu/items/hide`
  - Trang: `Menu.html` (CRUD món, trạng thái hiển thị, giá bán)

- **Đặt bàn (khách ngoài trang chủ)**
  - Endpoint: `POST /booking` hoặc API `POST /api/public/bookings`
  - Rule hiện tại: khách đặt phải trước ít nhất **2 giờ** (`BookingPolicy.MIN_LEAD_HOURS`)
  - Không cho đặt trong quá khứ, không cho trùng đúng khung giờ
  - Trạng thái mặc định: `CONFIRMED`

- **Đặt bàn (nhân viên trong admin)**
  - Trang: `Booking.html`, `BookingForm.html`
  - Endpoint: `/Booking/new`, `/Booking/edit/{id}`, `/Booking/save`
  - Role thao tác: `ADMIN`, `CASHIER`, `SERVER`
  - Rule hiện tại: booking tạo/sửa bởi staff phải trước ít nhất **20 phút** (`BookingPolicy.MIN_LEAD_MINUTES_STAFF`)

- **Quản lý bàn**
  - API chính: `/api/tables/...`
  - Trạng thái bàn: `AVAILABLE`, `RESERVED`, `OCCUPIED`
  - Có thể thao tác trực tiếp từ màn hình bàn:
    - gán/bỏ gán booking cho bàn
    - check-in booking
    - hủy booking
    - giải phóng bàn
  - Booking `CONFIRMED` đã gán bàn sẽ block tạo đơn mới trên bàn đó cho đến khi `CHECKED_IN` hoặc `CANCELLED`

- **Tạo đơn mới / đơn hàng**
  - API chính: `/api/orders/...`
  - Trang: `Order.html`
  - Hỗ trợ tạo đơn tại bàn / mang đi, cập nhật trạng thái, cập nhật thanh toán, xem chi tiết item
  - Quyền theo method đã cấu hình ở `SecurityConfig` (GET/POST/PUT tách riêng theo role)

- **Kho hàng**
  - API chính: `/api/inventory/...`
  - Trang: `Inventory.html` (lọc tồn kho, cập nhật nguyên liệu, nhập kho)

- **Khách hàng & tích điểm**
  - API khách hàng: `/api/customers/...`
  - API loyalty: `/api/loyalty/...`
  - Tích điểm phát sinh theo đơn hoàn tất, hỗ trợ quy đổi/điều chỉnh điểm theo chính sách trong hệ thống

- **Chấm công nhân viên**
  - API chính: `/api/attendance/check-in`, `/api/attendance/check-out`, `/api/attendance/my-records`
  - Có kiểm tra:
    - nhân viên còn hiệu lực làm việc
    - trong cửa sổ chấm công hợp lệ theo ca
    - đã được phân ca trong ngày
  - Hỗ trợ ca liền kề (merge thành work block) để tính giờ công

- **Chat & AI**
  - AI: `POST /api/chat/ai` (chỉ trả lời chủ đề khách hàng: menu/thông tin quán/gợi ý đồ uống)
  - Chat hỗ trợ:
    - Khách: `/api/chat/support/...`
    - Nhân viên: `/api/staff/chat/...`
  - Trang admin xử lý hội thoại: `Messages.html`
  - Quyền xử lý chat nhân viên: `ADMIN`, `CASHIER`, `SERVER`

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

Khi ứng dụng khởi động, hệ thống tự động seed dữ liệu mẫu (idempotent) thông qua các initializer để có thể đăng nhập và test flow ngay.

Một số đường dẫn chính:
- Trang landing / public: `http://localhost:8080/`
- Trang đăng nhập: `http://localhost:8080/login`
- Dashboard: `http://localhost:8080/dashboard` (cần quyền ADMIN)
- Các trang quản trị khác: `/Menu`, `/Order`, `/Revenue`, `/Inventory`, `/Staff`, `/Booking`, `/Messages`, `/Setting`

---

## 5. Test & chất lượng mã

- Sử dụng `spring-boot-starter-test` + `spring-security-test` cho unit/integration test (file test mẫu: `QuanLyQuanCafeApplicationTests.java`).
- Có thể chạy toàn bộ test bằng:
  ```bash
  mvn test
  ```

---

## 6. Trạng thái phát triển

Dự án hiện đã hoàn thiện các luồng nghiệp vụ cốt lõi phục vụ vận hành quán cà phê và đang được tiếp tục rà soát, tối ưu và mở rộng theo phản hồi thực tế.

Trong các phiên bản tiếp theo, hệ thống sẽ tiếp tục được cải tiến về hiệu năng, trải nghiệm người dùng và mức độ hoàn thiện nghiệp vụ.
