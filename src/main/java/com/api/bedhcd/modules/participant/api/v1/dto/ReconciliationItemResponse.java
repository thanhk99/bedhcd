package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bản ghi đối soát giữa số lượng cổ phần tham dự dự kiến (KSNB import)
 * và số lượng cổ phần tham dự trong hệ thống (meeting_participants).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItemResponse {
    private String cccd;
    private String fullName;
    /** Số cổ phần tham dự dự kiến (từ file KSNB) */
    private Long expectedShares;
    /** Số cổ phần trong hệ thống (từ meeting_participants) */
    private Long systemShares;
    /** systemShares - expectedShares */
    private Long difference;
    /** KHOP: chênh lệch bằng 0, LECH: chênh lệch khác 0 hoặc thiếu 1 phía */
    private String status;
    /** Lý do lệch: NOT_FOUND_IN_SYSTEM | SHARE_MISMATCH | NOT_IN_FILE (null nếu khớp) */
    private String reason;
}
