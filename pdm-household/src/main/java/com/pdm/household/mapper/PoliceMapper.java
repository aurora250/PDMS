package com.pdm.household.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface PoliceMapper {
    /** 按 user_uuid 查询警员信息（含 area_id、police_station） */
    @Select("SELECT p.area_id, p.police_station, p.police_number, a.area_code"
            + " FROM police p LEFT JOIN area a ON p.area_id = a.area_id"
            + " WHERE p.user_uuid = #{userUuid} AND p.is_deleted = 0")
    Map<String, Object> selectByUserUuid(@Param("userUuid") String userUuid);
}
