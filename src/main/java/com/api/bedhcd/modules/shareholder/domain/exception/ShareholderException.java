package com.api.bedhcd.modules.shareholder.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class ShareholderException extends BaseDomainException {

    public ShareholderException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ShareholderException notFound(String id) {
        return new ShareholderException(HttpStatus.NOT_FOUND, "SHAREHOLDER_NOT_FOUND", "Không tìm thấy cổ đông với ID: " + id);
    }

    public static ShareholderException badRequest(String message) {
        return new ShareholderException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ShareholderException locked(String message) {
        return new ShareholderException(HttpStatus.CONFLICT, "SHAREHOLDER_LOCKED", message);
    }
}
