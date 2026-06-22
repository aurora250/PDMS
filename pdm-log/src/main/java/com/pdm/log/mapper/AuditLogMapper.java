package com.pdm.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdm.log.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
