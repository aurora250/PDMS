package com.pdm.keyperson.mapper;

import com.pdm.keyperson.entity.PetitionRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信访记录Mapper接口
 * 基于MyBatis-Plus实现信访记录表（petition_record）的CRUD操作
 *
 */
@Mapper
public interface PetitionRecordMapper extends BaseMapper<PetitionRecord> {
}