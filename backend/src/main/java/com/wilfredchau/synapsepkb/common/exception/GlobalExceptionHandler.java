package com.wilfredchau.synapsepkb.common.exception;

import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.api.CommonErrorCode;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        details.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(ApiResponse.failure(
                CommonErrorCode.VALIDATION_ERROR.toError(details),
                getRequestId(request)));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(ApiResponse.failure(
                exception.toApiError(),
                getRequestId(request)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        CommonErrorCode errorCode = CommonErrorCode.fromStatus(status);

        return ResponseEntity.status(status).body(ApiResponse.failure(
                errorCode.toError(resolveReason(exception)),
                getRequestId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        log.error("Unhandled application exception", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(
                CommonErrorCode.INTERNAL_SERVER_ERROR.toError(),
                getRequestId(request)));
    }

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);
    }

    private String resolveReason(ResponseStatusException exception) {
        if (exception.getReason() != null && !exception.getReason().isBlank()) {
            return exception.getReason();
        }
        return "Request failed";
    }
}
