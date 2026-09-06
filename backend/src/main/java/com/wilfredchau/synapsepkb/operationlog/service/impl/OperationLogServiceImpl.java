package com.wilfredchau.synapsepkb.operationlog.service.impl;

import com.wilfredchau.synapsepkb.operationlog.entity.OperationLog;
import com.wilfredchau.synapsepkb.operationlog.mapper.OperationLogMapper;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import com.wilfredchau.synapsepkb.operationlog.service.OperationLogService;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public OperationLog create(AuditLogDraft draft) {
        OperationLog operationLog = new OperationLog();
        operationLog.setActorUserId(draft.getActorUserId());
        operationLog.setActorUsername(draft.getActorUsername());
        operationLog.setAction(draft.getAction());
        operationLog.setTargetType(draft.getTargetType());
        operationLog.setTargetId(draft.getTargetId());
        operationLog.setRequestId(draft.getRequestId());
        operationLog.setSuccess(draft.isSuccess());
        operationLog.setMessage(draft.getMessage());
        operationLog.setDetailsJson(draft.getDetailsJson());
        operationLog.setOccurredAt(draft.getOccurredAt() == null ? OffsetDateTime.now() : draft.getOccurredAt());
        operationLogMapper.insert(operationLog);
        return operationLog;
    }
}
