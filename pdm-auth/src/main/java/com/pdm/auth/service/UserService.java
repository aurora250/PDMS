package com.pdm.auth.service;

import com.pdm.auth.entity.User;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户管理服务接口。
 *
 * <p>定义用户管理相关的业务操作，包括分页查询、按 UUID 查询、信息更新、状态管理和删除。
 */
public interface UserService {

    /**
     * 分页查询用户列表，支持关键词、角色和状态筛选。
     *
     * @param page 页码
     * @param size 每页条数
     * @param keyword 搜索关键词（可选）
     * @param role 角色筛选（可选）
     * @param status 状态筛选（可选）
     * @return 用户分页结果
     */
    Page<User> listUsers(int page, int size, String keyword, String role, String status);

    /**
     * 根据用户 UUID 查询用户。
     *
     * @param userUuid 用户唯一标识
     * @return 用户实体
     */
    User getUserByUuid(String userUuid);

    /**
     * 更新用户信息。
     *
     * @param userUuid 用户唯一标识
     * @param updates 包含待更新字段的用户对象
     * @return 更新后的用户实体
     */
    User updateUser(String userUuid, User updates);

    /**
     * 更新用户账号状态。
     *
     * @param userUuid 用户唯一标识
     * @param newStatus 新状态
     */
    void updateUserStatus(String userUuid, String newStatus);

    /**
     * 删除用户。
     *
     * @param userUuid 用户唯一标识
     */
    void deleteUser(String userUuid);
}
