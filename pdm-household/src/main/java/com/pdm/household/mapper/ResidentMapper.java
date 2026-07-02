package com.pdm.household.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /** 按UUID查询居民完整信息 */
    @Select("SELECT * FROM resident WHERE uuid = #{uuid} AND is_deleted = 0")
    Map<String, Object> selectByUuid(@Param("uuid") String uuid);

    /** 直接插入居民（用于出生登记等业务，绕过 ResidentServiceImpl 的身份证校验） */
    @Insert("INSERT INTO resident (uuid, name, gender, id_card_no, nation, birth_date, phone,"
            + " residence, area_id, household_type, household_status, household_address, household_area_id)"
            + " VALUES (#{uuid}, #{name}, #{gender}, #{idCardNo}, #{nation}, #{birthDate}, #{phone},"
            + " #{residence}, #{areaId}, #{householdType}, #{householdStatus}, #{householdAddress}, #{householdAreaId})")
    int insertResident(Map<String, Object> params);

    /** 更新居民户籍状态（死亡注销等场景） */
    @Update("UPDATE resident SET household_status = #{status}, update_time = CURRENT_TIMESTAMP"
            + " WHERE uuid = #{uuid} AND is_deleted = 0")
    int updateHouseholdStatus(@Param("uuid") String uuid, @Param("status") String status);

    /** 按UUID列表查询居民完整信息 */
    @Select("<script>SELECT uuid, residence, area_id, household_address, household_area_id"
            + " FROM resident WHERE uuid IN "
            + "<foreach collection='uuids' item='u' open='(' separator=',' close=')'>#{u}</foreach>"
            + " AND is_deleted = 0</script>")
    List<Map<String, Object>> selectByUuids(@Param("uuids") List<String> uuids);

    /** 更新居民户籍地址和户籍地区（迁移审批通过后同步） */
    @Update("UPDATE resident SET household_address = #{address}, household_area_id = #{areaId},"
            + " update_time = CURRENT_TIMESTAMP WHERE uuid = #{uuid} AND is_deleted = 0")
    int updateHouseholdAddress(@Param("uuid") String uuid, @Param("address") String address,
            @Param("areaId") Long areaId);
}
