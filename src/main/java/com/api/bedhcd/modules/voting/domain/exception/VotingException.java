package com.api.bedhcd.modules.voting.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class VotingException extends BaseDomainException {
    public VotingException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static VotingException resolutionNotFound(String id) {
        return new VotingException(HttpStatus.NOT_FOUND, "RESOLUTION_NOT_FOUND", "Không tìm thấy nghị quyết: " + id);
    }

    public static VotingException notFound(String type, String id) {
        return new VotingException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy " + type + ": " + id);
    }

    public static VotingException invalidVote(String message) {
        return new VotingException(HttpStatus.BAD_REQUEST, "INVALID_VOTE", message);
    }

    public static VotingException invalidState(String message) {
        return new VotingException(HttpStatus.BAD_REQUEST, "INVALID_STATE", message);
    }

    public static VotingException unauthorized() {
        return new VotingException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Người dùng không có quyền thực hiện hành động này");
    }
    
    public static VotingException votingClosed(String status) {
        return new VotingException(HttpStatus.FORBIDDEN, "VOTING_CLOSED", "Biểu quyết đã đóng (Trạng thái: " + status + ")");
    }
}
