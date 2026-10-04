-- Lượt thi chứng chỉ của học viên: mỗi học viên thi mỗi bài đúng 1 lần.
-- Điểm do server chấm khi nộp; hạn nộp tính theo giờ server (deadline_at = started_at + thời gian làm bài).

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
