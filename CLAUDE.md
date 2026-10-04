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
