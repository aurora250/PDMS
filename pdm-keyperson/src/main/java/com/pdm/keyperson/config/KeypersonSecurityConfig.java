package com.pdm.keyperson.config;

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
public class KeypersonSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/keyperson/gis")
                        .hasAnyAuthority("keyperson:gis:read", "*").requestMatchers(HttpMethod.GET, "/api/keyperson/**")
                        .hasAnyAuthority("keyperson:read", "*").requestMatchers(HttpMethod.POST, "/api/keyperson/**")
                        .hasAnyAuthority("keyperson:write", "*").requestMatchers(HttpMethod.PUT, "/api/keyperson/**")
                        .hasAnyAuthority("keyperson:write", "*").requestMatchers(HttpMethod.DELETE, "/api/keyperson/**")
                        .hasAnyAuthority("keyperson:delete", "*").anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
