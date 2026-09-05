package com.wilfredchau.synapsepkb.auth;

import com.wilfredchau.synapsepkb.auth.dto.AuthResponse;
import com.wilfredchau.synapsepkb.auth.dto.CurrentUserResponse;
import com.wilfredchau.synapsepkb.auth.dto.LoginRequest;
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
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return authService.me(currentUser);
    }
}
