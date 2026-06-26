package com.pdm.keyperson.mapper;

import com.pdm.keyperson.entity.KeyPerson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 重点人员Mapper接口
 * 基于MyBatis-Plus实现重点人员表（key_person）的CRUD操作，扩展自定义查询方法
 */
@Mapper
public interface KeyPersonMapper extends BaseMapper<KeyPerson> {

    /**
     * 根据UUID查询未删除的重点人员
     *
     * @param uuid 重点人员唯一标识
     * @return 符合条件的重点人员实体（null表示不存在或已删除）
     */
    @Select("SELECT * FROM key_person WHERE uuid = #{uuid} AND is_deleted = 0")
    KeyPerson selectByUuid(@Param("uuid") String uuid);
}