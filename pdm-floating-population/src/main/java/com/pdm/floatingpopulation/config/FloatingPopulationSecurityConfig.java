package com.pdm.floatingpopulation.config;

import com.pdm.common.security.JwtAuthenticationFilter;
import com.pdm.common.security.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class FloatingPopulationSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/fp/statistics/**")
                        .hasAnyAuthority("fp:read", "*").requestMatchers(HttpMethod.GET, "/api/fp/**")
                        .hasAnyAuthority("fp:read", "*").requestMatchers(HttpMethod.POST, "/api/fp/**")
                        .hasAnyAuthority("fp:write", "*").requestMatchers(HttpMethod.PUT, "/api/fp/**")
                        .hasAnyAuthority("fp:write", "*").requestMatchers(HttpMethod.DELETE, "/api/fp/**")
                        .hasAnyAuthority("fp:delete", "*").anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
