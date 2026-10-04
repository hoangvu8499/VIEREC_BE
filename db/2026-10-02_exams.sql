-- Bài thi lấy chứng chỉ: mỗi khoá học 1 bài thi, mỗi bài thi nhiều câu trắc nghiệm A/B/C/D (1 đáp án đúng).
-- Điểm thang 10 = số câu đúng / tổng số câu x 10.

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
