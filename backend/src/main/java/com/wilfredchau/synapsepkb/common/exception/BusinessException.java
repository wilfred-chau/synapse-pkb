package com.wilfredchau.synapsepkb.common.exception;

import com.wilfredchau.synapsepkb.common.api.ApiError;
import com.wilfredchau.synapsepkb.common.api.ApiErrorCode;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ApiErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), Map.of());
    }

    public BusinessException(ApiErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public BusinessException(ApiErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public HttpStatus status() {
        return errorCode.status();
    }

    public ApiError toApiError() {
        return errorCode.toError(getMessage(), details);
    }
}
