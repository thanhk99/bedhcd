package com.api.bedhcd.modules.election.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.ElectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResponse {
    private String id;
    private String meetingId;
    private String title;
    private String description;
    private ElectionType type;
    private Integer displayOrder;
    private List<CandidateResponse> candidates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateResponse {
        private String id;
        private String fullName;
        private String description;
    }
}
