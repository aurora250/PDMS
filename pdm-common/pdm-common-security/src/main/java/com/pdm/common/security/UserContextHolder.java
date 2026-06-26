package com.pdm.common.security;

/**
 * 当前登录用户上下文持有者。
 *
 * <p>基于 {@link ThreadLocal} 实现，在每个请求处理线程中保存当前登录用户的基本信息。 由 {@link JwtAuthenticationFilter}
 * 在请求进入时设置，在请求结束时通过 {@code finally} 块自动清理， 避免内存泄漏和跨请求数据污染。
 *
 * <p>工具类不可实例化。
 */
public final class UserContextHolder {

    /** 线程级别的用户上下文存储 */
    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    /**
     * 设置当前线程的用户上下文。
     *
     * @param context 用户上下文
     */
    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程的完整用户上下文。
     *
     * @return 用户上下文，未设置时返回 {@code null}
     */
    public static UserContext get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前用户的 UUID。
     *
     * @return 用户 UUID，未登录时返回 {@code null}
     */
    public static String getUserUuid() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.userUuid() : null;
    }

    /**
     * 获取当前用户的用户名。
     *
     * @return 用户名，未登录时返回 {@code null}
     */
    public static String getUsername() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.username() : null;
    }

    /**
     * 获取当前用户的角色。
     *
     * @return 用户角色，未登录时返回 {@code null}
     */
    public static String getRole() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.role() : null;
    }

    /** 清除当前线程的用户上下文，防止内存泄漏。 */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 用户上下文记录，包含当前用户的基本信息和请求 IP。
     *
     * @param userUuid 用户 UUID
     * @param username 用户名
     * @param role 用户角色
     * @param ipAddress 客户端 IP 地址
     */
    public record UserContext(String userUuid, String username, String role, String ipAddress) {}
}
