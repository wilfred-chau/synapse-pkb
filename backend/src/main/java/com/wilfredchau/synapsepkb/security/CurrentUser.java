package com.wilfredchau.synapsepkb.security;

import com.wilfredchau.synapsepkb.common.api.CommonErrorCode;
import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static long id() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
                || user.id() == null || user.id() <= 0) {
            throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
        }
        return user.id();
    }
}
