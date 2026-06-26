package com.pdm.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO。
 *
 * <p>
 * 登录成功后返回给客户端的 JWT 令牌及用户基本信息。 使用 Builder 模式构造，支持链式调用。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT 访问令牌 */
    private String accessToken;
    /** JWT 刷新令牌 */
    private String refreshToken;
    /** 令牌类型，默认为 Bearer */
    private String tokenType = "Bearer";
    /** 令牌过期时间（秒） */
    private long expiresIn;
    /** 用户 UUID */
    private String userUuid;
    /** 用户名 */
    private String username;
    /** 用户角色 */
    private String role;
    /** 是否强制在下次登录时修改密码 */
    private boolean mustChangePassword;
}
