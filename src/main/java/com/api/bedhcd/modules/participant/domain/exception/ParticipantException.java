package com.api.bedhcd.modules.participant.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class ParticipantException extends BaseDomainException {
    
    public ParticipantException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ParticipantException notFound(String message) {
        return new ParticipantException(HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND", message);
    }

    public static ParticipantException badRequest(String message) {
        return new ParticipantException(HttpStatus.BAD_REQUEST, "INVALID_PARTICIPANT_REQUEST", message);
    }

    public static ParticipantException invalidState(String message) {
        return new ParticipantException(HttpStatus.CONFLICT, "INVALID_PARTICIPANT_STATE", message);
    }
}
