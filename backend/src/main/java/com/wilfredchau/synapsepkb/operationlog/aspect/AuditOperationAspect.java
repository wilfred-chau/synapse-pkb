package com.wilfredchau.synapsepkb.operationlog.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.common.logging.RequestTracing;
import com.wilfredchau.synapsepkb.operationlog.annotation.AuditOperation;
import com.wilfredchau.synapsepkb.operationlog.model.AuditInvocationContext;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import com.wilfredchau.synapsepkb.operationlog.service.AuditOperationCustomizer;
import com.wilfredchau.synapsepkb.operationlog.service.OperationLogService;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditOperationAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditOperationAspect.class);

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;

    public AuditOperationAspect(
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            OperationLogService operationLogService) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(auditOperation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditOperation auditOperation) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            AuditLogDraft draft = buildDefaultDraft(joinPoint, auditOperation, result);
            resolveCustomizer(auditOperation).customize(draft, buildContext(joinPoint, auditOperation, result));
            operationLogService.create(draft);
        } catch (Exception ex) {
            log.warn("Audit operation logging failed: action={}, method={}, reason={}",
                    auditOperation.action(),
                    joinPoint.getSignature().toShortString(),
                    ex.getMessage(),
                    ex);
        }

        return result;
    }

    private AuditLogDraft buildDefaultDraft(
            ProceedingJoinPoint joinPoint,
            AuditOperation auditOperation,
            Object result) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AuthenticatedUser currentUser = resolveCurrentUser();

        AuditLogDraft draft = new AuditLogDraft();
        draft.setActorUserId(currentUser == null ? null : currentUser.id());
        draft.setActorUsername(currentUser == null ? null : currentUser.username());
        draft.setAction(auditOperation.action());
        draft.setTargetType(auditOperation.targetType());
        draft.setSuccess(true);
        draft.setMessage(auditOperation.message().isBlank() ? auditOperation.action() : auditOperation.message());
        draft.setRequestId(resolveRequestId());
        draft.setOccurredAt(OffsetDateTime.now());
        draft.setDetailsJson(buildDefaultDetails(method, result));
        return draft;
    }

    private AuditInvocationContext buildContext(
            ProceedingJoinPoint joinPoint,
            AuditOperation auditOperation,
            Object result) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return new AuditInvocationContext(
                auditOperation,
                method,
                joinPoint.getArgs(),
                result,
                resolveCurrentUser(),
                resolveRequestId());
    }

    private AuditOperationCustomizer resolveCustomizer(AuditOperation auditOperation) {
        return applicationContext.getBean(auditOperation.customizer());
    }

    private AuthenticatedUser resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        return null;
    }

    private String resolveRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return (String) servletRequestAttributes.getRequest().getAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE);
    }

    private String buildDefaultDetails(Method method, Object result) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("serviceClass", method.getDeclaringClass().getSimpleName());
        details.put("method", method.getName());
        details.put("resultType", result == null ? null : result.getClass().getSimpleName());
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit details", ex);
        }
    }
}
