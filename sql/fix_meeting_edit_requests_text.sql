-- ============================================================
-- Migration: meeting_edit_requests chuyển từ payload JSON
-- sang description + changes (text rõ ràng, không còn JSON)
-- Chạy TRƯỚC khi deploy code mới (ddl-auto=update chỉ thêm cột,
-- không đổi được ý nghĩa cột payload).
-- ============================================================

-- 1. Thêm 2 cột mới nếu chưa tồn tại
ALTER TABLE meeting_edit_requests ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE meeting_edit_requests ADD COLUMN IF NOT EXISTS changes TEXT;

-- 2. Dịch dữ liệu JSON cũ sang text cho các bản ghi đang tồn tại (best-effort).
--    Lưu ý: JSON cũ chỉ chứa giá trị MỚI (không có giá trị cũ)
--    nên cột old được đánh dấu là '∅'.
UPDATE meeting_edit_requests
SET changes = (
    SELECT string_agg(
        key || '|' || CASE key
            WHEN 'title'       THEN 'Tên cuộc họp'
            WHEN 'description' THEN 'Mô tả'
            WHEN 'location'    THEN 'Địa điểm'
            WHEN 'startTime'   THEN 'Giờ bắt đầu'
            WHEN 'endTime'     THEN 'Giờ kết thúc'
            WHEN 'configId'    THEN 'Cấu hình'
            WHEN 'status'      THEN 'Trạng thái'
            ELSE key
        END || '|∅|' || value,
        E'\n'
    )
    FROM jsonb_each_text(payload::jsonb)
)
WHERE payload IS NOT NULL AND payload <> '' AND action_type IN ('CREATE', 'UPDATE', 'UPDATE_STATUS');

-- 3. Tạo description đơn giản cho dữ liệu cũ (có thể chỉnh sửa thêm)
UPDATE meeting_edit_requests
SET description = 'Yêu cầu ' || action_type || ' cuộc họp (dữ liệu cũ, xem chi tiết cột changes)'
WHERE description IS NULL;

-- 4. Cột payload cũ được GIỮ LẠI để đối chiếu dữ liệu lịch sử.
--    Nếu đã xác nhận ổn định, có thể xoá:
-- ALTER TABLE meeting_edit_requests DROP COLUMN payload;
