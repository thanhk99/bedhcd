package com.api.bedhcd.modules.election.domain.model;

import com.api.bedhcd.modules.election.domain.exception.ElectionException;
import com.api.bedhcd.shared.domain.enums.ElectionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Election {
    private String id;
    private String meetingId;
    private String title;
    private String description;
    private ElectionType electionType;
    private Integer displayOrder;
    @Builder.Default
    private List<Candidate> candidates = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Tính tổng quyền biểu quyết cho bầu cử này
     */
    public long calculateVotingPower(long baseVotingPower) {
        int seats = (candidates != null && !candidates.isEmpty()) ? candidates.size() : 1;
        return baseVotingPower * seats;
    }

    /**
     * Kiểm tra xem có được phép xoá đợt bầu cử hay không
     */
    public void validateCanBeDeleted(long totalVoters) {
        if (this.candidates != null && !this.candidates.isEmpty()) {
            throw ElectionException.invalidState("Không thể xóa đợt bầu cử đã có ứng viên (người) ở trong.");
        }
        if (totalVoters > 0) {
            throw ElectionException.invalidState("Không thể xóa đợt bầu cử đã có người tham gia bỏ phiếu.");
        }
    }
}
