package com.pdm.auth.service.impl;

import com.pdm.auth.entity.User;
import com.pdm.auth.mapper.UserMapper;
import com.pdm.auth.service.UserService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 用户管理服务实现类，提供用户的查询、更新、状态管理和删除功能。
 *
 * <ul>
 *   <li><b>分页查询</b> —— 支持按用户名/UUID 关键词搜索，按角色和状态精确筛选。
 *   <li><b>按 UUID 查询</b> —— 根据用户唯一标识精确查找。
 *   <li><b>更新用户信息</b> —— 支持部分字段更新（手机、邮箱、角色、权限组）。
 *   <li><b>更新账号状态</b> —— 修改账号状态（审批中/有效/冻结/注销/锁定），锁定时自动设置 24 小时锁定。
 *   <li><b>删除用户</b> —— 仅允许删除非"有效"状态的用户，活跃用户需先停用。
 * </ul>
 *
 * @author freedom
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /**
     * 分页查询用户列表。
     *
     * <p>支持以下筛选条件（均为可选）：
     *
     * <ul>
     *   <li>{@code keyword} —— 模糊匹配用户名和用户 UUID（OR 逻辑）
     *   <li>{@code role} —— 精确匹配用户角色
     *   <li>{@code status} —— 精确匹配账号状态
     * </ul>
     *
     * 结果按创建时间倒序排列。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @param keyword 搜索关键词（可选，匹配用户名或 UUID）
     * @param role 角色筛选（可选）
     * @param status 状态筛选（可选）
     * @return 分页结果，包含用户列表及分页信息
     */
    @Override
    public Page<User> listUsers(int page, int size, String keyword, String role, String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(
                    w -> w.like(User::getUsername, keyword).or().like(User::getUserUuid, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getUserRole, role);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getAccountStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(Page.of(page, size), wrapper);
    }

    /**
     * 根据用户 UUID 查询用户。
     *
     * @param userUuid 用户唯一标识
     * @return 用户实体
     * @throws BusinessException 用户不存在时抛出
     */
    @Override
    public User getUserByUuid(String userUuid) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 更新用户信息。
     *
     * <p>根据 UUID 定位用户，仅更新传入对象中非空的字段：
     *
     * <ul>
     *   <li>手机号（{@code phone}）
     *   <li>邮箱（{@code email}）
     *   <li>角色（{@code userRole}）
     *   <li>权限组 ID（{@code permissionGroupId}）
     * </ul>
     *
     * @param userUuid 用户唯一标识
     * @param updates 包含待更新字段的用户对象（仅非空字段生效）
     * @return 更新后的用户实体
     * @throws BusinessException 用户不存在时抛出
     */
    @Override
    @Transactional
    public User updateUser(String userUuid, User updates) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(updates.getPhone())) {
            user.setPhone(updates.getPhone());
        }
        if (StringUtils.hasText(updates.getEmail())) {
            user.setEmail(updates.getEmail());
        }
        if (StringUtils.hasText(updates.getUserRole())) {
            user.setUserRole(updates.getUserRole());
        }
        if (updates.getPermissionGroupId() != null) {
            user.setPermissionGroupId(updates.getPermissionGroupId());
        }
        userMapper.updateById(user);
        return user;
    }

    /**
     * 更新用户账号状态。
     *
     * <p>仅允许以下五种状态值：
     *
     * <ul>
     *   <li>{@code "审批中"} —— 账号待审批
     *   <li>{@code "有效"} —— 账号正常可用
     *   <li>{@code "冻结"} —— 账号已冻结
     *   <li>{@code "注销"} —— 账号已注销
     *   <li>{@code "锁定"} —— 账号锁定（自动设置 24 小时后解锁）
     * </ul>
     *
     * @param userUuid 用户唯一标识
     * @param newStatus 新状态（必须为上述五种之一）
     * @throws BusinessException 用户不存在或状态值无效时抛出
     */
    @Override
    @Transactional
    public void updateUserStatus(String userUuid, String newStatus) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (!List.of("审批中", "有效", "冻结", "注销", "锁定").contains(newStatus)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号状态: " + newStatus);
        }
        user.setAccountStatus(newStatus);
        if ("锁定".equals(newStatus)) {
            user.setLockedUntil(LocalDateTime.now().plusHours(24));
        }
        userMapper.updateById(user);
    }

    /**
     * 删除用户。
     *
     * <p>仅允许删除非"有效"状态的用户，活跃用户需先调用 {@link #updateUserStatus(String, String)} 停用后再删除。
     *
     * @param userUuid 用户唯一标识
     * @throws BusinessException 用户不存在或账号仍为"有效"状态时抛出
     */
    @Override
    @Transactional
    public void deleteUser(String userUuid) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        if ("有效".equals(user.getAccountStatus())) {
            throw new BusinessException("活跃用户需先停用账号再删除");
        }
        userMapper.deleteById(user.getId());
    }
}
