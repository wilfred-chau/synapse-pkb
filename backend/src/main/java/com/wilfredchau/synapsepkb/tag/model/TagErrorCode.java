package com.wilfredchau.synapsepkb.tag.model;

import com.wilfredchau.synapsepkb.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum TagErrorCode implements ApiErrorCode {
    TAG_INVALID_NAME(HttpStatus.BAD_REQUEST, "Tag name must contain 1-32 characters without hidden separators or controls"),
    TAG_INVALID_ID(HttpStatus.BAD_REQUEST, "Tag ID must be positive"),
    TAG_NAME_CONFLICT(HttpStatus.CONFLICT, "A tag with this name already exists"),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Tag was not found"),
    TAG_MERGE_SAME_TAG(HttpStatus.BAD_REQUEST, "Source and target must be different tags"),
    TAG_OPERATION_CONFLICT(HttpStatus.CONFLICT, "Tags changed or are busy; refresh and retry");

    private final HttpStatus status;
    private final String message;

    TagErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String code() { return name(); }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return message; }
}
