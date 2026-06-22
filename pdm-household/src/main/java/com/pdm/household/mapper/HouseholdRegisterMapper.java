package com.pdm.household.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdm.household.entity.HouseholdRegister;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HouseholdRegisterMapper extends BaseMapper<HouseholdRegister> {
    @Select("SELECT * FROM household_register WHERE household_book_no = #{no} AND is_deleted = 0")
    HouseholdRegister selectByBookNo(@Param("no") String bookNo);
}
