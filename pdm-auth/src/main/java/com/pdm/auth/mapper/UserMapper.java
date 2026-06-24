package com.pdm.auth.mapper;

import com.pdm.auth.entity.User;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE user_uuid = #{userUuid} AND is_deleted = 0")
    User selectByUserUuid(@Param("userUuid") String userUuid);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    int countByUsername(@Param("username") String username);
}
