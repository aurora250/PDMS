package com.pdm.resident.mapper;

import com.pdm.resident.entity.ResidentRelation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResidentRelationMapper extends BaseMapper<ResidentRelation> {

    @Select("SELECT * FROM resident_relation WHERE relation_person_uuid = #{uuid} AND is_deleted = 0")
    ResidentRelation selectByPersonUuid(@Param("uuid") String uuid);

    /** 反向查询子女：谁以此UUID为父亲或母亲 */
    @Select("SELECT rr.relation_person_uuid AS uuid, r.name, r.gender " + "FROM resident_relation rr "
            + "LEFT JOIN resident r ON rr.relation_person_uuid = r.uuid "
            + "WHERE (rr.father_uuid = #{uuid} OR rr.mother_uuid = #{uuid}) AND rr.is_deleted = 0")
    List<Map<String, Object>> selectChildren(@Param("uuid") String uuid);
}
