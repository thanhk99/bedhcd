package com.api.bedhcd.modules.identity.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class IdentityException extends BaseDomainException {

    protected IdentityException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static IdentityException userNotFound(String userId) {
        return new IdentityException(
                HttpStatus.NOT_FOUND,
                "IDENTITY_USER_NOT_FOUND",
                "User not found with ID: " + userId
        );
    }

    public static IdentityException usernameNotFound(String username) {
        return new IdentityException(
                HttpStatus.NOT_FOUND,
                "IDENTITY_USERNAME_NOT_FOUND",
                "User not found with username: " + username
        );
    }

    public static IdentityException invalidCredentials() {
        return new IdentityException(
                HttpStatus.UNAUTHORIZED,
                "IDENTITY_INVALID_CREDENTIALS",
                "Invalid username or password"
        );
    }

    public static IdentityException usernameAlreadyExists(String username) {
        return new IdentityException(
                HttpStatus.CONFLICT,
                "IDENTITY_USERNAME_EXISTS",
                "Username already exists: " + username
        );
    }

    public static IdentityException emailAlreadyExists(String email) {
        return new IdentityException(
                HttpStatus.CONFLICT,
                "IDENTITY_EMAIL_EXISTS",
                "Email already exists: " + email
        );
    }

    public static IdentityException invalidRefreshToken() {
        return new IdentityException(
                HttpStatus.UNAUTHORIZED,
                "IDENTITY_INVALID_REFRESH_TOKEN",
                "Invalid or expired refresh token"
        );
    }

    public static IdentityException unauthorized(String message) {
        return new IdentityException(
                HttpStatus.UNAUTHORIZED,
                "IDENTITY_UNAUTHORIZED",
                message
        );
    }

    public static IdentityException accessDenied(String message) {
        return new IdentityException(
                HttpStatus.FORBIDDEN,
                "IDENTITY_ACCESS_DENIED",
                message
        );
    }

    public static IdentityException invalidState(String message) {
        return new IdentityException(
                HttpStatus.FORBIDDEN,
                "IDENTITY_INVALID_STATE",
                message
        );
    }
}
