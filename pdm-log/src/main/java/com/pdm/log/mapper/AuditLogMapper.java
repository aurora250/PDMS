package com.pdm.log.mapper;

import com.pdm.log.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志数据访问层接口
 * 基于MyBatis-Plus的BaseMapper实现审计日志表的基础CRUD操作
 *
 * @author 开发者
 * @since 1.0.0
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}