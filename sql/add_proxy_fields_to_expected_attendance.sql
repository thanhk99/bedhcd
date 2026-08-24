-- Migration: Thêm 2 cột proxy_cccd và proxy_shares vào bảng expected_attendance phục vụ đối soát

ALTER TABLE expected_attendance
    ADD COLUMN IF NOT EXISTS proxy_cccd VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS proxy_shares BIGINT NULL DEFAULT 0;
