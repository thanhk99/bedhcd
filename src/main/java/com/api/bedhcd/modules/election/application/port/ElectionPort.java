package com.api.bedhcd.modules.election.application.port;

import java.util.List;

/**
 * Port interface để các module khác (VD: meeting) lấy dữ liệu election.
 * Tuân theo DDD rule: module ngoài chỉ inject interface này, không inject service/repo trực tiếp.
 */
public interface ElectionPort {

    /**
     * Lấy danh sách thông tin election kèm candidates theo meetingId.
     */
    List<ElectionSummary> getElectionsByMeetingId(String meetingId);

    /**
     * DTO nội bộ Port — không dùng chung với ElectionResponse của API layer
     */
    record ElectionSummary(
        String electionId,
        String title,
        String electionType,
        List<CandidateSummary> candidates
    ) {}

    record CandidateSummary(
        String candidateId,
        String name,
        String description,
        Integer displayOrder
    ) {}
}
