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
     * - Cổ phần tham dự (attendingShares) = cổ phần chính chủ, tách biệt với uỷ quyền.
     * - Loại tham dự: DIRECT nếu sở hữu cổ phần, PROXY nếu chỉ nhận uỷ quyền.
     *
     * @param requestedAttendingShares Số cổ phần muốn tham dự (null = toàn bộ có thể tham dự)
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

        // Cổ phần trực tiếp có thể tham dự = sở hữu - đã uỷ quyền đi (tách biệt hoàn toàn với nhận uỷ quyền)
        long maxAvailable = Math.max(0L, owned - delegated);

        // Lấy con số hợp lệ tối đa
        long attending = Math.min(maxAvailable, requestedAttendingShares);

        // Bắt buộc tổng số cổ phần mang đến đại hội (Tự có + Nhận uỷ quyền) phải lớn hơn 0
        if (attending == 0 && received == 0) {
            throw ParticipantException.invalidState("Không thể điểm danh vì cổ phần trực tiếp và cổ phần nhận uỷ quyền đều bằng 0.");
        }

        this.attendingShares = attending;
        // Người sở hữu cổ phần (dù chỉ 1) là DIRECT; người không sở hữu mà chỉ nhận uỷ quyền là PROXY
        this.participationType = owned > 0 ? ParticipationType.DIRECT : ParticipationType.PROXY;
        this.status = ParticipantStatus.CHECKED_IN;
        this.checkedInAt = LocalDateTime.now();
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
}
