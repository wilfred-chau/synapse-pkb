package com.wilfredchau.synapsepkb.common.api;

import java.util.Map;
import org.springframework.http.HttpStatus;

public interface ApiErrorCode {

    String code();

    HttpStatus status();

    String defaultMessage();

    default ApiError toError() {
        return toError(defaultMessage(), Map.of());
    }

    default ApiError toError(String message) {
        return toError(message, Map.of());
    }

    default ApiError toError(Map<String, Object> details) {
        return toError(defaultMessage(), details);
    }

    default ApiError toError(String message, Map<String, Object> details) {
        return new ApiError(code(), message, details);
    }
}
