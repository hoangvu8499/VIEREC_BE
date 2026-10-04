-- =============================================
-- 2026-09-26: chứng chỉ hoàn thành khoá học
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- =============================================
USE vierec;

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
