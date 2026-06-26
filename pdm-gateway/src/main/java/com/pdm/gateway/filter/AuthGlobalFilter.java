package com.pdm.gateway.filter;

import com.pdm.common.core.result.Result;
import com.pdm.common.security.JwtTokenProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 全局认证过滤器。
 *
 * <p>拦截所有经过网关的请求，对白名单以外的路径进行 JWT 令牌校验。 校验通过后从令牌中提取用户信息（UUID、用户名、角色）， 通过 {@code X-User-Uuid}、{@code
 * X-Username}、{@code X-User-Role} 请求头传递给下游微服务。 校验失败返回 401 未授权响应。
 *
 * <p>白名单路径：{@code /api/auth/login}、{@code /api/auth/register}、{@code /api/auth/refresh}。 过滤器优先级为
 * -100（高优先级），确保在路由之前执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** JWT 令牌提供器，用于校验和解析令牌 */
    private final JwtTokenProvider jwtTokenProvider;

    /** Jackson 对象映射器，用于序列化错误响应 */
    private final ObjectMapper objectMapper;

    /** Ant 路径匹配器，用于白名单路径匹配 */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 认证白名单，包含登录、注册和令牌刷新接口 */
    private static final List<String> WHITELIST =
            List.of("/api/auth/login", "/api/auth/register", "/api/auth/refresh");

    /**
     * 全局过滤逻辑。
     *
     * <p>白名单路径直接放行；其他路径须携带有效的 Bearer Token， 校验通过后将用户信息写入请求头传递给下游服务。
     *
     * @param exchange 当前请求-响应交换对象
     * @param chain 过滤器链
     * @return Mono&lt;Void&gt; 表示过滤完成
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        if (WHITELIST.stream().anyMatch(p -> PATH_MATCHER.match(p, path))) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return unauthorized(exchange, "未登录或Token已过期");
        }

        // 将用户信息传递给下游服务
        String userUuid = jwtTokenProvider.getUserUuid(token);
        String username = jwtTokenProvider.getUsername(token);
        String role = jwtTokenProvider.getRole(token);

        ServerHttpRequest modifiedRequest =
                exchange.getRequest()
                        .mutate()
                        .header("X-User-Uuid", userUuid)
                        .header("X-Username", username)
                        .header("X-User-Role", role)
                        .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * 从请求头中提取 Bearer Token。
     *
     * @param request 服务端 HTTP 请求
     * @return JWT 令牌字符串，不存在或格式不正确时返回 {@code null}
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 返回 401 未授权 JSON 响应。
     *
     * @param exchange 当前请求-响应交换对象
     * @param message 错误提示信息
     * @return Mono&lt;Void&gt; 表示响应写入完成
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.fail(2000, message);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(result);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    /**
     * 获取过滤器执行顺序。
     *
     * @return 顺序值，-100 表示高优先级，确保在路由之前执行认证
     */
    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
