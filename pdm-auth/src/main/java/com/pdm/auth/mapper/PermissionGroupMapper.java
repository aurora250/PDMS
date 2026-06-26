package com.pdm.auth.mapper;

import com.pdm.auth.entity.PermissionGroup;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 权限组 Mapper 接口。
 *
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，自动获得 CRUD 基础操作，无需额外 SQL 映射。
 * </p>
 */
@Mapper
public interface PermissionGroupMapper extends BaseMapper<PermissionGroup> {
}
