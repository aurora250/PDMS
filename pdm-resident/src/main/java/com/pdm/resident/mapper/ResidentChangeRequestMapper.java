package com.pdm.resident.mapper;

import com.pdm.resident.entity.ResidentChangeRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 常住人口变更申请数据访问层
 * 基于MyBatis-Plus实现变更申请的基础数据库操作
 */
@Mapper
public interface ResidentChangeRequestMapper extends BaseMapper<ResidentChangeRequest> {
}