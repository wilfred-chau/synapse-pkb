package com.wilfredchau.synapsepkb.operationlog.annotation;

import com.wilfredchau.synapsepkb.operationlog.service.AuditOperationCustomizer;
import com.wilfredchau.synapsepkb.operationlog.service.NoopAuditOperationCustomizer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditOperation {

    String action();

    String targetType();

    String message() default "";

    Class<? extends AuditOperationCustomizer> customizer() default NoopAuditOperationCustomizer.class;
}
