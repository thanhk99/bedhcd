package com.api.bedhcd.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseDomainException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    protected BaseDomainException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
