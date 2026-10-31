package com.wilfredchau.synapsepkb.tag.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.operationlog.model.AuditInvocationContext;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import com.wilfredchau.synapsepkb.operationlog.service.AuditOperationCustomizer;
import com.wilfredchau.synapsepkb.tag.model.vo.MergeTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.RenameTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.TagResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TagAuditCustomizer implements AuditOperationCustomizer {
    private final ObjectMapper objectMapper;

    public TagAuditCustomizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void customize(AuditLogDraft draft, AuditInvocationContext context) {
        Map<String, Object> details;
        if (context.result() instanceof TagResponse tag) {
            draft.setTargetId(tag.id().toString());
            details = Map.of("name", tag.name());
        } else if (context.result() instanceof RenameTagResponse renamed) {
            if (!renamed.changed()) {
                draft.skip();
                return;
            }
            draft.setTargetId(renamed.tag().id().toString());
            details = Map.of("previousName", renamed.previousName(), "name", renamed.tag().name());
        } else if (context.result() instanceof MergeTagResponse merged) {
            draft.setTargetId(merged.targetTag().id().toString());
            details = Map.of("sourceTagId", merged.sourceTagId(), "sourceName", merged.sourceName(),
                    "targetTagId", merged.targetTag().id(), "targetName", merged.targetTag().name());
        } else {
            draft.skip();
            return;
        }
        try {
            draft.setDetailsJson(objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize tag audit details", ex);
        }
    }
}
