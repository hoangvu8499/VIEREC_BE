-- =============================================
-- 2026-09-26: thông tin file upload (API tạo bài học)
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- =============================================
USE vierec;

ALTER TABLE files
    ADD COLUMN original_name VARCHAR(255)    NOT NULL COMMENT 'Tên file gốc lúc upload' AFTER url,
    ADD COLUMN content_type  VARCHAR(100)    NOT NULL COMMENT 'MIME type, vd application/pdf, video/mp4' AFTER original_name,
    ADD COLUMN size_bytes    BIGINT UNSIGNED NOT NULL COMMENT 'Dung lượng file (byte)' AFTER content_type;

ALTER TABLE lesson_files
    ADD COLUMN file_type ENUM('DOCUMENT','VIDEO') NOT NULL DEFAULT 'DOCUMENT'
        COMMENT 'Vai trò của file trong bài học: tài liệu hay video' AFTER file_id;
