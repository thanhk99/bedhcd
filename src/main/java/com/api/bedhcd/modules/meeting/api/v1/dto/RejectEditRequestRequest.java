package com.api.bedhcd.modules.meeting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body khi từ chối một yêu cầu chỉnh sửa cuộc họp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectEditRequestRequest {

    /**
     * Lý do từ chối (bắt buộc điền)
     */
    private String note;
}
