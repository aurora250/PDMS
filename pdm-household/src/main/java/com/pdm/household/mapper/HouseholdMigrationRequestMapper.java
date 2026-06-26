package com.pdm.household.mapper;

import com.pdm.household.entity.HouseholdMigrationRequest;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 户口迁移申请 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，提供户口迁移申请表的基础 CRUD 操作。
 */
@Mapper
public interface HouseholdMigrationRequestMapper extends BaseMapper<HouseholdMigrationRequest> {}
