package com.api.bedhcd.modules.meeting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchApprovalRequest {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolutionOperation {
        private String action; // CREATE, UPDATE, DELETE
        private String id;
        private com.api.bedhcd.modules.voting.api.v1.dto.ResolutionRequest data;
        private com.api.bedhcd.modules.voting.api.v1.dto.ResolutionRequest oldData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElectionOperation {
        private String action; // CREATE, UPDATE, DELETE
        private String id;
        private ElectionData data;
        private ElectionData oldData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElectionData {
        private String title;
        private String description;
        private com.api.bedhcd.shared.domain.enums.ElectionType type;
        private Integer displayOrder;
        private List<CandidateOperation> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateOperation {
        private String action; // CREATE, UPDATE, DELETE
        private String id;
        private com.api.bedhcd.modules.election.api.v1.dto.CandidateRequest data;
        private com.api.bedhcd.modules.election.api.v1.dto.CandidateRequest oldData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentData {
        private String title;
        private String fileUrl;
        private String fileName;
        private Integer displayOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentOperation {
        private String action; // CREATE, UPDATE, DELETE
        private String id;
        private DocumentData data;
        private DocumentData oldData;
    }

    private List<ResolutionOperation> resolutions;
    private List<ElectionOperation> elections;
    private List<DocumentOperation> documents;
    private String note;
}
