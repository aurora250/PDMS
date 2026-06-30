package com.pdm.household.mapper;

import com.pdm.household.entity.HouseholdRegister;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HouseholdRegisterMapper extends BaseMapper<HouseholdRegister> {
    @Select("SELECT * FROM household_register WHERE household_book_no = #{no} AND is_deleted = 0")
    HouseholdRegister selectByBookNo(@Param("no") String bookNo);

    /** 按居民UUID查询其所属户口簿（通过 member_uuid_list 模糊匹配） */
    @Select("SELECT * FROM household_register WHERE member_uuid_list LIKE CONCAT('%', #{residentUuid}, '%') AND is_deleted = 0")
    HouseholdRegister selectByResidentUuid(@Param("residentUuid") String residentUuid);
}
