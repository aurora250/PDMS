package com.pdm.resident.mapper;

import com.pdm.resident.entity.Resident;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResidentMapper extends BaseMapper<Resident> {

    @Select("SELECT * FROM resident WHERE uuid = #{uuid} AND is_deleted = 0")
    Resident selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM resident WHERE id_card_no = #{idCardNo} AND is_deleted = 0")
    Resident selectByIdCardNo(@Param("idCardNo") String idCardNo);

    /** 批量查询姓名 */
    @Select("<script>" + "SELECT uuid, name FROM resident WHERE uuid IN "
            + "<foreach collection='uuids' item='uuid' open='(' separator=',' close=')'>#{uuid}</foreach>"
            + " AND is_deleted = 0" + "</script>")
    List<Map<String, Object>> batchGetNames(@Param("uuids") List<String> uuids);
}
