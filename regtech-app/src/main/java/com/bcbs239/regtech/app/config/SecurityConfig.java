package com.bcbs239.regtech.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the application.
 * Note: Authentication and authorization are fully handled by custom SecurityFilter in regtech-iam module.
 * This configuration disables Spring Security's default authentication to avoid conflicts.
 * 
 * Spring Framework 7 CORS Integration:
 * - CORS configuration is explicitly provided via CorsConfigurationSource
 * - Pre-flight OPTIONS requests are handled according to Spring Framework 7 behavior
 * - Empty CORS configuration no longer rejects requests (but we provide explicit config)
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()  // All authentication is delegated to custom SecurityFilter
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource))  // Enable CORS with explicit configuration (Requirement 12.1, 12.2)
            .csrf(AbstractHttpConfigurer::disable)  // CSRF disabled - API uses JWT tokens
            .httpBasic(AbstractHttpConfigurer::disable)  // Disable HTTP Basic
            .formLogin(AbstractHttpConfigurer::disable)  // Disable form login
            .logout(AbstractHttpConfigurer::disable);  // Disable default logout

        return http.build();
    }
}