package com.wilfredchau.synapsepkb.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wilfredchau.synapsepkb.tag.entity.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    List<Tag> listOwned(@Param("userId") long userId);
    List<Tag> lockOwned(@Param("userId") long userId, @Param("ids") List<Long> ids);
    int renameOwned(@Param("userId") long userId, @Param("id") long id,
                    @Param("name") String name, @Param("updatedAt") OffsetDateTime updatedAt);
    int deleteOwned(@Param("userId") long userId, @Param("id") long id);
}
