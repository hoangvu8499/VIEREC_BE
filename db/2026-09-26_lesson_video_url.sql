-- =============================================
-- 2026-09-26: link video từ hệ thống khác cho bài học (video upload không còn bắt buộc)
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- =============================================
USE vierec;

ALTER TABLE lessons
    ADD COLUMN video_url VARCHAR(2048) NULL COMMENT 'Link video ở hệ thống khác (YouTube, Drive...)' AFTER document_url;
