package com.pdm.auth.mapper;

import com.pdm.auth.entity.User;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD 能力， 并定义按用户名和用户 UUID
 * 查询以及用户名计数的方法，查询时自动过滤已逻辑删除的记录。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（排除已删除记录）。
     *
     * @param username 用户名
     * @return 用户实体，未找到则返回 {@code null}
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据用户 UUID 查询用户（排除已删除记录）。
     *
     * @param userUuid 用户唯一标识
     * @return 用户实体，未找到则返回 {@code null}
     */
    @Select("SELECT * FROM sys_user WHERE user_uuid = #{userUuid} AND is_deleted = 0")
    User selectByUserUuid(@Param("userUuid") String userUuid);

    /**
     * 统计指定用户名的用户数量（排除已删除记录），用于注册时检查用户名唯一性。
     *
     * @param username 用户名
     * @return 匹配该用户名的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    int countByUsername(@Param("username") String username);
}
