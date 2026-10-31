package com.wilfredchau.synapsepkb.tag.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MergeTagRequest(@NotNull @Positive Long targetTagId) {}
