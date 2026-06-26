package com.pdm.floatingpopulation.mapper;

import com.pdm.floatingpopulation.entity.FpRegisterRecord;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 流动人口登记记录 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，提供流动人口登记记录表的基础 CRUD 操作。
 */
@Mapper
public interface FpRegisterRecordMapper extends BaseMapper<FpRegisterRecord> {}
