package com.pdm.common.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO。
 *
 * <p>
 * 包含用户名和密码两个必填字段，用于身份认证。
 * </p>
 */
@Data
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
