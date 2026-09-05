package com.wilfredchau.synapsepkb.common.logging;

public final class RequestTracing {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    private RequestTracing() {
    }
}
