-- Hàng chờ duyệt (status = PENDING, mới nhất trước) và học viên của một khoá lọc theo trạng thái.
-- idx_enrollments_course_status thay idx_enrollments_course (FK course_id vẫn có index đứng đầu là course_id).
ALTER TABLE course_enrollments
    ADD KEY idx_enrollments_status_enrolled (status, enrolled_at),
    ADD KEY idx_enrollments_course_status (course_id, status, enrolled_at);

ALTER TABLE course_enrollments DROP INDEX idx_enrollments_course;
