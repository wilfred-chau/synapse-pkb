package com.wilfredchau.synapsepkb.auth.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.auth.model.vo.AuthResponse;
import com.wilfredchau.synapsepkb.operationlog.model.AuditInvocationContext;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import com.wilfredchau.synapsepkb.operationlog.service.AuditOperationCustomizer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuthLoginAuditCustomizer implements AuditOperationCustomizer {

    private final ObjectMapper objectMapper;

    public AuthLoginAuditCustomizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void customize(AuditLogDraft draft, AuditInvocationContext context) {
        if (!(context.result() instanceof AuthResponse authResponse)) {
            return;
        }

        draft.setActorUserId(authResponse.user().id());
        draft.setActorUsername(authResponse.user().username());
        draft.setTargetId(String.valueOf(authResponse.user().id()));
        draft.setMessage("User logged in successfully");
        draft.setDetailsJson(toJson(Map.of(
                "spaceKey", authResponse.user().spaceKey(),
                "displayName", authResponse.user().displayName(),
                "channel", "PASSWORD_LOGIN")));
    }

    private String toJson(Map<String, Object> source) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(source));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize auth login audit details", ex);
        }
    }
}
