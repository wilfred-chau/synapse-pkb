package com.wilfredchau.synapsepkb.tag.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RenameTagResponse(TagResponse tag, boolean changed, @JsonIgnore String previousName) {}
