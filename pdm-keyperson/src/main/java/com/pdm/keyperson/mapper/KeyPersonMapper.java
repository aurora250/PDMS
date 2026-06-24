package com.pdm.keyperson.mapper;

import com.pdm.keyperson.entity.KeyPerson;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KeyPersonMapper extends BaseMapper<KeyPerson> {

    @Select("SELECT * FROM key_person WHERE uuid = #{uuid} AND is_deleted = 0")
    KeyPerson selectByUuid(@Param("uuid") String uuid);
}
