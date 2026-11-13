-- Đề thi bốc ngẫu nhiên từ kho câu hỏi của bài thi.
-- exams.questions_per_attempt: số câu mỗi đề do admin cài; NULL = dùng toàn bộ kho (bài thi tạo trước khi nâng cấp).
-- exam_attempt_questions: các câu đã bốc cho từng lượt thi, theo thứ tự hiện cho học viên; chấm điểm theo đúng các
-- câu này.

ALTER TABLE exams
    ADD COLUMN questions_per_attempt INT UNSIGNED NULL
        COMMENT 'Số câu mỗi đề, bốc ngẫu nhiên từ kho; NULL = toàn bộ kho' AFTER pass_score;

CREATE TABLE exam_attempt_questions (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    attempt_id  BIGINT UNSIGNED  NOT NULL,
    question_id BIGINT UNSIGNED  NOT NULL,
    sort_order  INT UNSIGNED     NOT NULL COMMENT 'Thứ tự câu trong đề của học viên',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_attempt_questions (attempt_id, question_id),
    KEY idx_exam_attempt_questions_question (question_id),
    CONSTRAINT fk_exam_attempt_questions_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_exam_attempt_questions_question FOREIGN KEY (question_id) REFERENCES exam_questions(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lượt đang làm dở lúc nâng cấp: đề của họ là toàn bộ kho, theo thứ tự cũ.
INSERT INTO exam_attempt_questions (attempt_id, question_id, sort_order)
SELECT a.id, q.id, ROW_NUMBER() OVER (PARTITION BY a.id ORDER BY q.sort_order, q.id)
FROM exam_attempts a
JOIN exam_questions q ON q.exam_id = a.exam_id
WHERE a.submitted_at IS NULL;
