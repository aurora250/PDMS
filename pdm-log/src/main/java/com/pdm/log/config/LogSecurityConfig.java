package com.pdm.log.config;

import com.pdm.common.security.JwtAuthenticationFilter;
import com.pdm.common.security.JwtTokenProvider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class LogSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    @ConditionalOnMissingBean(name = "securityFilterChain")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/log/audit")
                        .hasAnyAuthority("log:audit:read", "*").requestMatchers("/api/log/login")
                        .hasAnyAuthority("log:login:read", "*").requestMatchers("/api/log/export")
                        .hasAnyAuthority("log:export", "*").anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
