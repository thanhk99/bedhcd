package com.api.bedhcd.shared.dto.importing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bản ghi từ file danh sách tham dự dự kiến do KSNB import.
 * Format file: CCCD | Số cổ phần tham dự
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedAttendanceImportRecord {
    private String cccd;
    private Long expectedShares;
    private String proxyCccd;
    private Long proxyShares;
}
