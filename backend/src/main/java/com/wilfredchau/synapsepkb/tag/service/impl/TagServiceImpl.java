package com.wilfredchau.synapsepkb.tag.service.impl;

import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import com.wilfredchau.synapsepkb.operationlog.annotation.AuditOperation;
import com.wilfredchau.synapsepkb.security.CurrentUser;
import com.wilfredchau.synapsepkb.tag.entity.Tag;
import com.wilfredchau.synapsepkb.tag.mapper.TagMapper;
import com.wilfredchau.synapsepkb.tag.model.TagErrorCode;
import com.wilfredchau.synapsepkb.tag.model.TagName;
import com.wilfredchau.synapsepkb.tag.model.vo.MergeTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.RenameTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.TagResponse;
import com.wilfredchau.synapsepkb.tag.service.TagService;
import com.wilfredchau.synapsepkb.tag.service.audit.TagAuditCustomizer;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagServiceImpl implements TagService {
    private final TagMapper mapper;

    public TagServiceImpl(TagMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<TagResponse> list() {
        return mapper.listOwned(CurrentUser.id()).stream().map(TagResponse::from).toList();
    }

    @Override
    @Transactional(timeout = 5)
    @AuditOperation(action = "TAG_CREATE", targetType = "TAG", customizer = TagAuditCustomizer.class)
    public TagResponse create(String name) {
        Tag tag = new Tag();
        tag.setUserId(CurrentUser.id());
        tag.setName(TagName.normalize(name));
        checkUpdated(mapper.insert(tag));
        return TagResponse.from(tag);
    }

    @Override
    @Transactional(timeout = 5)
    @AuditOperation(action = "TAG_RENAME", targetType = "TAG", customizer = TagAuditCustomizer.class)
    public RenameTagResponse rename(long id, String name) {
        long userId = CurrentUser.id();
        checkId(id);
        Tag tag = findLocked(mapper.lockOwned(userId, List.of(id)), id);
        String normalized = TagName.normalize(name);
        String previousName = tag.getName();
        if (previousName.equals(normalized)) {
            return new RenameTagResponse(TagResponse.from(tag), false, previousName);
        }
        tag.setName(normalized);
        tag.setUpdatedAt(OffsetDateTime.now());
        checkUpdated(mapper.renameOwned(userId, id, normalized, tag.getUpdatedAt()));
        return new RenameTagResponse(TagResponse.from(tag), true, previousName);
    }

    @Override
    @Transactional(timeout = 5)
    @AuditOperation(action = "TAG_MERGE", targetType = "TAG", customizer = TagAuditCustomizer.class)
    public MergeTagResponse merge(long sourceId, long targetId) {
        long userId = CurrentUser.id();
        checkId(sourceId);
        checkId(targetId);
        if (sourceId == targetId) {
            throw new BusinessException(TagErrorCode.TAG_MERGE_SAME_TAG);
        }
        List<Tag> locked = mapper.lockOwned(userId, List.of(sourceId, targetId));
        Tag source = findLocked(locked, sourceId);
        Tag target = findLocked(locked, targetId);
        target.setUpdatedAt(OffsetDateTime.now());
        checkUpdated(mapper.renameOwned(userId, targetId, target.getName(), target.getUpdatedAt()));
        checkUpdated(mapper.deleteOwned(userId, sourceId));
        return new MergeTagResponse(sourceId, TagResponse.from(target), source.getName());
    }

    private Tag findLocked(List<Tag> tags, long id) {
        return tags.stream().filter(tag -> tag.getId() == id).findFirst()
                .orElseThrow(() -> new BusinessException(TagErrorCode.TAG_NOT_FOUND));
    }

    private void checkId(long id) {
        if (id <= 0) {
            throw new BusinessException(TagErrorCode.TAG_INVALID_ID);
        }
    }

    private void checkUpdated(int rows) {
        if (rows != 1) {
            throw new BusinessException(TagErrorCode.TAG_OPERATION_CONFLICT);
        }
    }
}
