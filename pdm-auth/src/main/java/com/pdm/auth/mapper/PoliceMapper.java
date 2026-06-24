package com.pdm.auth.mapper;

import com.pdm.auth.entity.Police;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PoliceMapper extends BaseMapper<Police> {

    @Select("SELECT * FROM police WHERE police_number = #{policeNumber} AND is_deleted = 0")
    Police selectByPoliceNumber(@Param("policeNumber") String policeNumber);

    @Select("SELECT * FROM police WHERE user_uuid = #{userUuid} AND is_deleted = 0")
    Police selectByUserUuid(@Param("userUuid") String userUuid);
}
