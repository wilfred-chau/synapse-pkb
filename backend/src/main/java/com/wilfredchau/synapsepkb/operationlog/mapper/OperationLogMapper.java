package com.wilfredchau.synapsepkb.operationlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wilfredchau.synapsepkb.operationlog.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
