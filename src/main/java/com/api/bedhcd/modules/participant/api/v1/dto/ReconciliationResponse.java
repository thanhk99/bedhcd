package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả đối soát của một cuộc họp, bao gồm danh sách chi tiết và các chỉ số tổng.
 * So sánh giữa danh sách tham dự dự kiến (KSNB) và dữ liệu trong meeting_participants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponse {
    private List<ReconciliationItemResponse> items;

    /** Tổng số lượng cổ phần tham dự dự kiến (theo file KSNB) */
    private Long totalExpectedShares;
    /** Tổng số cổ đông tham dự dự kiến (theo file KSNB) */
    private Long totalExpectedShareholders;
    /** Tổng số lượng cổ phần trong hệ thống (meeting_participants) */
    private Long totalSystemShares;
    /** Tổng số cổ đông trong hệ thống (meeting_participants) */
    private Long totalSystemShareholders;
    /** Tổng số bản ghi lệch */
    private Long totalMismatched;
}
