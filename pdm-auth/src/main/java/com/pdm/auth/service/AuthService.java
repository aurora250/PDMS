package com.pdm.auth.service;

import com.pdm.common.dto.LoginRequest;
import com.pdm.common.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String ipAddress);

    void logout(String token, String userUuid);

    LoginResponse refreshToken(String refreshToken);

    void changePassword(String userUuid, String oldPassword, String newPassword);

    void registerUser(String userUuid, String username, String rawPassword, String phone, String residentUuid);
}
