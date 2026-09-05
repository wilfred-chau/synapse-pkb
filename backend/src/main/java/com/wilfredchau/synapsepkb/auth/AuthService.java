package com.wilfredchau.synapsepkb.auth;

import com.wilfredchau.synapsepkb.auth.dto.AuthResponse;
import com.wilfredchau.synapsepkb.auth.dto.CurrentUserResponse;
import com.wilfredchau.synapsepkb.auth.dto.LoginRequest;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import com.wilfredchau.synapsepkb.security.JwtService;
import com.wilfredchau.synapsepkb.user.PkbUser;
import com.wilfredchau.synapsepkb.user.PkbUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getSpaceKey());

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
