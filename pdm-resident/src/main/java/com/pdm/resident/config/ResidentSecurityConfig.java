package com.pdm.resident.config;

import com.pdm.common.security.JwtAuthenticationFilter;
import com.pdm.common.security.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ResidentSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/resident/search")
                        .hasAnyAuthority("resident:read", "*").requestMatchers(HttpMethod.POST, "/api/resident")
                        .hasAnyAuthority("resident:write", "*").requestMatchers(HttpMethod.PUT, "/api/resident/**")
                        .hasAnyAuthority("resident:write", "*").requestMatchers(HttpMethod.DELETE, "/api/resident/**")
                        .hasAnyAuthority("resident:delete", "*")
                        .requestMatchers("/api/resident/import", "/api/resident/export")
                        .hasAnyAuthority("resident:import", "resident:export", "*")
                        .requestMatchers(HttpMethod.GET, "/api/resident/change-request")
                        .hasAnyAuthority("resident:read", "*")
                        .requestMatchers(HttpMethod.POST, "/api/resident/change-request")
                        .hasAnyAuthority("resident:write", "*")
                        .requestMatchers(HttpMethod.PUT, "/api/resident/change-request/**")
                        .hasAnyAuthority("resident:change-request:approve", "*").anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
