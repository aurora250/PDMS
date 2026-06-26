package com.pdm.floatingpopulation.mapper;

import com.pdm.floatingpopulation.entity.ResidentPermit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 居住证 Mapper 接口。
 *
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，提供居住证表的基础 CRUD 操作。
 * </p>
 */
@Mapper
public interface ResidentPermitMapper extends BaseMapper<ResidentPermit> {
}
