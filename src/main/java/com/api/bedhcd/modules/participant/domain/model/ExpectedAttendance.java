package com.api.bedhcd.modules.participant.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bản ghi danh sách tham dự dự kiến do KSNB import cho một cuộc họp.
 * Đây là nguồn đối chiếu (expected) với dữ liệu trong meeting_participants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedAttendance {
    private Long id;
    private String meetingId;
    private String cccd;
    private Long expectedShares;
    private LocalDateTime createdAt;
}
