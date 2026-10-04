-- Khoá tài khoản khi nhập sai mật khẩu, bắt đổi mật khẩu lần đầu, quên mật khẩu bằng mã gửi qua email.
ALTER TABLE users
    ADD COLUMN failed_login_count   INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Số lần nhập sai mật khẩu liên tiếp; đủ 5 thì khoá (SUPER_ADMIN: chặn 15 phút)' AFTER status,
    ADD COLUMN last_failed_login_at DATETIME     NULL     COMMENT 'Lần nhập sai gần nhất' AFTER failed_login_count,
    ADD COLUMN must_change_password TINYINT(1)   NOT NULL DEFAULT 0
        COMMENT '1: tài khoản tạo với mật khẩu mặc định, phải đổi khi đăng nhập' AFTER last_failed_login_at;

-- Mã 6 số đặt lại mật khẩu: chỉ lưu SHA-256 của mã, hết hạn sau 10 phút, sai 5 lần thì bỏ.
CREATE TABLE password_reset_codes (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED  NOT NULL,
    code_hash   CHAR(64)         NOT NULL COMMENT 'SHA-256 (hex) của mã gửi qua email',
    expires_at  DATETIME         NOT NULL,
    attempts    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Số lần nhập sai mã',
    used_at     DATETIME         NULL     COMMENT 'Đã dùng hoặc bị thay bằng mã mới',
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_password_reset_codes_user (user_id, created_at),
    CONSTRAINT fk_password_reset_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
