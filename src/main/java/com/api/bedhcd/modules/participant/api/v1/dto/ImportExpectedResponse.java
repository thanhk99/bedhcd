package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả sau khi xác nhận import danh sách tham dự dự kiến (KSNB).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportExpectedResponse {
    private int totalRows;
    private long matchedCount;
    private long mismatchedCount;
    private String message;
}
