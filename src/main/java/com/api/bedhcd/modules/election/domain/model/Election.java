package com.api.bedhcd.modules.election.domain.model;

import com.api.bedhcd.shared.domain.enums.ElectionType;
import com.api.bedhcd.modules.voting.domain.model.VotingOption;
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
    private Integer numSeats;
    private Integer displayOrder;
    @Builder.Default
    private List<VotingOption> candidates = new ArrayList<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Tính tổng quyền biểu quyết cho bầu cử này
     */
    public long calculateVotingPower(long baseVotingPower) {
        int seats = (numSeats != null && numSeats > 0) ? numSeats : (candidates != null ? candidates.size() : 0);
        return baseVotingPower * seats;
    }
}
