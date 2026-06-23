package com.pdm.auth.service;

import com.pdm.auth.entity.User;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserService {

    Page<User> listUsers(int page, int size, String keyword, String role, String status);

    User getUserByUuid(String userUuid);

    User updateUser(String userUuid, User updates);

    void updateUserStatus(String userUuid, String newStatus);

    void deleteUser(String userUuid);
}
