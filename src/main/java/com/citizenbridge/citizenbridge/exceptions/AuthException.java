package com.citizenbridge.citizenbridge.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AuthException extends RuntimeException {
    private HttpStatus status = HttpStatus.UNAUTHORIZED; // Default status

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public AuthException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidCredentialsException extends AuthException {
        public InvalidCredentialsException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class UserAlreadyExistsException extends AuthException {
        public UserAlreadyExistsException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends AuthException {
        public UserNotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class TokenException extends AuthException {
        public TokenException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccountDeactivatedException extends AuthException {
        public AccountDeactivatedException(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InsufficientPrivilegesException extends AuthException {
        public InsufficientPrivilegesException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }
}