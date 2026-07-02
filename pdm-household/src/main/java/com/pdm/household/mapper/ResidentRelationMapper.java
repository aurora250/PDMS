package com.pdm.household.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResidentRelationMapper {
    /** 插入家庭关系记录（出生登记同步） */
    @Insert("INSERT INTO resident_relation (relation_person_uuid, father_uuid, mother_uuid)"
            + " VALUES (#{childUuid}, #{fatherUuid}, #{motherUuid})"
            + " ON CONFLICT (relation_person_uuid) WHERE is_deleted = 0"
            + " DO UPDATE SET father_uuid = #{fatherUuid}, mother_uuid = #{motherUuid}, update_time = CURRENT_TIMESTAMP")
    int upsertRelation(@Param("childUuid") String childUuid,
            @Param("fatherUuid") String fatherUuid,
            @Param("motherUuid") String motherUuid);
}
