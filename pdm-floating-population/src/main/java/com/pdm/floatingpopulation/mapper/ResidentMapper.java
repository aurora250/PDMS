package com.pdm.floatingpopulation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface ResidentMapper {
    /** 查询居民居住地信息 */
    @Select("SELECT id, area_id, residence FROM resident WHERE uuid = #{uuid} AND is_deleted = 0")
    Map<String, Object> selectByUuid(@Param("uuid") String uuid);

    /** 更新居民居住地和所属区域 */
    @Update("UPDATE resident SET residence = #{residence}, area_id = #{areaId} WHERE uuid = #{uuid} AND is_deleted = 0")
    int updateResidenceByUuid(@Param("uuid") String uuid, @Param("residence") String residence,
            @Param("areaId") Long areaId);
}
