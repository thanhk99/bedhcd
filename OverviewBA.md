# TÀI LIỆU TỔNG QUAN PHÂN TÍCH NGHIỆP VỤ (OVERVIEW BA) - HỆ THỐNG BEDHCD
---

## 1. THỐNG KÊ MODULE NGHIỆP VỤ

### 1.1. Module Tổ chức & Nội dung (Admin Side)
- **Quản lý Cuộc họp (Meeting)**: Quản lý thông tin thời gian, địa điểm và trạng thái cuộc họp (**PENDING**, **ONGOING**, **CLOSED**).
- **Quản lý Nghị quyết (Resolution)**: Định nghĩa các vấn đề biểu quyết. Mặc định 3 tùy chọn: *Đồng ý, Không đồng ý, Ý kiến khác*.
- **Quản lý Bầu cử (Election)**: Bầu nhân sự HĐQT/BKS. Áp dụng phương thức *Bầu dồn phiếu*.

### 1.2. Module Tư cách & Ủy quyền (Participation & Proxy)
- **Meeting Participant**: Quản lý "Snapshot" quyền lợi của user trong từng cuộc họp cụ thể.
- **Ủy quyền (Proxy)**: Chuyển giao quyền biểu quyết tạm thời mà không ảnh hưởng dữ liệu User gốc.

### 1.3. Module Biểu quyết (Voting)
- **Bỏ phiếu Nghị quyết**: Trọng số phiếu = 100% quyền biểu quyết.
- **Bầu dồn phiếu**: Phân bổ tổng phiếu tích lũy cho các ứng viên.
- **Lịch sử (Audit Log)**: Ghi lại mọi hành động: VOTE_CAST (bỏ phiếu lần đầu), VOTE_CHANGED (thay đổi phiếu).

---

## 2. LUỒNG HOẠT ĐỘNG & TÁC ĐỘNG DATABASE

Dưới đây là chi tiết sự tương tác giữa logic xử lý và các bảng dữ liệu:

### 2.1. Luồng Ủy quyền (Proxy Flow)
**Logic**: Khi A ủy quyền cho B:
1. Hệ thống kiểm tra số dư cổ phần của A qua `MeetingParticipant`.
2. Ghi nhận giao dịch vào `ProxyDelegation`.
3. Thực hiện cập nhật "số dư quyền biểu quyết" đồng thời cho cả A và B.

| Thực thể | Tác động | Chi tiết |
| :--- | :--- | :--- |
| `proxy_delegation` | **INSERT** | Tạo bản ghi ủy quyền mới (Trạng thái ACTIVE) |
| `meeting_participant` | **UPDATE** | Giảm `shares_owned` của Delegator, tăng `received_proxy_shares` của Proxy |

### 2.2. Luồng Biểu quyết (Voting Flow)
**Logic**: Khi User thực hiện nhấn nút bầu:
1. Hệ thống tính `Voting Power = SharesOwned + ReceivedProxyShares`.
2. Kiểm tra trạng thái Meeting (phải là `ONGOING`).
3. Ghi phiếu bầu chính thức và nhật ký hành động.

| Thực thể | Tác động | Chi tiết |
| :--- | :--- | :--- |
| `vote` | **INSERT/UPDATE** | Lưu lựa chọn hiện tại. Nếu bầu lại sẽ ghi đè bản ghi cũ |
| `vote_log` | **INSERT** | Lưu vết lịch sử: VOTE_CAST (lần đầu) hoặc VOTE_CHANGED |
| `vote_draft` | **DELETE** | Tự động xóa bản nháp ngay khi phiếu chính thức được gửi |
| `meeting_realtime_status` | **BROADCAST** | Đẩy kết quả qua **Kafka** để Dashboard cập nhật thực tế |

---

## 3. LOGIC XỬ LÝ CHI TIẾT

### 3.1. Tính toán Quyền biểu quyết
Hệ thống sử dụng cơ chế **Atomic Updates** trong một Transaction để đảm bảo tính nhất quán:
- Số cổ phần trong bảng `User` là cố định.
- Số cổ phần trong `MeetingParticipant` là biến động theo các lệnh ủy quyền trong phạm vi cuộc họp.
- Việc tách biệt này giúp API bỏ phiếu chạy cực nhanh vì không phải tính toán lại tổng ủy quyền mỗi khi có người vote.

### 3.2. Chế độ Bầu dồn phiếu (Cumulative Voting)
- **Tổng phiếu** = (Quyền biểu quyết) x (Số lượng ứng viên).
- User có thể dồn toàn bộ cho 1 người hoặc chia nhỏ.
- Hệ thống kiểm tra: `SUM(phiếu cho các ứng viên) <= Tổng phiếu cho phép`.

### 3.3. Tích hợp Real-time (Kafka)
- **Producer**: Sau khi bản ghi `Vote` được lưu thành công, hệ thống gửi một Snapshot kết quả mới nhất qua Kafka.
- **Consumer**: Lắng nghe và đẩy về màn hình của Admin/Dashboard thông qua WebSocket, đảm bảo tính minh bạch tại hội trường.

---

## 4. CÁC QUY TẮC NGHIỆP VỤ BẮT BUỘC (BUSINESS RULES)

1. **Rule về thời gian**: Chỉ được vote khi `Meeting.status == ONGOING`.
2. **Rule về duy nhất**: Một đại biểu chỉ có một "lá phiếu cuối cùng" (Final Vote) được tính vào kết quả.
3. **Rule về ủy quyền**: Nếu người ủy quyền đã tự mình đi vote trước khi lệnh ủy quyền được thực thi, lệnh ủy quyền đó có thể bị khóa hoặc mất hiệu lực tùy theo cấu hình.
4. **Rule về nhật ký**: Mọi thay đổi lựa chọn đều phải được lưu vào `vote_log` để phục vụ hậu kiểm và giải quyết tranh chấp (nếu có).
