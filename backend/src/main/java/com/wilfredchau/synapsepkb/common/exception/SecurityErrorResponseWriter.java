package com.wilfredchau.synapsepkb.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.common.api.ApiError;
import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
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
                HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required");
    }

    public void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN",
                "You do not have permission to access this resource");
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        String requestId = (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(RequestTracing.REQUEST_ID_HEADER, requestId);

        ApiResponse<Void> body = ApiResponse.failure(
                new ApiError(code, message, Map.of()),
                requestId);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
