package com.pdm.keyperson.mapper;

import com.pdm.keyperson.entity.Alert;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警Mapper接口
 * 基于MyBatis-Plus实现告警表（alert）的CRUD操作
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
}