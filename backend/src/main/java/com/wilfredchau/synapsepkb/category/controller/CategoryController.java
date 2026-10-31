package com.wilfredchau.synapsepkb.category.controller;

import com.wilfredchau.synapsepkb.category.model.Category;
import com.wilfredchau.synapsepkb.category.model.vo.CategoryResponse;
import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {
    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> list(HttpServletRequest request) {
        return ApiResponse.success(Arrays.stream(Category.values())
                .map(category -> new CategoryResponse(category.code(), category.displayName())).toList(),
                (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE));
    }
}
