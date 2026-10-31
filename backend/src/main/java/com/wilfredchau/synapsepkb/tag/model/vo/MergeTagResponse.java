package com.wilfredchau.synapsepkb.tag.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record MergeTagResponse(Long sourceTagId, TagResponse targetTag, @JsonIgnore String sourceName) {}
