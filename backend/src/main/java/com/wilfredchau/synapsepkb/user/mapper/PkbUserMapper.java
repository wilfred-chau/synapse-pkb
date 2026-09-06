package com.wilfredchau.synapsepkb.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wilfredchau.synapsepkb.user.entity.PkbUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PkbUserMapper extends BaseMapper<PkbUser> {

    PkbUser selectByUsername(@Param("username") String username);
}
