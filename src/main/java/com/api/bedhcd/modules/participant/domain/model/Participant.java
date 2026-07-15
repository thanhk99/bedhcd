package com.api.bedhcd.modules.participant.domain.model;

import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import com.api.bedhcd.modules.participant.domain.exception.ParticipantException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    private Long id;
    private String meetingId;
    private String userId;
    private ParticipationType participationType;
    private ParticipantStatus status;
    private Long attendingShares;
    private Long sharesOwned;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private LocalDateTime checkedInAt;
    /** Phiếu con từ tách phiếu - không tính vào số lượng cổ đông tham dự */
    private boolean splitTicket;

    /**
     * Nghiệp vụ: Đồng bộ dữ liệu cổ phần từ nguồn ngoài vào participant
     */
    public void syncShares(long ownedShares, long receivedProxyShares, long delegatedShares) {
        this.sharesOwned = ownedShares;
        this.receivedProxyShares = receivedProxyShares;
        this.delegatedShares = delegatedShares;
    }

    /**
     * Nghiệp vụ: Thực hiện điểm danh cho cổ đông.
     * - Không cho phép điểm danh 2 lần.
     * - Cổ phần tham dự (attendingShares) = cổ phần chính chủ, tách biệt với uỷ
     * quyền.
     * - Loại tham dự: DIRECT nếu sở hữu cổ phần, PROXY nếu chỉ nhận uỷ quyền.
     * - Tổng quyền biểu quyết = Số cổ phần trực tiếp tham dự + Số cổ phần nhận uỷ
     * quyền
     * 
     * @param requestedAttendingShares Số cổ phần muốn tham dự (null = toàn bộ có
     *                                 thể tham dự)
     */
    public void checkIn(Long requestedAttendingShares) {
        // Không cho phép điểm danh 2 lần
        if (isCheckedIn()) {
            throw ParticipantException.invalidState("Cổ đông này đã được điểm danh trước đó.");
        }

        // Validate tham số đầu vào không được để trống
        if (requestedAttendingShares == null) {
            throw ParticipantException.invalidState("Số cổ phần tham dự không được để trống.");
        }

        if (requestedAttendingShares < 0) {
            throw ParticipantException.invalidState("Số cổ phần tham dự không được là số âm.");
        }

        long owned = this.sharesOwned != null ? this.sharesOwned : 0L;
        long delegated = this.delegatedShares != null ? this.delegatedShares : 0L;
        long received = this.receivedProxyShares != null ? this.receivedProxyShares : 0L;

        // Cổ phần trực tiếp có thể tham dự = sở hữu - đã uỷ quyền đi (tách biệt hoàn
        // toàn với nhận uỷ quyền)
        long maxAvailable = Math.max(0L, owned - delegated);

        // Lấy con số hợp lệ tối đa
        long attending = Math.min(maxAvailable, requestedAttendingShares);

        // Bắt buộc tổng số cổ phần mang đến đại hội (Tự có + Nhận uỷ quyền) phải lớn
        // hơn 0
        if (attending == 0 && received == 0) {
            throw ParticipantException
                    .invalidState("Không thể điểm danh vì cổ phần trực tiếp và cổ phần nhận uỷ quyền đều bằng 0.");
        }

        this.attendingShares = attending;
        // Người sở hữu cổ phần (dù chỉ 1) là DIRECT; người không sở hữu mà chỉ nhận uỷ
        // quyền là PROXY
        this.participationType = owned > 0 ? ParticipationType.DIRECT : ParticipationType.PROXY;
        this.status = ParticipantStatus.CHECKED_IN;
        this.checkedInAt = LocalDateTime.now();
    }

    /**
     * Nghiệp vụ: Cập nhật số cổ phần tham dự cho cổ đông đã điểm danh.
     */
    public void updateCheckIn(Long requestedAttendingShares) {
        if (!isCheckedIn()) {
            throw ParticipantException.invalidState("Cổ đông chưa được điểm danh.");
        }
        if (requestedAttendingShares == null) {
            throw ParticipantException.invalidState("Số cổ phần tham dự không được để trống.");
        }
        if (requestedAttendingShares < 0) {
            throw ParticipantException.invalidState("Số cổ phần tham dự không được là số âm.");
        }

        long owned = this.sharesOwned != null ? this.sharesOwned : 0L;
        long delegated = this.delegatedShares != null ? this.delegatedShares : 0L;
        long maxAvailable = Math.max(0L, owned - delegated);
        long attending = Math.min(maxAvailable, requestedAttendingShares);

        long received = this.receivedProxyShares != null ? this.receivedProxyShares : 0L;
        if (attending == 0 && received == 0) {
            throw ParticipantException
                    .invalidState("Không thể cập nhật vì cổ phần trực tiếp và cổ phần nhận uỷ quyền đều bằng 0.");
        }

        this.attendingShares = attending;
    }

    /**
     * Nghiệp vụ: Tính tổng quyền biểu quyết
     * Tổng biểu quyết = Số cổ phần trực tiếp tham dự + Số cổ phần nhận uỷ quyền
     */
    public long calculateTotalVotingPower() {
        if (!isCheckedIn()) {
            return 0L;
        }
        return (attendingShares != null ? attendingShares : 0L)
                + (receivedProxyShares != null ? receivedProxyShares : 0L);
    }

    /**
     * Kiểm tra trạng thái đã điểm danh chưa
     */
    public boolean isCheckedIn() {
        return status == ParticipantStatus.CHECKED_IN || status == ParticipantStatus.PRINT;
    }

    /**
     * Nghiệp vụ: Đánh dấu đã in thẻ
     */
    public void markAsPrinted() {
        if (!isCheckedIn()) {
            throw ParticipantException.invalidState("Cổ đông chưa điểm danh, không thể in thẻ.");
        }
        this.status = ParticipantStatus.PRINT;
        this.checkedInAt = LocalDateTime.now();
    }

    // --- Nghiệp vụ Uỷ quyền (Delegation) ---

    /**
     * Nghiệp vụ: Kiểm tra điều kiện uỷ quyền.
     * - Chỉ được uỷ quyền khi trạng thái là PENDING (chưa điểm danh).
     * - Số cổ phần có thể uỷ quyền = sharesOwned - attendingShares (cổ phần đang
     * giữ lại tham dự).
     * Khi PENDING, attendingShares = 0 nên toàn bộ sharesOwned đều khả dụng.
     */
    public void validateCanDelegate(long sharesToDelegate) {
        if (isCheckedIn()) {
            throw ParticipantException.invalidState(
                    "Không thể uỷ quyền khi cổ đông đã điểm danh. Chỉ được uỷ quyền trước khi điểm danh (trạng thái PENDING).");
        }

        long owned = this.sharesOwned != null ? this.sharesOwned : 0L;
        long attending = this.attendingShares != null ? this.attendingShares : 0L;
        long available = Math.max(0L, owned - attending);

        if (sharesToDelegate <= 0) {
            throw ParticipantException.invalidState("Số cổ phần uỷ quyền phải lớn hơn 0.");
        }

        if (sharesToDelegate > available) {
            throw ParticipantException.invalidState(
                    "Số cổ phần uỷ quyền (" + sharesToDelegate + ") vượt quá số cổ phần khả dụng (" + available + ")." +
                            " (Công thức: sở hữu " + owned + " - đang tham dự " + attending + " = " + available + ")");
        }
    }

    public void addDelegatedShares(long shares) {
        long delegated = this.delegatedShares != null ? this.delegatedShares : 0L;
        this.delegatedShares = delegated + shares;
    }

    public void addReceivedProxyShares(long shares) {
        long received = this.receivedProxyShares != null ? this.receivedProxyShares : 0L;
        this.receivedProxyShares = received + shares;
    }

    public void adjustDelegatedShares(long delta) {
        long delegated = this.delegatedShares != null ? this.delegatedShares : 0L;
        this.delegatedShares = delegated + delta;
    }

    public void adjustReceivedProxyShares(long delta) {
        long received = this.receivedProxyShares != null ? this.receivedProxyShares : 0L;
        this.receivedProxyShares = received + delta;
    }
}
