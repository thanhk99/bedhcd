package com.api.bedhcd.modules.resolution.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class ResolutionException extends BaseDomainException {
    public ResolutionException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ResolutionException resolutionNotFound(String id) {
        return new ResolutionException(HttpStatus.NOT_FOUND, "RESOLUTION_NOT_FOUND",
                "Không tìm thấy nghị quyết: " + id);
    }

    public static ResolutionException notFound(String type, String id) {
        return new ResolutionException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Không tìm thấy " + type + ": " + id);
    }

    public static ResolutionException invalidVote(String message) {
        return new ResolutionException(HttpStatus.BAD_REQUEST, "INVALID_VOTE", message);
    }

    public static ResolutionException invalidState(String message) {
        return new ResolutionException(HttpStatus.BAD_REQUEST, "INVALID_STATE", message);
    }

    public static ResolutionException unauthorized() {
        return new ResolutionException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Người dùng không có quyền thực hiện hành động này");
    }

    public static ResolutionException resolutionClosed(String status) {
        return new ResolutionException(HttpStatus.FORBIDDEN, "resolution_CLOSED",
                "Biểu quyết đã đóng (Trạng thái: " + status + ")");
    }
}
