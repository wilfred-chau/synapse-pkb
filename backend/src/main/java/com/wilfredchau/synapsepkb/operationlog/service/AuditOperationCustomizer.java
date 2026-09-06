package com.wilfredchau.synapsepkb.operationlog.service;

import com.wilfredchau.synapsepkb.operationlog.model.AuditInvocationContext;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;

public interface AuditOperationCustomizer {

    void customize(AuditLogDraft draft, AuditInvocationContext context);
}
