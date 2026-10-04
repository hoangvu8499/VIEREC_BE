-- =============================================
-- 2026-09-26: lịch sử thanh toán khoá học (admin ghi nhận thủ công)
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- =============================================
USE vierec;

CREATE TABLE payments (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED  NOT NULL COMMENT 'Người thanh toán',
    course_id        BIGINT UNSIGNED  NOT NULL COMMENT 'Khoá học đăng ký',
    course_name      VARCHAR(200)     NOT NULL COMMENT 'Snapshot tên khoá lúc ghi nhận',
    amount           DECIMAL(15,2)    NOT NULL COMMENT 'Số tiền (VND)',
    method           ENUM('CASH','BANK_TRANSFER','CARD','E_WALLET','OTHER') NOT NULL,
    status           ENUM('PENDING','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    transaction_ref  VARCHAR(100)     NULL     COMMENT 'Mã giao dịch ngân hàng / ví',
    note             VARCHAR(500)     NULL,
    paid_at          DATETIME         NULL     COMMENT 'Thời điểm trả tiền (khi PAID / REFUNDED)',
    created_by       BIGINT UNSIGNED  NULL     COMMENT 'Admin ghi nhận',
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_payments_user (user_id),
    KEY idx_payments_course (course_id),
    CONSTRAINT fk_payments_user       FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_payments_course     FOREIGN KEY (course_id)  REFERENCES courses(id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
