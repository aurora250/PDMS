package com.pdm.auth.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO。
 *
 * <p>
 * 包含旧密码验证和新密码设置所需的字段，各字段均带 Jakarta Validation 校验注解。
 * </p>
 */
@Data
public class ChangePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 旧密码，用于身份验证，不能为空 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码，不能为空且长度需在 8-16 位之间 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 16, message = "密码长度需8-16位")
    private String newPassword;
}
