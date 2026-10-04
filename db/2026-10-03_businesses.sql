-- Khách hàng doanh nghiệp: admin tạo doanh nghiệp + tài khoản quản lý (role BUSINESS).
-- Học viên thuộc doanh nghiệp: users.business_id (NULL = học viên tự do). Người quản lý cũng có business_id.

CREATE TABLE businesses (
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name          VARCHAR(200)     NOT NULL COMMENT 'Tên doanh nghiệp',
    tax_code      VARCHAR(14)      NOT NULL COMMENT 'Mã số thuế: 10 số hoặc 10 số-3 số',
    address       VARCHAR(255)     NOT NULL,
    phone_number  VARCHAR(15)      NOT NULL COMMENT 'SĐT liên hệ',
    email         VARCHAR(255)     NOT NULL COMMENT 'Email liên hệ',
    status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'INACTIVE: người quản lý không dùng được chức năng doanh nghiệp',
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_businesses_tax_code (tax_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN business_id BIGINT UNSIGNED NULL COMMENT 'Doanh nghiệp của học viên / người quản lý; NULL = tự do' AFTER status,
    ADD KEY idx_users_business (business_id),
    ADD CONSTRAINT fk_users_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE SET NULL;

INSERT INTO roles (code, name, description) VALUES
 ('BUSINESS', 'Business', 'Quản lý doanh nghiệp: tạo tài khoản, ghi danh và theo dõi học viên của doanh nghiệp');

-- Tiến độ xem video (trước đây chỉ lưu localStorage của học viên). Mỗi dòng: một video của một bài học.
-- video_key: 'youtube-<mã video>' (video_url của bài) hoặc 'file-<id file>' (file VIDEO đính kèm).
CREATE TABLE lesson_progress (
    id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED  NOT NULL,
    lesson_id         BIGINT UNSIGNED  NOT NULL,
    video_key         VARCHAR(100)     NOT NULL,
    duration_seconds  INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Thời lượng video',
    watched_seconds   INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Số giây khác nhau đã xem (không đếm xem lại)',
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_progress_video (user_id, lesson_id, video_key),
    KEY idx_lesson_progress_lesson (lesson_id),
    CONSTRAINT fk_lesson_progress_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
