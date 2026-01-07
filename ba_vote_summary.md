# Business Analysis (BA) - Hệ Thống Bỏ Phiếu Đại Hội Cổ Đông

## 1. Tổng Quan Dự Án


## 2. Các Đối Tượng Liên Quan (Stakeholders)
- **Admin**: Quản lý cuộc họp, phiên bỏ phiếu, ứng viên và công bố kết quả.
- **Cổ đông (User)**: Thực hiện bỏ phiếu, lưu bản nháp, hoặc ủy quyền biểu quyết.
- **Người được ủy quyền (Proxy)**: Thay mặt cổ đông thực hiện quyền biểu quyết với tổng số cổ phần được giao.

## 3. Các Loại Hình Bỏ Phiếu
| Loại | Cơ chế | Áp dụng |
| :--- | :--- | :--- |
| **Biểu quyết (Resolution)** | Chọn 1 trong 3 (Tán thành/Không/Trắng) | Thông qua nghị quyết, điều lệ |
| **Bầu cử tích lũy (Cumulative)** | Tỷ lệ: `Cổ phần x Số ghế`. Tự do phân bổ phiếu. | Bầu HĐQT (BOD) & Ban kiểm soát (SB) |

## 4. Quy trình Nghiệp vụ Chính (Core Flows)
### 4.1. Quy trình Bỏ phiếu
1. **Kiểm tra thời gian**: Bỏ phiếu trong thời gian quy định của cuộc họp (không cần session mở).
2. **Tính toán quyền năng**: Tổng phiếu = (Phiếu sở hữu + Phiếu nhận ủy quyền) [x Số ghế nếu là bầu cử].
3. **Thực hiện**: User chọn/nhập phiếu -> Hệ thống Validate -> Ghi log -> Xóa bản nháp -> Xác nhận.
4. **Thay đổi**: Cho phép thay đổi phiếu nhiều lần trong thời hạn bỏ phiếu.

### 4.2. Lưu bản nháp (Draft)
- Cho phép lưu tạm lựa chọn khi chưa muốn submit chính thức.
- Tự động xóa khi đã bỏ phiếu thật hoặc khi cuộc họp kết thúc.

### 4.3. Ủy quyền (Proxy)
- Một cổ đông chỉ được ủy quyền cho **duy nhất 1 người** trong 1 cuộc họp.
- Người nhận ủy quyền kế thừa số cổ phần để tăng quyền biểu quyết.

## 5. Quy tắc Nghiệp vụ (Business Rules)
- **Tính Bí Mật**: Kết quả cá nhân chỉ User và Admin (quyền audit) biết.
- **Tính Toàn Vẹn**: Chỉ tính kết quả từ các phiếu đã SUBMIT. Phiếu nháp không có giá trị pháp lý.
- **Cơ chế Cumulative**: Cho phép dồn tất cả phiếu cho 1 ứng viên duy nhất.
- **Thứ tự Ưu tiên**: Nếu cả người ủy quyền và người được ủy quyền cùng có mặt, quyền biểu quyết mặc định theo logic check-in/thỏa thuận (Rule: Người ủy quyền tự vote thì ủy quyền mất hiệu lực).

## 6. Yêu cầu Hệ thống (Non-Functional)
- **Logging**: Ghi lại mọi hành động (VOTE_CAST, CHANGED, LOGIN) kèm IP/User Agent.
- **Security**: Bảo mật mật khẩu qua BCrypt, session qua JWT và Http-only Cookie.
- **Hiệu năng**: Xử lý tính toán kết quả theo thời gian thực (Real-time aggregation).
