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
- Phân quyền bằng `@PreAuthorize`. Role → authority `ROLE_<code>`, permission → authority mang đúng `code`.
- Entity:
  - Có đủ `created_at` + `updated_at` → extends `BaseEntity`. Trường hợp khác tự khai báo `@CreatedDate` / `@LastModifiedDate`.
  - Không dùng `@CreatedBy` (không có `AuditorAware`); cột kiểu `created_by` do service set.
  - Xoá mềm: `deleted_at` + `@Where(clause = "deleted_at IS NULL")`.
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
- **Xoá mềm + unique:** dòng đã xoá vẫn giữ giá trị unique. Kiểm tra trùng dùng native query `count...IncludingDeleted`,
  không dùng `existsBy...`.
- Role chỉ đổi qua `PUT /users/{id}/roles`, không qua `PUT /users/{id}`. Chỉ SUPER_ADMIN được cấp/gỡ SUPER_ADMIN hoặc
  đổi role của SUPER_ADMIN; không ai tự đổi role của mình. Kiểm tra theo role trong DB, không theo JWT.
- Email được `trim` + chuyển chữ thường trước khi lưu và trước khi kiểm tra trùng.
- Với id `IDENTITY`, lỗi unique key ném ra ngay ở `persist()`, không đợi `flush()`.
- Trade-off đã chấp nhận: logout không thu hồi token; quyền nằm trong JWT nên đổi role hoặc khoá user có hiệu lực sau
  tối đa 30 phút.
- Lỗi còn tồn tại: `sort` sai field trả 500; `size` chưa giới hạn; docker-compose mở MySQL ở 3307 còn config mặc định
  là 3306; endpoint `prometheus` thiếu dependency; Swagger `servers` cố định `http://localhost:8383`.