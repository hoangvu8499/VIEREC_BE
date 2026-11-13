# CLAUDE.md

REST API cho hệ thống đào tạo phòng sự cố (VIEREC). Java 8, Spring Boot 2.7.18, MySQL 8.

## Quy tắc bắt buộc

1. **DB do chủ project quản lý bằng tay.** Schema gốc là `DB.sql` ở thư mục gốc repo, nhưng DB thật mới là nguồn đúng
   (kiểm tra bằng `SHOW CREATE TABLE`). Chỉ dùng bảng và cột đã có. Không Flyway/Liquibase, `ddl-auto: validate`,
   không tự thêm bảng, cột hay entity.
2. **Muốn thêm gì thì hỏi trước:** bảng, cột, dependency, module hay tính năng ngoài yêu cầu → đề xuất và chờ đồng ý.
3. Không commit thông tin DB. Để trong `application-local.yml` (đã gitignore).
4. Không bật `app.seed-admin` khi chưa được đồng ý (nó ghi dữ liệu vào DB).
5. Gặp gotcha hoặc quyết định không hiển nhiên thì ghi vào mục **Gotcha** cuối file. Mục nào hết đúng thì sửa hoặc xoá.

## Ràng buộc kỹ thuật

- Không nâng lên Boot 3, dùng `javax.*`.
- Docker chạy JRE 8 dù máy dev có JDK 17: cấm API Java 9+ (`List.of`, `var`, `Optional.isEmpty`, `String.isBlank`...).
- Test chạy H2 `create-drop`, nên không chứng minh entity khớp MySQL. Muốn chắc phải chạy app với profile `dev`.
- App chạy cổng 8383, FE chạy 8484 (origin CORS mặc định, không dùng `*` vì bật `allowCredentials`).

## Lệnh

```bash
./mvnw spring-boot:run       # cần MySQL
./mvnw test                  # H2, không cần DB
./mvnw verify -Pquality      # test + JaCoCo + Checkstyle
```

Sau khi xoá hoặc đổi tên class thì chạy `clean`. Trên Windows phải tắt app trước khi `clean` vì jar bị khoá.

## Quy ước

- Module mới làm theo khuôn `modules/user`.
- API prefix `/api/v1`, trả `ApiResponse.success(...)`, phân trang trả `PageResponse` (không trả `Page`).
- Lỗi: ném `BusinessException` / `ResourceNotFoundException` kèm `ErrorCode` dạng `VRC-<http>-<số>`, mỗi module một
  dải số (user: `x1xx`). Không tự dựng body lỗi trong controller.
- Phân quyền endpoint bằng annotation trong `security/access` (`@AdminOnly`, `@BusinessManagerOnly`), không viết chuỗi
  `@PreAuthorize` trong controller; khu vực cần quyền riêng thì thêm annotation mới ở đó. Role → authority `ROLE_<code>`,
  permission → authority mang đúng `code`.
- Entity:
  - Có đủ `created_at` + `updated_at` → extends `BaseEntity`. Trường hợp khác tự khai báo `@CreatedDate` / `@LastModifiedDate`.
  - Không dùng `@CreatedBy` (không có `AuditorAware`); cột kiểu `created_by` do service set.
  - Xoá mềm: `deleted_at` + `@Where(clause = "deleted_at IS NULL")`.
  - Quan hệ luôn `LAZY` (cả `@ManyToOne`, mặc định của JPA là EAGER). `default_batch_fetch_size` gom lazy-load của một
    trang thành một câu `IN`; danh sách mới thêm một ca vào `QueryCountApiTest` (số câu SQL không tăng theo số dòng).
  - Cột `ENUM` / `CHAR(n)` / `TEXT` bắt buộc có `columnDefinition`, nếu không `validate` sẽ fail.
- Mapping dùng MapStruct, không map tay trong service.
- Transaction: `@Transactional(readOnly = true)` ở class, `@Transactional` ở method ghi. `open-in-view` tắt.
- `ValidationMessages.properties` chỉ được chứa ASCII; tiếng Việt viết dạng `\uXXXX`.
- Code và comment viết tiếng Anh, dòng tối đa 120 ký tự.

## Xác thực

- Token **chỉ** nằm trong cookie `HttpOnly`: không trả trong body, không hỗ trợ header `Authorization: Bearer`.
  - `access_token`: 30 phút, `Path=/`. `refresh_token`: 30 ngày, `Path=/api/v1/auth`.
- Filter chỉ nhận token có `typ=access`.
- Xoá cookie phải dùng đúng tên, path và domain như lúc ghi (`AuthCookieService`).

## Gotcha

- **MapStruct + collection:** trong `updateEntity`, collection rỗng vẫn bị `clear()` + `addAll()` dù đặt `IGNORE`.
  Luôn ignore field collection (ví dụ `roles`, `userRoles`) và xử lý trong service.
- **MapStruct + default method:** method `String → String` trong mapper bị áp cho *mọi* field String. Đánh dấu
  `@Named` rồi gọi qua `expression`.
- **Xoá mềm + join:** `@Where` của entity không áp cho entity được join/fetch; query join `Course`/`User` phải tự
  thêm `deletedAt IS NULL`.
- JSON mặc định bỏ field `null` (`non_null`). Field FE cần thấy `null` thì gắn `@JsonInclude(ALWAYS)`.
- **Xoá mềm + unique:** dòng đã xoá vẫn giữ giá trị unique. Kiểm tra trùng dùng native query `count...IncludingDeleted`,
  không dùng `existsBy...`.
- Role chỉ đổi qua `PUT /users/{id}/roles`, không qua `PUT /users/{id}`. Chỉ SUPER_ADMIN được cấp/gỡ SUPER_ADMIN hoặc
  đổi role của SUPER_ADMIN; không ai tự đổi role của mình. Kiểm tra theo role trong DB, không theo JWT.
- Ghi danh mới (và ghi danh lại sau khi huỷ) là `PENDING`, admin duyệt sang `ENROLLED`. Video, tài liệu, file bài học
  chỉ mở cho admin và `EnrollmentStatus.LEARNING`: `GET /courses/{id}` bỏ trống các field đó, tải file chặn ở
  `LessonFileAccessGuard`. File thuộc module khác muốn chặn quyền đọc thì thêm một `FileAccessGuard`.
- PDF chứng chỉ (`CertificateServiceImpl.checkRead`): chủ chứng chỉ, người quản lý (role BUSINESS) của doanh nghiệp
  chủ chứng chỉ, admin. `GET /files/{id}?download=true` trả `Content-Disposition: attachment` (mặc định `inline`).
- Email được `trim` + chuyển chữ thường trước khi lưu và trước khi kiểm tra trùng.
- Với id `IDENTITY`, lỗi unique key ném ra ngay ở `persist()`, không đợi `flush()`.
- Trade-off đã chấp nhận: logout không thu hồi token; quyền nằm trong JWT nên đổi role hoặc khoá user có hiệu lực sau
  tối đa 30 phút.
- Lỗi còn tồn tại: `sort` sai field trả 500; `size` chưa giới hạn; docker-compose mở MySQL ở 3307 còn config mặc định
  là 3306; endpoint `prometheus` thiếu dependency; Swagger `servers` cố định `http://localhost:8383`.
- Tìm đơn vị xử lý sự cố (`support_points`): địa chỉ → toạ độ qua OpenStreetMap Nominatim (`app.geocoding.*`),
  chính sách của họ: User-Agent thật, tối đa 1 request/giây → `NominatimGeocodingService` cache theo địa chỉ. Test thì
  `@MockBean GeocodingService`. Thử tay trên Windows đừng dùng `curl` cho địa chỉ có dấu: tham số dòng lệnh bị hỏng
  mã hoá nên Nominatim trả sai/rỗng; dùng Python `urllib` hoặc trình duyệt.
- Doanh thu tính từ `course_enrollments.price` (giá khoá chụp lúc gửi yêu cầu) và `approved_at` (lần đầu vào
  ENROLLED/COMPLETED; về PENDING/CANCELLED thì xoá). Đổi giá khoá không làm đổi doanh thu hay số tiền đã báo học viên.
- Bài thi (`exams`, `exam_questions`, module `exam`): sửa câu hỏi qua collection `Exam.questions` (orphanRemoval) rồi
  `flush()`, không `save()`: entity đang managed, `save()` merge một bản sao nên câu mới trả về không có id. Import Excel
  (Apache POI, `ExamQuestionWorkbook`) validate từng dòng bằng chính rule của `ExamQuestionRequest`; lỗi trả qua
  `BusinessException(code, violations)` → `errors` như lỗi validate.
- Đề ngẫu nhiên: câu hỏi của bài thi là **kho**; lúc bắt đầu thi bốc `exams.questions_per_attempt` câu ngẫu nhiên
  (cả thứ tự) và lưu vào `exam_attempt_questions`; chấm đúng các câu này, không chấm theo cả kho. `NULL` = cả kho
  (bài thi tạo trước khi có cột). Kho ít câu hơn số câu mỗi đề → không cho bắt đầu (`EXAM_NOT_ENOUGH_QUESTIONS`);
  admin vẫn lưu được cài đặt đó (để cài trước rồi nhập câu sau). Xoá câu hỏi thì FK `ON DELETE CASCADE` gỡ câu khỏi
  đề đang làm (test H2 cần `@OnDelete` trên entity mới có cascade).
- Test đọc body bằng `getContentAsString()` sẽ hỏng tiếng Việt (MockMvc mặc định ISO-8859-1): dùng
  `getContentAsString(StandardCharsets.UTF_8)`. `jsonPath` thì không bị.
- Học viên thi (`exam_attempts`, `/courses/{id}/my-exam`): 1 lượt mỗi (exam, user), chỉ ENROLLED/COMPLETED. Hạn nộp và
  điểm đạt chụp lúc bắt đầu; nộp quá hạn + 1 phút (`SUBMIT_GRACE`) chấm 0 câu, lượt bỏ dở quá hạn được chấm khi đọc.
  Đạt so phân số chính xác (đúng × 10 ≥ điểm đạt × số câu), không so điểm đã làm tròn; đạt → enrollment COMPLETED.
  Bắt đầu thi cần xem ≥ 80% video (`LearningProgressService.summarize`, trừ khi enrollment đã COMPLETED).
- Tiến độ video (`lesson_progress`, `/courses/{id}/my-progress`): một dòng mỗi (user, lesson, video_key), giá trị chỉ
  tăng. `CourseVideos.keys` nhận dạng video giống FE (`lessonVideos` / `youtubeVideoId`): đổi một bên phải đổi bên kia.
  Video chưa mở (chưa biết thời lượng) thì chưa đủ điều kiện thi.
- Doanh nghiệp (`businesses`, `users.business_id`, role BUSINESS, module `business`): người quản lý = user có role
  BUSINESS **và** business_id; học viên doanh nghiệp = user có business_id nhưng không có BUSINESS
  (`UserRepository.IS_MANAGER`). `/my-business` lấy doanh nghiệp từ user đăng nhập, không nhận id từ client. Ghi danh hộ
  dùng `EnrollmentService.enrollAll` (theo lô, người đã có lượt đăng ký thì trả trong `skipped`, không ném lỗi:
  exception ném trong service khác cùng transaction làm transaction rollback-only dù có catch).
- **Chạy `spring-boot:run` ở chế độ nền (detached):** devtools fork một JVM con, JVM này chết ngay khi shell detach nên
  log chỉ in tới banner rồi `BUILD SUCCESS` mà không mở cổng 8383. Chạy nền phải thêm
  `-Dspring-boot.run.fork=false "-Dspring-boot.run.arguments=--spring.devtools.restart.enabled=false"`.
- **`Invalid method Code length 0 in class file ...`:** extension Java của VS Code (`redhat.java`) build bằng JRE riêng
  của nó (JDK 25) và ghi đè vào `target/classes` của Maven, trong khi project target Java 8 → class hỏng. `./mvnw clean`
  rồi chạy lại. Không chỉ xảy ra sau khi xoá/đổi tên class như mục **Lệnh** nói.
- **Đổi mật khẩu bằng SQL:** hash phải do `BCryptPasswordEncoder(12)` sinh (đúng `SecurityConfig.passwordEncoder()`),
  prefix `$2a$12$`, dài 60. Nhớ `must_change_password = 0` và `failed_login_count = 0`, nếu không user vẫn bị ép đổi
  mật khẩu hoặc còn đang bị chặn.
- Import tài khoản học viên doanh nghiệp (`BusinessMemberWorkbook`, `POST /my-business/members/import`): mật khẩu
  mặc định là field của form, không phải cột Excel, nên mọi dòng dùng chung một mật khẩu →
  `UserService.createBusinessAccounts` cache hash theo mật khẩu thô và chỉ gọi BCrypt một lần. Hash từng dòng thì
  500 dòng × ~0,25s (cost 12) = hơn 2 phút cho một request. Tài khoản tạo ra có `must_change_password = 1`, nhưng BE
  **không** chặn đăng nhập theo cờ này; FE phải tự đọc `mustChangePassword` ở `/auth/me` và ép đổi. File không có
  cột tên đăng nhập: **số CCCD là tên đăng nhập** (chủ project chốt), nên tài khoản khác đã dùng chuỗi CCCD đó làm
  username cũng chặn dòng, báo ở cột `cccd`.
- Check trùng khi import theo lô: `findTaken*` (native, thấy cả dòng đã xoá mềm) trả về các giá trị đã có để báo
  đúng số dòng Excel cho mọi dòng sai trong một lần, thay vì 4 query × mỗi dòng rồi chết ở dòng đầu tiên. Validate
  hết file trước rồi mới ghi; `createUser` vẫn giữ check từng dòng làm chốt cuối cho trường hợp tranh chấp.
- Import Excel dùng chung `common/excel/ExcelSheets` (mở file, đọc ô như người dùng thấy: số không thành dạng khoa
  học, không mất số 0 đầu; báo lỗi `rows[<dòng Excel>].<field>`). `ExamQuestionWorkbook` chưa chuyển sang dùng.
- Điểm hỗ trợ (admin, `SupportPointAdminController`, `@AdminOnly`; `/support-points/nearby` vẫn mở cho mọi người
  đăng nhập): form cho bỏ trống cả 2 toạ độ → server tự geocode từ địa chỉ + quận + tỉnh; import Excel thì **bắt
  buộc** toạ độ vì Nominatim chỉ cho 1 request/giây (500 dòng = hơn 8 phút). Toạ độ làm tròn 7 số lẻ (DECIMAL(10,7)).
  Ô toạ độ dạng chữ nhận cả dấu phẩy thập phân (Excel tiếng Việt). Xoá là xoá mềm.
- Test sắp xếp theo chữ tiếng Việt: H2 xếp "Đ" sau "Z", MySQL `utf8mb4_unicode_ci` xếp như "D". Dữ liệu test đừng
  phụ thuộc thứ tự của chữ có dấu.
- `QueryCountApiTest` đang fail sẵn (6 ca, `DataIntegrityViolation` ở `insertUser`): câu INSERT thủ công bỏ cột
  `must_change_password`, mà DDL H2 do Hibernate sinh không có default cho cột `NOT NULL` này (MySQL thật thì có
  `DEFAULT 0`). Không liên quan tới thay đổi mới; sửa bằng cách thêm cột đó vào câu INSERT của test.
