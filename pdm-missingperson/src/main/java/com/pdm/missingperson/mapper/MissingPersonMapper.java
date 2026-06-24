package com.pdm.missingperson.mapper;

import com.pdm.missingperson.entity.MissingPerson;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MissingPersonMapper extends BaseMapper<MissingPerson> {

    @Select("SELECT * FROM missing_person WHERE resident_uuid = #{residentUuid} AND is_deleted = 0")
    MissingPerson selectByResidentUuid(@Param("residentUuid") String residentUuid);
}
