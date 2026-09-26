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
    status         ENUM('ENROLLED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ENROLLED',
    enrolled_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at   DATETIME         NULL,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollments_user_course (user_id, course_id),
    KEY idx_enrollments_course (course_id),
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
