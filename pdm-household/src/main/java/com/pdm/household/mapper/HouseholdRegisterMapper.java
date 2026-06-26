package com.pdm.household.mapper;

import com.pdm.household.entity.HouseholdRegister;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 户口本登记 Mapper 接口。
 *
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，提供户口本登记表的基础 CRUD 操作， 并扩展了按户口本编号查询的方法。
 * </p>
 */
@Mapper
public interface HouseholdRegisterMapper extends BaseMapper<HouseholdRegister> {

    /**
     * 根据户口本编号查询户口本登记记录。
     *
     * @param bookNo
     *            户口本编号
     * @return 户口本登记实体，不存在时返回 {@code null}
     */
    @Select("SELECT * FROM household_register WHERE household_book_no = #{no} AND is_deleted = 0")
    HouseholdRegister selectByBookNo(@Param("no") String bookNo);
}
