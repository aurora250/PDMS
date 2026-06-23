package com.pdm.resident.mapper;

import com.pdm.resident.entity.Resident;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResidentMapper extends BaseMapper<Resident> {

    @Select("SELECT * FROM resident WHERE uuid = #{uuid} AND is_deleted = 0")
    Resident selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM resident WHERE id_card_no = #{idCardNo} AND is_deleted = 0")
    Resident selectByIdCardNo(@Param("idCardNo") String idCardNo);
}
