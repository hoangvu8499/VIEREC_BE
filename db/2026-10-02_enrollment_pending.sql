-- =============================================
-- 2026-10-02: ghi danh chờ duyệt. Học viên báo đã chuyển khoản → PENDING, admin duyệt → ENROLLED.
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- Các ghi danh ENROLLED cũ giữ nguyên (đã được vào học trước khi có bước duyệt).
-- =============================================
USE vierec;

ALTER TABLE course_enrollments
    MODIFY COLUMN status ENUM('PENDING','ENROLLED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING';
