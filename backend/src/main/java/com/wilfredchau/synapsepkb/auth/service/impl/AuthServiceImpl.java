package com.wilfredchau.synapsepkb.auth.service.impl;

import com.wilfredchau.synapsepkb.auth.model.dto.LoginRequest;
import com.wilfredchau.synapsepkb.auth.model.vo.AuthResponse;
import com.wilfredchau.synapsepkb.auth.model.vo.CurrentUserResponse;
import com.wilfredchau.synapsepkb.auth.service.audit.AuthLoginAuditCustomizer;
import com.wilfredchau.synapsepkb.auth.service.AuthService;
import com.wilfredchau.synapsepkb.common.api.CommonErrorCode;
import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import com.wilfredchau.synapsepkb.operationlog.annotation.AuditOperation;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import com.wilfredchau.synapsepkb.security.JwtService;
import com.wilfredchau.synapsepkb.user.entity.PkbUser;
import com.wilfredchau.synapsepkb.user.service.PkbUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final PkbUserService pkbUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            PkbUserService pkbUserService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.pkbUserService = pkbUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @AuditOperation(
            action = "AUTH_LOGIN",
            targetType = "USER",
            message = "User logged in successfully",
            customizer = AuthLoginAuditCustomizer.class)
    public AuthResponse login(LoginRequest request) {
        PkbUser user = pkbUserService.findByUsername(request.username())
                .filter(PkbUser::isEnabled)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.debug("Login rejected for user {}", request.username());
            throw new BusinessException(CommonErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getSpaceKey());

        log.debug("Login succeeded for user {}", authenticatedUser.username());
        return new AuthResponse(jwtService.generateToken(authenticatedUser), toCurrentUser(authenticatedUser));
    }

    @Override
    public CurrentUserResponse me(AuthenticatedUser currentUser) {
        return toCurrentUser(currentUser);
    }

    private CurrentUserResponse toCurrentUser(AuthenticatedUser currentUser) {
        return new CurrentUserResponse(
                currentUser.id(),
                currentUser.username(),
                currentUser.displayName(),
                currentUser.spaceKey());
    }
}
