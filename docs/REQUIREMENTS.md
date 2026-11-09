# ClinicBook – Yêu cầu của case study

Tài liệu này ghi lại yêu cầu của đề bài (CodeGym – Spring Boot) và yêu cầu chức năng của ClinicBook, kèm nơi kiểm tra từng mục trong code và trên giao diện.

## 1. Yêu cầu kỹ thuật của đề bài

| # | Yêu cầu | Điểm | Cách đáp ứng | Kiểm tra bằng |
|---|---|---|---|---|
| 1 | Dùng Spring Boot và các `spring-boot-starter-*` | 5 | web, thymeleaf, data-jpa, security, validation, aop | `pom.xml` |
| 2 | Kết nối database bằng cấu hình | 5 | MySQL qua biến môi trường, test dùng H2 | `application.properties`, `application-test.properties` |
| 3 | Xác thực / phân quyền bằng JWT | 10 | `POST /api/auth/login` trả token HS256 với claim chuẩn (`iss`, `sub`, `aud`, `iat`, `nbf`, `exp`, `jti`), filter đọc header `Authorization: Bearer` rồi nạp người dùng từ database | `JwtServiceTest`, `ApiSecurityTest`; gọi API bằng `curl` (xem README) |
| 4 | Phân quyền theo method | 5 | `@EnableMethodSecurity` + `@PreAuthorize` ở lớp `service/impl` | `AppointmentServiceTest` (`methodSecurity_*`) |
| 5 | CRUD ít nhất 3 đối tượng, mô hình controller → interface service → implementation → repository | 20 | Specialty, Doctor, TimeSlot, Appointment | `AdminHubTest`, `DoctorScheduleTest`, `BookingFlowTest` |
| 6 | Tách luồng `/api` (REST) và luồng MVC | 5 | Hai `SecurityFilterChain`: `@Order(1)` `/api/**` stateless JWT, `@Order(2)` web form login + session + CSRF | `SecurityConfig`, `controller/api`, `controller/web` |
| 7 | Đa ngôn ngữ (i18n) ở ít nhất 3 màn hình | 10 | `messages_vi/en.properties`, `CookieLocaleResolver`, nút VI/EN; tên chuyên khoa và "BS." / "Dr." đổi theo ngôn ngữ | Thêm `?lang=en` vào bất kỳ trang; `WebPagesTest`, `WebAccessTest` |
| 8 | Thymeleaf + Bootstrap, responsive ít nhất 2 màn hình | 10 | `fragments/layout.html`, lưới `row-cols-*`, `table-responsive`, navbar thu gọn dưới 1200px | Thu nhỏ cửa sổ / bật DevTools ở các trang chủ, bác sĩ, admin |
| 9 | AOP cho ít nhất 1 nghiệp vụ | 10 | `@Audited` + `AuditAspect` ghi nhật ký; `PerformanceAspect` log thời gian | `/admin/audit`; `AuditMessageTest`, `AppointmentServiceTest` |
| 10 | HttpSession | 5 | Lựa chọn đặt lịch dở dang lưu trong session (`BookingDraft`) | `BookingFlowTest.fullFlowCreatesAPendingAppointment` |
| 11 | Cookie | 5 | `clinicbook-lang`, `clinicbook-consent`, `recentDoctors` (chỉ khi khách đồng ý) | `WebAccessTest` (nhóm cookie) |
| 12 | Jenkins local / deploy | 10 | `Jenkinsfile`, `deploy/deploy.sh`, `deploy/deploy.bat` | Chạy pipeline (xem README) |

## 2. Yêu cầu chức năng theo vai trò

### Khách
- Xem trang chủ, danh sách và chi tiết bác sĩ (lọc theo chuyên khoa / tên), xem tin tức.
- Đăng ký (luôn tạo bệnh nhân) và đăng nhập. Đăng xuất quay về trang chủ.
- Chọn ngôn ngữ; chọn "Chấp nhận tất cả" hoặc "Chỉ cần thiết" ở banner cookie.

### Bệnh nhân (PATIENT)
- Đặt lịch theo 3 bước: **chuyên khoa → bác sĩ của chuyên khoa → ngày / giờ trống**, nhập lý do, xác nhận.
- Xem lịch hẹn của mình, hủy lịch (khung giờ được trả lại).
- Không thể đặt hai lần cùng một khung giờ; nếu hai người bấm cùng lúc thì chỉ một người thành công.
- Đổi mật khẩu.

### Bác sĩ (DOCTOR)
- Xem hàng đợi bệnh nhân theo ngày, xác nhận / hoàn thành / từ chối lịch của mình.
- Quản lý khung giờ làm việc theo tuần. Quy tắc: 06:00 – 22:00, mỗi khung 15 – 240 phút, tối đa 90 ngày tới, không chồng giờ, không thêm vào quá khứ; khung đã có người đặt không xóa được.

### Quản trị viên (ADMIN)
- Tổng quan: số liệu, mức độ bận theo chuyên khoa, việc cần theo dõi, bác sĩ nhiều lịch nhất, hoạt động gần đây.
- Quản lý chuyên khoa (tên Việt + tên Anh) và bác sĩ (tạo kèm tài khoản): thêm / sửa / xóa có hộp thoại xác nhận; danh sách phân trang 10 dòng.
- Xem lịch tổng theo ngày, lịch sử lịch hẹn và nhật ký hệ thống (lọc, phân trang).
- **Không** đặt, xác nhận, hoàn thành hay hủy lịch hẹn: việc này thuộc về bệnh nhân và bác sĩ.

## 3. Yêu cầu phi chức năng

| Mục | Nội dung |
|---|---|
| Bảo mật | Mật khẩu băm BCrypt; CSRF cho form web; kiểm tra quyền ở service chứ không chỉ ở URL; truy cập trang không đủ quyền (403) quay về trang chủ kèm thông báo |
| Toàn vẹn dữ liệu | Đặt lịch khóa dòng slot (`PESSIMISTIC_WRITE`) và có `@Version`; hủy lịch trả slot; không xóa chuyên khoa còn bác sĩ, bác sĩ còn lịch hẹn, slot đã đặt |
| Trải nghiệm | Responsive từ điện thoại tới màn hình rộng; thông báo thành công tự tắt sau 5 giây; giữ vị trí cuộn sau khi cập nhật; chuyển trang có hiệu ứng nhẹ (tôn trọng `prefers-reduced-motion`) |
| Cấu trúc code | Mỗi service có interface (`service/`) và implementation (`service/impl/`); controller chỉ gọi interface; enum nằm ở `entity/enums`; thông báo lỗi dùng hằng trong `constant/ErrorMessage` (tên hằng → khóa i18n); source không có dòng comment |
| Bảng mã | Mọi file UTF-8 (`.editorconfig`); file `.properties` đọc bằng UTF-8 |
| Kiểm thử | `./mvnw test` chạy trên H2, không cần MySQL; có test đặt trùng đồng thời (`BookingConcurrencyTest`) |

## 4. Tài khoản dùng thử

| Vai trò | Tài khoản | Mật khẩu |
|---|---|---|
| Admin | `admin` | `admin123` |
| Bác sĩ | `doctor1` … `doctor25` | `doctor123` |
| Bệnh nhân | `patient1` … `patient50` | `patient123` |

## 5. Danh sách kiểm tra nhanh trên giao diện

1. `/logout` (hoặc bấm Đăng xuất) → về trang chủ, có thông báo ngắn.
2. Đăng nhập bệnh nhân, mở `/admin` → về trang chủ kèm thông báo "không có quyền".
3. Bệnh nhân: Đặt lịch → chọn chuyên khoa → chọn bác sĩ → chọn giờ → xác nhận; thông báo thành công biến mất sau 5 giây.
4. Bác sĩ: Lịch làm việc → ô ngày đã điền sẵn; nhập giờ 05:00 hoặc 23:00 → báo lỗi giờ làm việc.
5. Admin: Tổng quan → "Hoạt động gần đây" đọc được như câu; Bác sĩ → xóa mở hộp thoại; danh sách 10 dòng / trang.
6. Đổi `?lang=en`: tên chuyên khoa và "BS." → "Dr." đổi theo.
7. Lần đầu vào trang: banner cookie hiện ra; chọn "Chỉ cần thiết" thì mục "Bác sĩ bạn đã xem gần đây" không xuất hiện.
