package com.wilfredchau.synapsepkb.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.currentTimeMillis();

        request.setAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(RequestTracing.REQUEST_ID_HEADER, requestId);
        MDC.put(RequestTracing.MDC_REQUEST_ID_KEY, requestId);

        log.debug("HTTP request started: method={}, uri={}, query={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("HTTP request completed: method={}, uri={}, status={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
            MDC.remove(RequestTracing.MDC_REQUEST_ID_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incomingRequestId = request.getHeader(RequestTracing.REQUEST_ID_HEADER);
        if (incomingRequestId != null && !incomingRequestId.isBlank()) {
            return incomingRequestId;
        }
        return UUID.randomUUID().toString();
    }
}
