package com.api.bedhcd.modules.meeting.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class MeetingException extends BaseDomainException {
    
    public MeetingException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MeetingException notFound(String id) {
        return new MeetingException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "Không tìm thấy cuộc họp với ID: " + id);
    }

    public static MeetingException invalidState(String message) {
        return new MeetingException(HttpStatus.CONFLICT, "INVALID_MEETING_STATE", message);
    }
    
    public static MeetingException accessDenied(String action) {
        return new MeetingException(HttpStatus.FORBIDDEN, "MEETING_ACCESS_DENIED", "Bạn không có quyền thực hiện hành động: " + action);
    }

    public static MeetingException editRequestNotFound(String id) {
        return new MeetingException(HttpStatus.NOT_FOUND, "EDIT_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu chỉnh sửa với ID: " + id);
    }

    public static MeetingException pendingRequestAlreadyExists(String meetingId) {
        return new MeetingException(HttpStatus.CONFLICT, "PENDING_REQUEST_EXISTS",
                "Cuộc họp đã có một yêu cầu chỉnh sửa đang chờ duyệt. Vui lòng chờ xử lý trước khi gửi yêu cầu mới. MeetingId: " + meetingId);
    }

    public static MeetingException editRequestNotPending(String requestId) {
        return new MeetingException(HttpStatus.CONFLICT, "EDIT_REQUEST_NOT_PENDING",
                "Yêu cầu chỉnh sửa không ở trạng thái PENDING và không thể thực hiện thao tác này. RequestId: " + requestId);
    }
}
