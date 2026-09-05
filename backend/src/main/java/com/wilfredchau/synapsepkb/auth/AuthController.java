package com.wilfredchau.synapsepkb.auth;

import com.wilfredchau.synapsepkb.auth.dto.AuthResponse;
import com.wilfredchau.synapsepkb.auth.dto.CurrentUserResponse;
import com.wilfredchau.synapsepkb.auth.dto.LoginRequest;
import com.wilfredchau.synapsepkb.common.api.ApiResponse;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import jakarta.servlet.http.HttpServletRequest;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                authService.login(request),
                getRequestId(httpServletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                authService.me(currentUser),
                getRequestId(httpServletRequest));
    }

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);
    }
}
