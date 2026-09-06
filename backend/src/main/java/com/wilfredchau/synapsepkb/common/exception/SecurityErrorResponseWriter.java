package com.wilfredchau.synapsepkb.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.common.api.ApiErrorCode;
import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.api.CommonErrorCode;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(
                request,
                response,
                CommonErrorCode.AUTHENTICATION_REQUIRED);
    }

    public void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(
                request,
                response,
                CommonErrorCode.ACCESS_DENIED);
    }

    public void writeInvalidToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(
                request,
                response,
                CommonErrorCode.AUTH_INVALID_TOKEN);
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiErrorCode errorCode) throws IOException {
        String requestId = (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);

        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (requestId != null) {
            response.setHeader(RequestTracing.REQUEST_ID_HEADER, requestId);
        }

        ApiResponse<Void> body = ApiResponse.failure(
                errorCode.toError(),
                requestId);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
