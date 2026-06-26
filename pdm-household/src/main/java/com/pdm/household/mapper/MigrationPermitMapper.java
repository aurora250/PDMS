package com.pdm.household.mapper;

import com.pdm.household.entity.MigrationPermit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 迁移证 Mapper 接口。
 *
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，提供迁移证表的基础 CRUD 操作。
 * </p>
 */
@Mapper
public interface MigrationPermitMapper extends BaseMapper<MigrationPermit> {
}
