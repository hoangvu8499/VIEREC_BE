-- =============================================
-- 2026-10-02: đơn vị xử lý sự cố (tính năng "Tìm đơn vị xử lý sự cố", chuyển từ project IncidentManagement).
-- Chạy một lần trên DB đã có. DB.sql đã được cập nhật tương ứng cho DB mới.
-- 7 đơn vị mẫu lấy từ bảng ProcessingUnitLocation của IncidentManagement.
-- =============================================
USE vierec;

CREATE TABLE support_points (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255)    NOT NULL COMMENT 'Tên đơn vị xử lý sự cố',
    province     VARCHAR(100)    NOT NULL COMMENT 'Tỉnh / thành phố',
    district     VARCHAR(100)    NOT NULL COMMENT 'Quận / huyện',
    address      VARCHAR(500)    NULL     COMMENT 'Địa chỉ chi tiết',
    latitude     DECIMAL(10,7)   NOT NULL,
    longitude    DECIMAL(10,7)   NOT NULL,
    phone_number VARCHAR(20)     NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME        NULL,
    PRIMARY KEY (id),
    KEY idx_support_points_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO support_points (name, province, district, latitude, longitude, phone_number) VALUES
('Đơn vị xử lý sự cố số 1', 'Đà Nẵng', 'Ngũ Hành Sơn', 15.9942240, 108.2377390, '0905123456'),
('Đơn vị xử lý sự cố số 2', 'Đà Nẵng', 'Ngũ Hành Sơn', 15.9812630, 108.2461430, '0912345678'),
('Đơn vị xử lý sự cố số 3', 'Đà Nẵng', 'Cẩm Lệ', 15.9724430, 108.2246520, '0987654321'),
('Đơn vị xử lý sự cố số 4', 'Đà Nẵng', 'Cẩm Lệ', 15.9992290, 108.2137610, '0933221100'),
('Đơn vị xử lý sự cố số 5', 'Đà Nẵng', 'Sơn Trà', 16.0010250, 108.2574790, '0977889900'),
('Đơn vị xử lý sự cố số 6', 'Đà Nẵng', 'Sơn Trà', 15.9880000, 108.2807400, '0966123123'),
('Đơn vị xử lý sự cố số 7', 'Quảng Nam', 'Điện Bàn', 15.9204730, 108.2084160, '0944556677');
