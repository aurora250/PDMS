package com.pdm.household.mapper;

import com.pdm.household.entity.HouseholdBusinessRequest;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 户政业务申请 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，提供户政业务申请表的基础 CRUD 操作。
 */
@Mapper
public interface HouseholdBusinessRequestMapper extends BaseMapper<HouseholdBusinessRequest> {}
