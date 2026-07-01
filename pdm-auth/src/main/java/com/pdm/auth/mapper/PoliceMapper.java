package com.pdm.auth.mapper;

import com.pdm.auth.entity.Police;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PoliceMapper extends BaseMapper<Police> {

    @Select("SELECT * FROM police WHERE police_number = #{policeNumber} AND is_deleted = 0")
    Police selectByPoliceNumber(@Param("policeNumber") String policeNumber);

    @Select("SELECT * FROM police WHERE user_uuid = #{userUuid} AND is_deleted = 0")
    Police selectByUserUuid(@Param("userUuid") String userUuid);

    @Select("<script>" +
        "SELECT p.*, r.name AS resident_name FROM police p " +
        "LEFT JOIN resident r ON p.resident_uuid = r.uuid AND r.is_deleted = 0 " +
        "WHERE p.is_deleted = 0 " +
        "<if test='keyword != null and keyword != \"\"'>" +
        "AND (p.police_number LIKE CONCAT('%',#{keyword},'%') " +
        "OR p.police_station LIKE CONCAT('%',#{keyword},'%') " +
        "OR p.department LIKE CONCAT('%',#{keyword},'%'))" +
        "</if>" +
        "<if test='residentUuid != null and residentUuid != \"\"'>" +
        "AND p.resident_uuid = #{residentUuid}" +
        "</if>" +
        "ORDER BY p.create_time DESC" +
        "</script>")
    Page<Police> selectPageWithResidentName(Page<Police> page, @Param("keyword") String keyword,
                                           @Param("residentUuid") String residentUuid);
}
