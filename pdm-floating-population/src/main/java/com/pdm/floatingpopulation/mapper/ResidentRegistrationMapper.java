package com.pdm.floatingpopulation.mapper;

import com.pdm.floatingpopulation.entity.ResidentRegistration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 常住人口登记 Mapper 接口。
 *
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，提供常住人口登记表的基础 CRUD 操作。
 * </p>
 */
@Mapper
public interface ResidentRegistrationMapper extends BaseMapper<ResidentRegistration> {
}
