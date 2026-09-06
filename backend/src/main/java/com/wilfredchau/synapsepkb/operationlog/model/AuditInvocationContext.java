package com.wilfredchau.synapsepkb.operationlog.model;

import com.wilfredchau.synapsepkb.operationlog.annotation.AuditOperation;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import java.lang.reflect.Method;

public record AuditInvocationContext(
        AuditOperation annotation,
        Method method,
        Object[] arguments,
        Object result,
        AuthenticatedUser currentUser,
        String requestId) {
}
