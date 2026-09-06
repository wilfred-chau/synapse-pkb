package com.wilfredchau.synapsepkb.operationlog.service;

import com.wilfredchau.synapsepkb.operationlog.model.AuditInvocationContext;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import org.springframework.stereotype.Component;

@Component
public class NoopAuditOperationCustomizer implements AuditOperationCustomizer {

    @Override
    public void customize(AuditLogDraft draft, AuditInvocationContext context) {
        // Keep the default fields resolved by the aspect.
    }
}
