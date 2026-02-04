# Tài Liệu Thống Kê Thuyết Trình - Hệ Thống BEDHCD

Tài liệu này tổng hợp các ý chính để bạn sử dụng cho buổi thuyết trình ngày mai, tập trung vào mô hình nghiệp vụ, kiến trúc kỹ thuật và các điểm nổi bật của hệ thống.

---

## 1. Tổng Quan Hệ Thống

*   **Mục tiêu**: Quản lý và thực hiện quy trình Đại hội cổ đông (biểu quyết nghị quyết, bầu cử thành viên HĐQT/BKS) minh bạch, chính xác và thời gian thực.
*   **Đối tượng tham gia**:
    *   **Cổ đông**: Người sở hữu cổ phần và thực hiện quyền biểu quyết.
    *   **Đại biểu (Proxy)**: Người nhận ủy quyền thay mặt cổ đông.
    *   **Admin**: Quản lý cuộc họp và điều phối quá trình bỏ phiếu.

---

## 2. Nghiệp Vụ Cốt Lõi (Core Business Logic)

### 2.1. Hai loại hình bỏ phiếu chính:
1.  **Biểu quyết Nghị quyết (Resolution)**:
    *   Mặc định 3 lựa chọn: *Đồng ý, Không đồng ý, Không ý kiến*.
    *   Trọng số: 100% quyền biểu quyết của cổ đông.
2.  **Bầu cử tích lũy (Cumulative Voting - Bầu HĐQT/BKS)**:
    *   **Tổng số phiếu** = (Số cổ phần sở hữu + nhận ủy quyền) x (Số lượng ứng viên cần bầu).
    *   Ví dụ: Có 1,000 cổ phần, bầu 5 người -> Có 5,000 phiếu.
    *   Đặc điểm: Cổ đông có quyền dồn toàn bộ phiếu cho 1 người hoặc chia nhỏ tùy ý.

### 2.2. Cơ chế Ủy quyền (Proxy Delegation)
*   Cho phép cổ đông ủy quyền cho **duy nhất 1 người**.
*   Số cổ phần được cộng dồn vào quyền biểu quyết của người nhận ủy quyền.
*   **Quy tắc ưu tiên**: Nếu cổ đông đã tự vote thì lệnh ủy quyền sẽ mất hiệu lực để đảm bảo tính minh bạch.

---

## 3. Kiến Trúc Kỹ Thuật (Technical Architecture)

### 3.1. Stack Công nghệ
*   **Backend**: Spring Boot 3.
*   **Database**: PostgreSQL (Atomic Transactions để đảm bảo quyền biểu quyết chính xác).
*   **Real-time**: Apache Kafka + WebSocket (STOMP).
*   **Security**: JWT + Http-only Cookie.

### 3.2. Luồng xử lý Real-time (Event-Driven)
*   Khi có 1 phiếu được gửi (`Vote`):
    1.  Lưu vào DB kèm log chi tiết (IP, User Agent).
    2.  Tính toán lại kết quả tạm thời.
    3.  Gửi event vào **Kafka Topic**.
    4.  **Kafka Consumer** nhận event và đẩy qua **WebSocket** đến Dashboard của Admin/Hội trường ngay lập tức.

---

## 4. Các Điểm Nổi Bật & Tối Ưu

*   **Tính toàn vẹn (Normalization)**: Dữ liệu được chuẩn hóa cao. Kết quả cuối cùng luôn dựa trên từng lá phiếu thật (Audit log đầy đủ), giúp tránh các vấn đề pháp lý sau đại hội.
*   **Hiệu năng cao**: Sử dụng Cache in-memory (`ConcurrentHashMap`) để lưu trạng thái realtime, giúp phản hồi kết quả tức thì mà không cần quét lại Database liên tục.
*   **Bảo mật & Hậu kiểm**: Mọi thay đổi phiếu (VOTE_CHANGED) đều được ghi nhật ký, cho phép truy soát 100% lịch sử bỏ phiếu.

---

## 5. Gợi Ý Các Slide Quan Trọng

1.  **Slide 1**: Sơ đồ tiến trình đại hội (Chuẩn bị -> Check-in -> Bỏ phiếu -> Kết quả).
2.  **Slide 2**: Giải thích cơ chế Bầu dồn phiếu (Cumulative) - Đây là điểm phức tạp nhất về nghiệp vụ.
3.  **Slide 3**: Sơ đồ kiến trúc hệ thống:
    ```mermaid
    graph LR
        Client[User Client\nFrontend/Mobile] -->|POST /vote| API[Spring Boot API]
        API -->|Transaction| DB[(PostgreSQL)]
        API -->|Produce Event| Kafka{Kafka Broker}
        Kafka -->|Consume| Worker[Vote Consumer]
        Worker -->|Push Status| Dashboard[Admin Dashboard\nWebSocket]
        Dashboard -->|Real-time UI| Admin((Admin / Hội trường))
    ```
4.  **Slide 4**: Đánh giá sự cân bằng giữa **Độ chính xác pháp lý** và **Hiệu năng realtime**.

---

> [!TIP]
> **Điểm nhấn cho buổi thuyết trình**: Hãy tập trung nhấn mạnh vào việc hệ thống sử dụng **Kafka** để xử lý bất đồng bộ. Điều này giúp hệ thống không bị "treo" khi hàng nghìn người cùng vote một lúc, đồng thời Dashboard tại hội trường vẫn hiển thị kết quả nhảy liên tục (Live result).
