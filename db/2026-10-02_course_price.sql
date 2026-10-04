-- =============================================
-- 2026-10-02: học phí khoá học và doanh thu.
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- Khoá đã có lấy giá mặc định 100.000 ₫; ghi danh đã có lấy giá đó, ghi danh đã được vào học coi như duyệt lúc
-- ghi danh (enrolled_at).
-- =============================================
USE vierec;

ALTER TABLE courses
    ADD COLUMN price BIGINT UNSIGNED NOT NULL DEFAULT 100000 COMMENT 'Học phí (VND)' AFTER instructor_id;

ALTER TABLE course_enrollments
    ADD COLUMN price       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Học phí lúc đăng ký (VND), dùng tính doanh thu' AFTER status,
    ADD COLUMN approved_at DATETIME        NULL     COMMENT 'Lúc admin duyệt vào học' AFTER enrolled_at,
    ADD KEY idx_enrollments_approved (approved_at);

UPDATE course_enrollments e JOIN courses c ON c.id = e.course_id SET e.price = c.price;
UPDATE course_enrollments SET approved_at = enrolled_at WHERE status IN ('ENROLLED', 'COMPLETED');
