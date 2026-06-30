package com.api.bedhcd.modules.meeting.domain.model;

import com.api.bedhcd.modules.meeting.domain.exception.MeetingException;
import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {
    private String id;
    private String meetingCode;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String status;
    private String configId; // Tham chiếu tới cấu hình linh hoạt

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Nghiệp vụ: Kiểm tra quyền thực hiện một hành động dựa trên config
     */
    public void validatePermission(MeetingConfig config, String action, String role) {
        MeetingRules rules = config.getRulesForStatus(this.status);
        if (rules == null || !rules.hasPermission(action, role)) {
            throw MeetingException.accessDenied(action);
        }
    }

    /**
     * Nghiệp vụ: Kiểm tra xem TÍNH NĂNG ĐIỂM DANH có được mở không
     */
    public boolean canAttend(MeetingConfig config) {
        MeetingRules rules = config.getRulesForStatus(this.status);
        return rules != null && rules.isAllowAttendance();
    }

    public boolean canEditMeeting(MeetingConfig config) {
        MeetingRules rules = config.getRulesForStatus(this.status);
        return rules != null && rules.isAllowEditMeeting();
    }

    public boolean canImportShareholder(MeetingConfig config) {
        MeetingRules rules = config.getRulesForStatus(this.status);
        return rules != null && rules.isAllowImportShareholder();
    }

    public boolean canRegisterProxy(MeetingConfig config) {
        MeetingRules rules = config.getRulesForStatus(this.status);
        return rules != null && rules.isAllowProxyRegistration();
    }

    public boolean canVote(MeetingConfig config) {
        MeetingRules rules = config.getRulesForStatus(this.status);
        return rules != null && rules.isAllowVoting();
    }

    public void transitTo(MeetingConfig config, String newStatus) {
        if (config == null) {
            throw MeetingException.invalidState("Cuộc họp chưa có cấu hình trạng thái.");
        }
        MeetingRules rules = config.getRulesForStatus(this.status);
        if (rules == null || !newStatus.equals(rules.getNextState())) {
            throw MeetingException.invalidState(
                    "Không thể chuyển từ '" + this.status + "' sang '" + newStatus + "'.");
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canDelete() {
        return MeetingStatus.SCHEDULED.equals(this.status);
    }
}
