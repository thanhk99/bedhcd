# Nghiệp vụ Đăng ký tham dự (Check-in) - BEDHCD

Tài liệu này lưu trữ logic nghiệp vụ cốt lõi cho quy trình đăng ký tham dự (check-in) cổ đông trong hệ thống Đại hội cổ đông.

## 1. Khái niệm cơ bản
- **MeetingParticipant**: Bản ghi đại diện cho sự hiện diện của một Cổ đông/Đại biểu trong một cuộc họp cụ thể.
- **Loại hình kỹ thuật (Participation Type)**:
  - `DIRECT`: Cổ đông tự đi dự.
  - `PROXY`: Đại biểu đi dự thay (nhận uỷ quyền).

- **5 Case nghiệp vụ (Business Cases)**:
  1. **Cổ đông**: Sở hữu > 0, không nhận uỷ quyền, không uỷ quyền đi.
  2. **Cổ đông nhận uỷ quyền**: Sở hữu > 0, có nhận uỷ quyền.
  3. **Cổ đông nhận uỷ quyền và uỷ quyền cho người khác**: Sở hữu > 0, có nhận uỷ quyền, có uỷ quyền đi.
  4. **Cổ đông uỷ quyền cho người khác**: Sở hữu > 0, có uỷ quyền đi.
  5. **Không phải cổ đông nhận uỷ quyền**: Sở hữu = 0, chỉ có nhận uỷ quyền.

- **Quyền biểu quyết (Voting Power)**: Tổng số cổ phần tối đa mà người tham dự có quyền sử dụng để biểu quyết.
  - công thức: `Voting Power = Sở hữu (Owned) - Uỷ quyền đi (Delegated) + Nhận uỷ quyền (Received Proxy)`.
- **Số cổ phần tham dự (Attending Shares)**: Số cổ phần thực tế mà người tham dự đăng ký khi check-in.

## 2. Quy trình Xác nhận Tham dự (Check-in)
Hệ thống áp dụng logic đơn giản và trực quan để ghi nhận số cổ phần tham dự:

### Quy tắc định danh:
- Chỉ sử dụng **CCCD** để tìm kiếm và xác định cổ đông khi check-in. Không sử dụng InvestorCode.

### Quy tắc tính toán Số cổ phần tham dự (Attending Shares):
1. **Trường hợp mặc định (Không nhập số)**: 
   - Hệ thống tự động tính bằng phần **Sở hữu ròng**: `Sở hữu - Uỷ quyền đi`.
   - Mục đích: Ưu tiên ghi nhận quyền biểu quyết chính chủ của cổ đông trước.
2. **Trường hợp có nhập số cụ thể**: 
   - Hệ thống tôn trọng và lấy **đúng con số được nhập vào**.
   - Tuy nhiên, con số này luôn bị giới hạn (cap) bởi **Tổng khả dụng** (`Voting Power`) để tránh lỗi biểu quyết vượt quá quyền hạn (over-voting).

### Quy tắc phân loại (Participation Type):
- **DIRECT**: Áp dụng cho tất cả những người có **Sở hữu gốc > 0**.
- **PROXY**: Chỉ áp dụng cho những người có **Sở hữu gốc = 0** nhưng được người khác uỷ quyền đi họp thay.

## 3. Chức năng Check-in Bundle (`getCheckInBundle`)
- Hỗ trợ tìm kiếm nhanh theo **CCCD**.
- Trả về thông tin tổng hợp:
  - Thông tin cổ đông chính.
  - Danh sách các uỷ quyền đi (Ai đang thay mặt cổ đông này?).
  - Danh sách các uỷ quyền đến (Cổ đông này đang thay mặt cho ai?).
- Giúp nhân viên check-in có cái nhìn toàn diện về quyền lợi của một người khi họ đến bàn thủ tục.

## 4. Lưu ý quan trọng
- **Tính toàn vẹn**: Luôn ưu tiên lấy số cổ phần từ bảng `ProxyDelegation` khi tính toán để tránh sai lệch dữ liệu.
- **Cơ chế giới hạn**: `finalAttendingShares = Math.min(Voting Power, InputValue)`.
- **Atomic Transaction**: Toàn bộ quá trình đăng ký phải nằm trong một Transaction.
