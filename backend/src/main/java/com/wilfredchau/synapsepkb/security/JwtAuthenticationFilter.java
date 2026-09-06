package com.wilfredchau.synapsepkb.security;

import com.wilfredchau.synapsepkb.common.exception.SecurityErrorResponseWriter;
import com.wilfredchau.synapsepkb.user.entity.PkbUserEntity;
import com.wilfredchau.synapsepkb.user.service.PkbUserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final PkbUserService pkbUserService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            PkbUserService pkbUserService,
            SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtService = jwtService;
        this.pkbUserService = pkbUserService;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);
        try {
            AuthenticatedUser tokenUser = jwtService.parseToken(token);
            PkbUserEntity databaseUser = pkbUserService.findByUsername(tokenUser.username()).orElse(null);

            if (databaseUser != null && databaseUser.isEnabled()) {
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                        databaseUser.getId(),
                        databaseUser.getUsername(),
                        databaseUser.getDisplayName(),
                        databaseUser.getSpaceKey());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null,
                                Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated request for user {}", authenticatedUser.username());
            }
        } catch (JwtException ex) {
            log.debug("JWT parsing failed for request {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
            securityErrorResponseWriter.writeInvalidToken(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
