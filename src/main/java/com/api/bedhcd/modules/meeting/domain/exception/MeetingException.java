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
}
