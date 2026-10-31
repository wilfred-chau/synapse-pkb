package com.wilfredchau.synapsepkb.tag.service;

import com.wilfredchau.synapsepkb.tag.model.vo.MergeTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.RenameTagResponse;
import com.wilfredchau.synapsepkb.tag.model.vo.TagResponse;
import java.util.List;

public interface TagService {
    List<TagResponse> list();
    TagResponse create(String name);
    RenameTagResponse rename(long id, String name);
    MergeTagResponse merge(long sourceId, long targetId);
}
