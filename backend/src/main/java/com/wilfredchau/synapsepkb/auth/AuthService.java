package com.wilfredchau.synapsepkb.auth;

import com.wilfredchau.synapsepkb.auth.dto.AuthResponse;
import com.wilfredchau.synapsepkb.auth.dto.CurrentUserResponse;
import com.wilfredchau.synapsepkb.auth.dto.LoginRequest;
import com.wilfredchau.synapsepkb.common.api.CommonErrorCode;
import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import com.wilfredchau.synapsepkb.security.JwtService;
import com.wilfredchau.synapsepkb.user.PkbUser;
import com.wilfredchau.synapsepkb.user.PkbUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final PkbUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            PkbUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        PkbUser user = userRepository.findByUsername(request.username())
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
