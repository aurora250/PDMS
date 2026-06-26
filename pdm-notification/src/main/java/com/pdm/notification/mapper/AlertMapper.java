package com.pdm.notification.mapper;

import com.pdm.notification.entity.Alert;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警信息数据访问层接口
 * 基于MyBatis-Plus实现Alert实体的数据库CRUD操作
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
}