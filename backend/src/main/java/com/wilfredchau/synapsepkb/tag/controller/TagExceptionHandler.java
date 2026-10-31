package com.wilfredchau.synapsepkb.tag.controller;

import com.wilfredchau.synapsepkb.common.api.ApiErrorCode;
import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.api.CommonErrorCode;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import com.wilfredchau.synapsepkb.tag.model.TagErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(0)
@RestControllerAdvice(assignableTypes = TagController.class)
public class TagExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(TagExceptionHandler.class);

    @ExceptionHandler({DataAccessException.class, TransactionTimedOutException.class})
    public ResponseEntity<ApiResponse<Void>> handle(RuntimeException error, HttpServletRequest request) {
        ApiErrorCode code = CommonErrorCode.INTERNAL_SERVER_ERROR;
        if (error instanceof QueryTimeoutException || error instanceof TransactionTimedOutException) {
            code = TagErrorCode.TAG_OPERATION_CONFLICT;
        }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException pg) {
                if ("23505".equals(pg.getSQLState()) && pg.getServerErrorMessage() != null
                        && "uk_tags_user_name_key".equals(pg.getServerErrorMessage().getConstraint())) {
                    code = TagErrorCode.TAG_NAME_CONFLICT;
                } else if ("55P03".equals(pg.getSQLState()) || "57014".equals(pg.getSQLState())
                        || "40P01".equals(pg.getSQLState()) || "40001".equals(pg.getSQLState())) {
                    code = TagErrorCode.TAG_OPERATION_CONFLICT;
                }
            }
        }
        String requestId = (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);
        if (code == CommonErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("Tag persistence failed: requestId={}, errorType={}", requestId, error.getClass().getSimpleName());
        }
        return ResponseEntity.status(code.status()).body(ApiResponse.failure(code.toError(), requestId));
    }
}
