# ClinicBook - Ứng dụng đặt lịch khám trực tuyến

Bệnh nhân chọn chuyên khoa → bác sĩ → khung giờ trống và đặt lịch; bác sĩ quản lý lịch làm việc và xác nhận lịch hẹn; quản trị viên quản lý chuyên khoa, bác sĩ và xem nhật ký hệ thống. Ứng dụng có hai luồng tách biệt: giao diện web (Thymeleaf, session) và REST API (`/api`, JWT).

## Công nghệ sử dụng

| Nhóm | Công nghệ | Dùng để |
|---|---|---|
| Ngôn ngữ, build | Java 17, Maven (Maven Wrapper `mvnw`) | Biên dịch, đóng gói jar |
| Framework | Spring Boot 3.3.5 | Nền tảng của ứng dụng |
| Web | Spring Web MVC | Controller giao diện (`controller/web`) và REST (`controller/api`) |
| Giao diện | Thymeleaf, Thymeleaf Extras Spring Security 6 | Render trang HTML phía server, kiểm tra quyền ngay trong template |
| | Bootstrap 5.3.3, Bootstrap Icons 1.11.3 (CDN), Google Fonts (Plus Jakarta Sans), JavaScript thuần | Bố cục responsive, biểu tượng, banner cookie, thông báo tự tắt |
| Dữ liệu | Spring Data JPA, Hibernate | Truy cập database qua repository, `ddl-auto=update` |
| | MySQL 8 (`mysql-connector-j`) | Database chạy thật |
| | H2 (bộ nhớ) | Database khi chạy test |
| Bảo mật | Spring Security | Hai `SecurityFilterChain`: form login + session + CSRF cho web, stateless cho `/api` |
| | JJWT 0.12.6 | Tạo và kiểm tra JWT (HS256) |
| | BCrypt | Băm mật khẩu |
| | `@EnableMethodSecurity`, `@PreAuthorize` | Phân quyền theo method ở lớp service |
| Kiểm tra dữ liệu | Spring Validation (Jakarta Validation) | Kiểm tra form và request |
| AOP | Spring AOP (AspectJ) | `AuditAspect` ghi nhật ký nghiệp vụ, `PerformanceAspect` đo thời gian chạy service |
| Đa ngôn ngữ | Spring `MessageSource`, `CookieLocaleResolver`, `LocaleChangeInterceptor` | Giao diện Việt / Anh (`?lang=vi`, `?lang=en`) |
| Phiên và cookie | `HttpSession`, Cookie | Giữ lựa chọn đặt lịch dở dang, ngôn ngữ, đồng ý cookie, bác sĩ đã xem |
| Tiện ích | Lombok | Giảm code getter / setter |
| Kiểm thử | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test, Mockito | Test đơn vị, test web, test API, test đặt trùng đồng thời |
| CI/CD | Jenkins (Pipeline), script `deploy.sh` / `deploy.bat` | Build, test, đóng gói và deploy local |
| Công cụ gọi API | Postman | Kiểm tra REST API |

## Chạy ứng dụng

1. Cài JDK 17 và MySQL 8.
2. Tạo database trống `clinicbook` (utf8mb4), ví dụ: `CREATE DATABASE clinicbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`. Khi chạy lần đầu, Hibernate (`ddl-auto=update`) tự tạo các bảng.
3. Cấu hình kết nối trong `src/main/resources/application.properties` hoặc bằng biến môi trường: `DB_HOST` (mặc định `localhost`), `DB_PORT` (`3306`), `DB_NAME` (`clinicbook`), `DB_USERNAME` (`root`), `DB_PASSWORD`, `JWT_SECRET` (chuỗi Base64, tối thiểu 32 byte; token luôn ký bằng HS256), `PORT` (`8080`).
4. Chạy:

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Mở <http://localhost:8080>.

Các tài khoản dưới đây có sẵn khi database đã được nạp dữ liệu mẫu của tác giả. Với database trống, hãy đăng ký bệnh nhân tại `/register`.

| Vai trò | Tài khoản | Mật khẩu |
|---|---|---|
| Admin | `admin` | `admin123` |
| Bác sĩ | `doctor1` ... `doctor25` (mỗi chuyên khoa 5 bác sĩ: 1-5 Nội tổng quát, 6-10 Nhi khoa, 11-15 Tim mạch, 16-20 Da liễu, 21-25 Răng hàm mặt) | `doctor123` |
| Bệnh nhân | `patient1` ... `patient50` | `patient123` |

Chạy test (dùng H2 trong bộ nhớ, không cần MySQL): `./mvnw clean test`

Yêu cầu chức năng và cách kiểm tra từng mục: xem [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md).

Nếu file `messages_vi.properties` hiện chữ lỗi trong IDE: file đã lưu UTF-8; IntelliJ → Settings → Editor → File Encodings → *Default encoding for properties files* = `UTF-8` (file `.editorconfig` ở thư mục gốc đã khai báo sẵn).

## Giao diện web

| Đường dẫn | Ai dùng | Nội dung |
|---|---|---|
| `/`, `/doctors`, `/doctors/{id}` | mọi người | Trang chủ, tìm / lọc bác sĩ, xem khung giờ trống |
| `/news`, `/news/{id}` | mọi người | Tab Tin tức: lọc theo chủ đề / từ khóa, đọc bài |
| `/booking`, `/booking/confirm` | PATIENT | Đặt lịch khám (menu "Đặt lịch khám"): chọn chuyên khoa → chọn bác sĩ của chuyên khoa đó → lý do, ngày, giờ → xác nhận. Lựa chọn dở dang giữ trong `HttpSession` (`BookingDraft`) |
| `/patient/dashboard` | PATIENT | Trang của bệnh nhân (đích sau khi đăng nhập): lịch khám tiếp theo, lịch sắp tới / lịch sử, thông tin tài khoản, hủy lịch |
| `/account/password` | đã đăng nhập (mọi vai trò) | Đổi mật khẩu: kiểm tra mật khẩu hiện tại, mật khẩu mới phải khác, độ mạnh hiển thị trực tiếp; ghi nhật ký `PASSWORD_CHANGED` (không ghi mật khẩu) |
| `/login`, `/register` | khách | Đăng nhập, đăng ký bệnh nhân |
| `/patient/appointments` | PATIENT | Lịch hẹn của tôi, hủy lịch |
| `/doctor/dashboard` | DOCTOR | Trang làm việc (đích sau khi đăng nhập): hàng đợi bệnh nhân theo ngày, lọc trạng thái / tìm tên, bệnh nhân tiếp theo, lịch chờ xác nhận, biểu đồ 7 ngày |
| `/doctor/schedule` | DOCTOR | Thời khóa biểu theo tuần (thứ 2 – chủ nhật, đổi tuần bằng `?week=`): khung giờ trống / đã đặt theo màu trạng thái, thêm / xóa khung giờ, xác nhận hoặc từ chối lịch chờ |
| `/admin` | ADMIN | Tổng quan vận hành (đích sau khi đăng nhập): lịch hẹn hôm nay, số bác sĩ / bệnh nhân, tỷ lệ lấp đầy khung giờ 7 ngày tới, mức độ bận theo chuyên khoa, lịch sắp tới (chỉ xem), mục "Việc cần theo dõi", "Bác sĩ nhiều lịch nhất", "Hoạt động gần đây" (mỗi dòng là một câu: ai đã làm gì với lịch hẹn của ai) |
| `/admin/doctors` | ADMIN | Quản lý bác sĩ: bảng có tìm theo tên / lọc chuyên khoa, mức độ bận 7 ngày tới, phân trang 10 dòng / trang, thêm (kèm tài khoản đăng nhập) / sửa / xóa (`/new`, `/{id}/edit`, `/{id}/delete`, xóa có hộp thoại xác nhận) |
| `/admin/specialties` | ADMIN | Quản lý chuyên khoa: thẻ có số bác sĩ và mức độ bận 7 ngày tới, thêm / sửa / xóa (không xóa được khi còn bác sĩ) |
| `/admin/appointments` | ADMIN | Lịch tổng theo ngày (cột là bác sĩ, lọc chuyên khoa, chuyển ngày) và lịch sử mọi lịch hẹn (tab trạng thái, tìm theo mã / bệnh nhân / bác sĩ / lý do, phân trang 10 dòng / trang), mỗi lịch có nút xem nhật ký riêng. **Admin chỉ xem**, không tạo / xác nhận / hủy lịch hẹn |
| `/admin/audit` | ADMIN | Nhật ký AOP hiển thị thành câu dễ đọc (VI / EN), lọc theo ngày, hành động và từ khóa, phân trang, số liệu trong ngày |

## REST API (`/api`, JWT)

Đăng nhập lấy token rồi gửi header `Authorization: Bearer <token>`.

```bash
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"username":"patient1","password":"patient123"}'
```

| Method | Đường dẫn | Quyền |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | công khai |
| GET | `/api/specialties`, `/api/specialties/{id}` | công khai |
| POST / PUT / DELETE | `/api/specialties[/{id}]` | ADMIN |
| GET | `/api/doctors?specialtyId=&keyword=`, `/api/doctors/{id}`, `/api/doctors/{id}/slots` | công khai |
| POST / PUT / DELETE | `/api/doctors[/{id}]` | ADMIN |
| POST / DELETE | `/api/slots[/{id}]` | DOCTOR (lịch của mình), ADMIN |
| POST | `/api/appointments` (`{"slotId":1,"reason":"..."}`) | PATIENT |
| GET | `/api/appointments`, `/api/appointments/{id}` | chủ lịch hẹn, bác sĩ của lịch hẹn, ADMIN (chỉ xem) |
| PATCH | `/api/appointments/{id}/status` (`{"status":"CONFIRMED"}`) | DOCTOR của lịch hẹn |
| DELETE | `/api/appointments/{id}` (hủy lịch) | chủ lịch hẹn (PATIENT), DOCTOR của lịch hẹn |

Lỗi trả về JSON `{status, message}`: 400 dữ liệu sai, 401 chưa đăng nhập / token sai, 403 không đủ quyền, 404 không tìm thấy, 409 vi phạm quy tắc (ví dụ slot đã có người đặt).

## Quy tắc nghiệp vụ

- Đặt lịch khóa dòng slot (`PESSIMISTIC_WRITE`) và slot có `@Version`, nên hai bệnh nhân không thể đặt cùng một slot.
- Hủy lịch trả slot về `AVAILABLE`. Trạng thái lịch hẹn: `PENDING → CONFIRMED → COMPLETED`, hoặc `CANCELLED` từ `PENDING` / `CONFIRMED`.
- Bệnh nhân chỉ xem / hủy lịch của mình, bác sĩ chỉ xử lý lịch của mình (kiểm tra ở service, không chỉ ở URL). Admin chỉ xem lịch hẹn: không đặt, xác nhận, hoàn thành hay hủy (`@PreAuthorize` ở `AppointmentServiceImpl`).
- Khung giờ làm việc: 06:00 – 22:00, mỗi khung 15 – 240 phút, tối đa 90 ngày tới, không chồng giờ (kiểm tra ở `SlotServiceImpl` và ngay trên form).
- Truy cập trang không đủ quyền (403) được đưa về trang chủ kèm thông báo; đăng xuất (`/logout`) cũng về trang chủ.
- Thông báo thành công tự biến mất sau 5 giây; xóa luôn qua hộp thoại xác nhận (không dùng `alert`).
- Đăng ký công khai luôn tạo vai trò PATIENT; tài khoản bác sĩ chỉ do ADMIN tạo.
- Không xóa được chuyên khoa còn bác sĩ, bác sĩ đã có lịch hẹn, hoặc slot đã được đặt.

## Jenkins (local)

Cần trên máy Jenkins: JDK 17 (`JAVA_HOME`), Git, truy cập Internet (Maven wrapper tự tải Maven).

1. Jenkins → New Item → **Pipeline** → *Pipeline script from SCM* → Git → URL repo này, `Script Path` = `Jenkinsfile`.
2. Đặt biến môi trường cho máy chạy app: `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (Manage Jenkins → System → Global properties).
3. *Build with Parameters*: `DEPLOY` (có deploy hay không), `DEPLOY_DIR`, `APP_PORT` (mặc định 8081).

Các stage: Checkout → `mvnw clean verify` (chạy test, báo cáo JUnit) → lưu jar (archive) → Deploy (`deploy.sh` / `deploy.bat` dừng bản cũ, chép jar mới, chạy nền, log ở `clinicbook.log`).

## Cấu trúc thư mục

```
src/main/java/vn/codegym/clinicbook
├── config/       SecurityConfig (2 chain), WebConfig (i18n)
├── security/     JwtService, JwtAuthenticationFilter, CustomUserDetailsService, CurrentUser
├── constant/     ErrorMessage (tên hằng → khóa i18n, ví dụ WRONG_PASSWORD)
├── entity/       User, Specialty, Doctor, Patient, TimeSlot, Appointment, AuditLog
│   └── enums/    Role, AppointmentStatus, SlotStatus, ArticleCategory
├── repository/   Spring Data JPA
├── service/      interface của từng service (controller chỉ gọi interface)
│   └── impl/     XxxServiceImpl: nghiệp vụ + @PreAuthorize
├── aop/          Audited, AuditAspect, PerformanceAspect
├── dto/          request / response
├── controller/api/   REST (JWT)
├── controller/web/   MVC + Thymeleaf (session)
├── exception/    BusinessException, ResourceNotFoundException
└── util/         RecentDoctorsCookie, DoctorNames (BS. / Dr. theo ngôn ngữ)
src/main/resources/{templates, static, i18n}
```

## Ghi chú

- Bootstrap và Bootstrap Icons nạp từ CDN (jsDelivr), cần Internet khi xem giao diện.
- Tham khảo ý tưởng domain từ project `OnlineDoctorBooking`; code được viết lại theo yêu cầu case study (thêm Thymeleaf/MVC, session, cookie, i18n, AOP, phân quyền theo method, sửa lỗi đặt trùng lịch và kiểm tra quyền theo từng bản ghi).
