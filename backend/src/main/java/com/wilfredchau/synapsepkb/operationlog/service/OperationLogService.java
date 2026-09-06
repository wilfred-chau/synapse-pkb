package com.wilfredchau.synapsepkb.operationlog.service;

import com.wilfredchau.synapsepkb.operationlog.entity.OperationLog;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;

public interface OperationLogService {

    OperationLog create(AuditLogDraft draft);
}
