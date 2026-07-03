package com.pdm.auth.mapper;

import com.pdm.auth.entity.Police;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PoliceMapper extends BaseMapper<Police> {

    @Select("SELECT * FROM police WHERE police_number = #{policeNumber} AND is_deleted = 0")
    Police selectByPoliceNumber(@Param("policeNumber") String policeNumber);

    @Select("SELECT * FROM police WHERE user_uuid = #{userUuid} AND is_deleted = 0")
    Police selectByUserUuid(@Param("userUuid") String userUuid);

    @Select("<script>" + "SELECT p.*, r.name AS resident_name FROM police p "
            + "LEFT JOIN resident r ON p.resident_uuid = r.uuid AND r.is_deleted = 0 " + "WHERE p.is_deleted = 0 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "AND (p.police_number LIKE CONCAT('%',#{keyword},'%') "
            + "OR p.police_station LIKE CONCAT('%',#{keyword},'%') "
            + "OR p.department LIKE CONCAT('%',#{keyword},'%'))" + "</if>"
            + "<if test='residentUuid != null and residentUuid != \"\"'>" + "AND p.resident_uuid = #{residentUuid}"
            + "</if>" + "ORDER BY p.create_time DESC" + "</script>")
    Page<Police> selectPageWithResidentName(Page<Police> page, @Param("keyword") String keyword,
            @Param("residentUuid") String residentUuid);

    /** 查询未关联系统用户的民警列表（user_uuid IS NULL），JOIN resident 获取姓名 */
    @Select("SELECT p.police_number, p.resident_uuid, p.police_station, p.police_rank, p.jurisdiction,"
            + " r.name AS resident_name" + " FROM police p JOIN resident r ON p.resident_uuid = r.uuid"
            + " WHERE p.user_uuid IS NULL AND p.is_deleted = 0" + " ORDER BY p.police_number")
    List<Map<String, Object>> selectUnassociated();

    /** 查询所有在岗民警的 resident_uuid 列表（用于排除民警身份的居民） */
    @Select("SELECT resident_uuid FROM police WHERE is_deleted = 0")
    List<String> selectAllActiveResidentUuids();
}
