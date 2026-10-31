package com.wilfredchau.synapsepkb.tag.model.vo;

import com.wilfredchau.synapsepkb.tag.entity.Tag;
import java.time.OffsetDateTime;

public record TagResponse(Long id, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt(), tag.getUpdatedAt());
    }
}
