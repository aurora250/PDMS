package com.pdm.household.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResidentMapper {
    /** 批量查询居民姓名（UUID → name） */
    @Select("<script>" + "SELECT uuid, name FROM resident WHERE uuid IN "
            + "<foreach collection='uuids' item='uuid' open='(' separator=',' close=')'>#{uuid}</foreach>"
            + "</script>")
    List<Map<String, Object>> batchGetNames(@Param("uuids") List<String> uuids);

    /** 按姓名关键词搜索居民UUID列表 */
    @Select("SELECT uuid FROM resident WHERE name LIKE CONCAT('%', #{keyword}, '%') AND is_deleted = 0 LIMIT 200")
    List<String> selectUuidsByName(@Param("keyword") String keyword);
}
