package com.pdm.auth.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册请求 DTO。
 *
 * <p>
 * 包含用户注册所需的全部字段，各字段均带 Jakarta Validation 校验注解。
 * </p>
 */
@Data
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 UUID（可选，为空时系统自动生成） */
    private String userUuid;

    /** 用户名，3-50 位字母、数字或下划线 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "用户名须为3-50位字母数字下划线")
    private String username;

    /** 明文密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 手机号，须符合中国大陆手机号格式 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 关联的实名认证居民 UUID */
    @NotBlank(message = "实名认证UUID不能为空")
    private String residentUuid;
}
