package com.pdm.resident.mapper;

import com.pdm.resident.entity.ResidentRelation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResidentRelationMapper extends BaseMapper<ResidentRelation> {

    @Select(
            "SELECT * FROM resident_relation WHERE relation_person_uuid = #{uuid} AND is_deleted = 0")
    ResidentRelation selectByPersonUuid(@Param("uuid") String uuid);
}
