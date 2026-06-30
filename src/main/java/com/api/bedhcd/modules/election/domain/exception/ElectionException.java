package com.api.bedhcd.modules.election.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class ElectionException extends BaseDomainException {
    public ElectionException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ElectionException electionNotFound(String id) {
        return new ElectionException(HttpStatus.NOT_FOUND, "ELECTION_NOT_FOUND", "Không tìm thấy cuộc bầu cử: " + id);
    }

    public static ElectionException invalidVoteDistribution(String message) {
        return new ElectionException(HttpStatus.BAD_REQUEST, "INVALID_ELECTION_VOTE", message);
    }

    public static ElectionException unauthorized() {
        return new ElectionException(HttpStatus.UNAUTHORIZED, "ELECTION_UNAUTHORIZED", "Chưa đăng nhập");
    }

    public static ElectionException invalidState(String message) {
        return new ElectionException(HttpStatus.CONFLICT, "ELECTION_INVALID_STATE", message);
    }
}
