package com.wilfredchau.synapsepkb.tag.controller;

import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import com.wilfredchau.synapsepkb.tag.model.dto.MergeTagRequest;
import com.wilfredchau.synapsepkb.tag.model.dto.TagNameRequest;
import com.wilfredchau.synapsepkb.tag.model.vo.MergeTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.RenameTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.TagResponse;
import com.wilfredchau.synapsepkb.tag.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> list(HttpServletRequest request) {
        return ApiResponse.success(service.list(), requestId(request));
    }

    @PostMapping
    public ApiResponse<TagResponse> create(@RequestBody TagNameRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.create(body.name()), requestId(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RenameTagResponse> rename(@PathVariable long id, @RequestBody TagNameRequest body,
                                                HttpServletRequest request) {
        return ApiResponse.success(service.rename(id, body.name()), requestId(request));
    }

    @PostMapping("/{sourceId}/merge")
    public ApiResponse<MergeTagResponse> merge(@PathVariable long sourceId, @Valid @RequestBody MergeTagRequest body,
                                              HttpServletRequest request) {
        return ApiResponse.success(service.merge(sourceId, body.targetTagId()), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);
    }
}
