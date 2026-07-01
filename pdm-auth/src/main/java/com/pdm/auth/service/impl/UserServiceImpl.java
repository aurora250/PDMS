package com.pdm.auth.service.impl;

import com.pdm.auth.entity.User;
import com.pdm.auth.mapper.UserMapper;
import com.pdm.auth.service.UserService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> USER_ADMIN_VISIBLE_ROLES = List.of("普通用户", "采集员", "街道办");

    @Override
    public Page<User> listUsers(int page, int size, String keyword, String role, String status, String callerRole) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().eq(User::getUserUuid, keyword));
        }
        // 用户管理员只能看到其管辖范围内的角色
        if ("用户管理员".equals(callerRole)) {
            wrapper.in(User::getUserRole, USER_ADMIN_VISIBLE_ROLES);
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

    @Override
    public User getUserByUuid(String userUuid) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

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

    @Override
    @Transactional
    public void updateUserStatus(String userUuid, String newStatus) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        // Validate status transition
        if (!List.of("审批中", "有效", "冻结", "注销", "锁定").contains(newStatus)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号状态: " + newStatus);
        }
        user.setAccountStatus(newStatus);
        if ("锁定".equals(newStatus)) {
            user.setLockedUntil(LocalDateTime.now().plusHours(24));
        }
        userMapper.updateById(user);
    }

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

    @Override
    @Transactional
    public void resetPassword(String userUuid, String newPassword) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 16) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userMapper.updateById(user);
    }
}
