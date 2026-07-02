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

    /** 按居民UUID查询其所属户口簿（户主或成员匹配，优先返回有效/审批中的，最新的排在前面） */
    @Select("SELECT * FROM household_register WHERE (householder_uuid = #{residentUuid} OR member_uuid_list LIKE CONCAT('%', #{residentUuid}, '%')) AND is_deleted = 0 AND status IN ('有效','审批中') ORDER BY create_time DESC LIMIT 1")
    HouseholdRegister selectByResidentUuid(@Param("residentUuid") String residentUuid);

    /** 按前缀查询最大户口簿号（用于自增序号） */
    @Select("SELECT MAX(household_book_no) FROM household_register WHERE household_book_no LIKE #{prefix}")
    String selectMaxBookNoByPrefix(@Param("prefix") String prefix);
}
