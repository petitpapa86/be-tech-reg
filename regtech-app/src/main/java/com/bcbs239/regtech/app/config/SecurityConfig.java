package com.bcbs239.regtech.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the application.
 * Note: Authentication and authorization are fully handled by custom SecurityFilter in regtech-iam module.
 * This configuration disables Spring Security's default authentication to avoid conflicts.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()  // All authentication is delegated to custom SecurityFilter
            )
            .csrf(AbstractHttpConfigurer::disable)  // CSRF disabled - API uses JWT tokens
            .httpBasic(AbstractHttpConfigurer::disable)  // Disable HTTP Basic
            .formLogin(AbstractHttpConfigurer::disable)  // Disable form login
            .logout(AbstractHttpConfigurer::disable);  // Disable default logout

        return http.build();
    }
}