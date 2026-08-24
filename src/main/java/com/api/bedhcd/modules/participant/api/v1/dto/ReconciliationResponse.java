package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả đối soát của một cuộc họp, bao gồm danh sách chi tiết và các chỉ số tổng.
 * So sánh giữa danh sách tham dự dự kiến (KSNB) và dữ liệu thực tế điểm danh/in thẻ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponse {
    private List<ReconciliationItemResponse> items;

    /** TẠM THỜI (Import) - Số lượng CĐ tham dự dự kiến */
    private Long totalExpectedShareholders;
    /** TẠM THỜI (Import) - Số lượng cổ phần tham dự dự kiến */
    private Long totalExpectedShares;
    /** TẠM THỜI (Import) - Tỷ lệ % (CĐ + UQ dự kiến / Tổng CP VSD) */
    private Double expectedRatio;

    /** THỰC TẾ (đã in phiếu) - Số lượng CĐ thực tế tham dự (chỉ tính cổ đông chính có status PRINT) */
    private Long totalActualShareholders;
    /** THỰC TẾ (đã in phiếu) - Số lượng cổ phần thực tế tham dự (Status = PRINT) */
    private Long totalActualShares;
    /** THỰC TẾ (đã in phiếu) - Tỷ lệ % (CĐ + UQ thực tế PRINT / Tổng CP VSD) */
    private Double actualRatio;

    /** Tổng số lượng cổ phần VSD của cuộc họp */
    private Long totalVsdShares;

    /** Tổng số bản ghi KHỚP */
    private Long totalMatched;
    /** Tổng số bản ghi LỆCH */
    private Long totalMismatched;
    /** Tổng số CP của các bản ghi KHỚP */
    private Long totalMatchedShares;
    /** Tổng số CP của các bản ghi LỆCH */
    private Long totalUnmatchedShares;
}
