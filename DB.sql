-- =============================================
-- Database: VIEREC (MySQL 8.0)
-- Phần: User và phân quyền
-- =============================================

CREATE DATABASE IF NOT EXISTS VIEREC CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE VIEREC;

-- 1. Người dùng
CREATE TABLE users (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)      NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL COMMENT 'Chỉ lưu hash (bcrypt/argon2), không lưu mật khẩu gốc',
    first_name      VARCHAR(50)      NOT NULL,
    last_name       VARCHAR(50)      NOT NULL,
    cccd            CHAR(12)         NULL     COMMENT 'Số căn cước công dân 12 chữ số',
    date_of_birth   DATE             NULL,
    address         VARCHAR(255)     NULL,
    phone_number    VARCHAR(15)      NULL,
    email           VARCHAR(255)     NULL,
    status          ENUM('ACTIVE','INACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME         NULL     COMMENT 'Xóa mềm',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_cccd (cccd),
    UNIQUE KEY uk_users_phone (phone_number),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Vai trò
CREATE TABLE roles (
    id           INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)   NOT NULL COMMENT 'Mã dùng trong code, vd SUPER_ADMIN',
    name         VARCHAR(100)  NOT NULL COMMENT 'Tên hiển thị',
    description  VARCHAR(255)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Quyền chi tiết (dùng khi cần phân quyền theo chức năng)
CREATE TABLE permissions (
    id           INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    code         VARCHAR(100)  NOT NULL COMMENT 'vd USER_CREATE, USER_VIEW',
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(255)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. User <-> Role (nhiều-nhiều)
CREATE TABLE user_roles (
    user_id      BIGINT UNSIGNED NOT NULL,
    role_id      INT UNSIGNED    NOT NULL,
    assigned_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by  BIGINT UNSIGNED NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user     FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role     FOREIGN KEY (role_id)     REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_assigner FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Role <-> Permission (nhiều-nhiều)
CREATE TABLE role_permissions (
    role_id        INT UNSIGNED NOT NULL,
    permission_id  INT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permissions_perm (permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu vai trò ban đầu
INSERT INTO roles (code, name, description) VALUES
 ('SUPER_ADMIN', 'Super Admin', 'Toàn quyền hệ thống'),
 ('ADMIN',       'Admin',       'Quản trị viên'),
 ('TRAINEE',     'Trainee',     'Học viên');

-- =============================================
-- Phần: Khoá học
-- =============================================

-- 6. Khoá học
CREATE TABLE courses (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name           VARCHAR(200)     NOT NULL,
    description    TEXT             NULL,
    instructor_id  BIGINT UNSIGNED  NULL     COMMENT 'Giảng viên, là một user trong hệ thống',
    price          BIGINT UNSIGNED  NOT NULL DEFAULT 100000 COMMENT 'Học phí (VND)',
    status         ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    created_by     BIGINT UNSIGNED  NULL,
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     DATETIME         NULL     COMMENT 'Xóa mềm',
    PRIMARY KEY (id),
    KEY idx_courses_instructor (instructor_id),
    CONSTRAINT fk_courses_instructor FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_courses_creator    FOREIGN KEY (created_by)    REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Bài học (mỗi khoá học có nhiều bài học)
CREATE TABLE lessons (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    course_id      BIGINT UNSIGNED  NOT NULL,
    title          VARCHAR(200)     NOT NULL COMMENT 'Tên bài học',
    document_url   VARCHAR(2048)    NULL     COMMENT 'Link tài liệu',
    video_url      VARCHAR(2048)    NULL     COMMENT 'Link video ở hệ thống khác (YouTube, Drive...)',
    instructions   TEXT             NULL     COMMENT 'Hướng dẫn học',
    sort_order     INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Thứ tự bài trong khoá',
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     DATETIME         NULL     COMMENT 'Xóa mềm',
    PRIMARY KEY (id),
    KEY idx_lessons_course_order (course_id, sort_order),
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Đăng ký khoá học (User <-> Course, nhiều-nhiều)
CREATE TABLE course_enrollments (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED  NOT NULL,
    course_id      BIGINT UNSIGNED  NOT NULL,
    status         ENUM('PENDING','ENROLLED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    price          BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT 'Học phí lúc đăng ký (VND), dùng tính doanh thu',
    enrolled_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at    DATETIME         NULL     COMMENT 'Lúc admin duyệt vào học',
    completed_at   DATETIME         NULL,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollments_user_course (user_id, course_id),
    KEY idx_enrollments_course_status (course_id, status, enrolled_at),
    KEY idx_enrollments_status_enrolled (status, enrolled_at),
    KEY idx_enrollments_approved (approved_at),
    CONSTRAINT fk_enrollments_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Phần: File
-- =============================================

-- 9. File (dùng chung, không gắn cứng vào bảng nào)
CREATE TABLE files (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    url           VARCHAR(2048)    NOT NULL COMMENT 'Đường dẫn file trong thư mục upload của server (tương đối)',
    original_name VARCHAR(255)     NOT NULL COMMENT 'Tên file gốc lúc upload',
    content_type  VARCHAR(100)     NOT NULL COMMENT 'MIME type, vd application/pdf, video/mp4',
    size_bytes    BIGINT UNSIGNED  NOT NULL COMMENT 'Dung lượng file (byte)',
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Bài học <-> File (mỗi bài học có nhiều file)
CREATE TABLE lesson_files (
    lesson_id   BIGINT UNSIGNED  NOT NULL,
    file_id     BIGINT UNSIGNED  NOT NULL,
    file_type   ENUM('DOCUMENT','VIDEO') NOT NULL DEFAULT 'DOCUMENT' COMMENT 'Vai trò của file trong bài học: tài liệu hay video',
    sort_order  INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Thứ tự file trong bài học',
    PRIMARY KEY (lesson_id, file_id),
    KEY idx_lesson_files_file (file_id),
    CONSTRAINT fk_lesson_files_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_files_file   FOREIGN KEY (file_id)   REFERENCES files(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Phần: Chứng chỉ
-- =============================================

-- 11. Chứng chỉ hoàn thành khoá học (họ tên, ngày sinh, CCCD, tên khoá chụp lại lúc cấp)
CREATE TABLE certificates (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    code           VARCHAR(30)      NOT NULL COMMENT 'Mã tra cứu công khai, vd VRC-2026-7K3QX9PA',
    enrollment_id  BIGINT UNSIGNED  NOT NULL COMMENT 'Mỗi lượt ghi danh tối đa 1 chứng chỉ',
    course_name    VARCHAR(200)     NOT NULL COMMENT 'Snapshot tên khoá lúc cấp',
    full_name      VARCHAR(101)     NOT NULL COMMENT 'Snapshot họ + tên học viên lúc cấp',
    date_of_birth  DATE             NULL     COMMENT 'Snapshot ngày sinh lúc cấp',
    cccd           CHAR(12)         NULL     COMMENT 'Snapshot CCCD lúc cấp',
    file_id        BIGINT UNSIGNED  NOT NULL COMMENT 'File PDF chứng chỉ',
    issued_by      BIGINT UNSIGNED  NULL     COMMENT 'Admin cấp chứng chỉ',
    issued_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificates_code (code),
    UNIQUE KEY uk_certificates_enrollment (enrollment_id),
    KEY idx_certificates_file (file_id),
    CONSTRAINT fk_certificates_enrollment FOREIGN KEY (enrollment_id) REFERENCES course_enrollments(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_certificates_file       FOREIGN KEY (file_id)       REFERENCES files(id),
    CONSTRAINT fk_certificates_issued_by  FOREIGN KEY (issued_by)     REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Phần: Thanh toán
-- =============================================

-- 12. Lịch sử thanh toán khoá học (admin ghi nhận thủ công, không tự ghi danh)
CREATE TABLE payments (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED  NOT NULL COMMENT 'Người thanh toán',
    course_id        BIGINT UNSIGNED  NOT NULL COMMENT 'Khoá học đăng ký',
    course_name      VARCHAR(200)     NOT NULL COMMENT 'Snapshot tên khoá lúc ghi nhận',
    amount           DECIMAL(15,2)    NOT NULL COMMENT 'Số tiền (VND)',
    method           ENUM('CASH','BANK_TRANSFER','CARD','E_WALLET','OTHER') NOT NULL,
    status           ENUM('PENDING','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    transaction_ref  VARCHAR(100)     NULL     COMMENT 'Mã giao dịch ngân hàng / ví',
    note             VARCHAR(500)     NULL,
    paid_at          DATETIME         NULL     COMMENT 'Thời điểm trả tiền (khi PAID / REFUNDED)',
    created_by       BIGINT UNSIGNED  NULL     COMMENT 'Admin ghi nhận',
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_payments_user (user_id),
    KEY idx_payments_course (course_id),
    CONSTRAINT fk_payments_user       FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_payments_course     FOREIGN KEY (course_id)  REFERENCES courses(id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Đơn vị xử lý sự cố: "Tìm đơn vị xử lý sự cố" (người dùng đã đăng nhập).
CREATE TABLE support_points (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255)    NOT NULL COMMENT 'Tên đơn vị xử lý sự cố',
    province     VARCHAR(100)    NOT NULL COMMENT 'Tỉnh / thành phố',
    district     VARCHAR(100)    NOT NULL COMMENT 'Quận / huyện',
    address      VARCHAR(500)    NULL     COMMENT 'Địa chỉ chi tiết',
    latitude     DECIMAL(10,7)   NOT NULL,
    longitude    DECIMAL(10,7)   NOT NULL,
    phone_number VARCHAR(20)     NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME        NULL,
    PRIMARY KEY (id),
    KEY idx_support_points_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bài thi lấy chứng chỉ (mỗi khoá 1 bài) và câu hỏi trắc nghiệm A/B/C/D.
CREATE TABLE exams (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    course_id        BIGINT UNSIGNED  NOT NULL COMMENT 'Mỗi khoá học tối đa 1 bài thi',
    title            VARCHAR(200)     NOT NULL COMMENT 'Tên bài thi',
    duration_minutes INT UNSIGNED     NOT NULL DEFAULT 30 COMMENT 'Thời gian làm bài (phút)',
    pass_score       DECIMAL(4,2)     NOT NULL DEFAULT 5.00 COMMENT 'Điểm đạt, thang 10',
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exams_course (course_id),
    CONSTRAINT fk_exams_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exam_questions (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    exam_id        BIGINT UNSIGNED  NOT NULL,
    content        TEXT             NOT NULL COMMENT 'Nội dung câu hỏi',
    option_a       VARCHAR(1000)    NOT NULL,
    option_b       VARCHAR(1000)    NOT NULL,
    option_c       VARCHAR(1000)    NOT NULL,
    option_d       VARCHAR(1000)    NOT NULL,
    correct_option ENUM('A','B','C','D') NOT NULL COMMENT 'Đáp án đúng',
    sort_order     INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Thứ tự câu trong bài thi',
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exam_questions_exam_order (exam_id, sort_order),
    CONSTRAINT fk_exam_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lượt thi chứng chỉ của học viên (mỗi học viên thi mỗi bài 1 lần); điểm do server chấm khi nộp.
CREATE TABLE exam_attempts (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    exam_id        BIGINT UNSIGNED  NOT NULL,
    user_id        BIGINT UNSIGNED  NOT NULL,
    started_at     DATETIME         NOT NULL COMMENT 'Bấm bắt đầu làm bài',
    deadline_at    DATETIME         NOT NULL COMMENT 'Hạn nộp = started_at + thời gian làm bài lúc bắt đầu',
    pass_score     DECIMAL(4,2)     NOT NULL COMMENT 'Điểm đạt lúc bắt đầu thi, thang 10',
    submitted_at   DATETIME         NULL     COMMENT 'NULL = đang làm bài',
    question_count INT UNSIGNED     NULL     COMMENT 'Số câu lúc chấm',
    correct_count  INT UNSIGNED     NULL,
    score          DECIMAL(4,2)     NULL     COMMENT 'Điểm thang 10',
    passed         TINYINT(1)       NULL,
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_attempts_exam_user (exam_id, user_id),
    KEY idx_exam_attempts_user (user_id),
    CONSTRAINT fk_exam_attempts_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_attempts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Khách hàng doanh nghiệp (role BUSINESS, users.business_id) và tiến độ xem video lưu trên server.
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
