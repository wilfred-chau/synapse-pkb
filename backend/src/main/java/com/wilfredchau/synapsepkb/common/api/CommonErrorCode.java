package com.wilfredchau.synapsepkb.common.api;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ApiErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Request validation failed"),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "Request failed"),
    AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    AUTH_INVALID_TOKEN("AUTH_INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Authentication token is invalid or expired"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "You do not have permission to access this resource"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Requested resource was not found"),
    REQUEST_FAILED("REQUEST_FAILED", HttpStatus.BAD_REQUEST, "Request failed"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    CommonErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    public static CommonErrorCode fromStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> BAD_REQUEST;
            case UNAUTHORIZED -> AUTHENTICATION_REQUIRED;
            case FORBIDDEN -> ACCESS_DENIED;
            case NOT_FOUND -> RESOURCE_NOT_FOUND;
            default -> REQUEST_FAILED;
        };
    }
}
