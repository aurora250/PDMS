package com.pdm.floatingpopulation.mapper;

import com.pdm.floatingpopulation.entity.ResidentRegistration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResidentRegistrationMapper extends BaseMapper<ResidentRegistration> {

    /** 查询某居民最近一次居住地登记的日期 */
    @Select("SELECT register_date FROM resident_registration WHERE uuid = #{uuid} AND is_deleted = 0"
            + " ORDER BY register_date DESC LIMIT 1")
    LocalDate selectLatestRegisterDate(@Param("uuid") String uuid);
}
