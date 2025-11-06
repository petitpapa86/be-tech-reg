package com.bcbs239.regtech.iam.infrastructure.security;


import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityConfigurationRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * IAM module security configuration.
 * Handles authentication and authorization for user management endpoints.
 */
@Configuration
public class IamSecurityConfiguration implements SecurityConfigurationRegistry.ModuleSecurityConfiguration {

    @Autowired
    private SecurityConfigurationRegistry securityConfigurationRegistry;

    @PostConstruct
    public void registerSecurityConfiguration() {
        securityConfigurationRegistry.registerModuleSecurityConfiguration("iam", this);
    }

    @Override
    public String[] getPathPatterns() {
        return new String[]{"/api/v1/users/**", "/api/v1/auth/**"};
    }

    @Override
    public int getOrder() {
        return 1; // Highest priority for IAM endpoints
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for REST API endpoints FIRST
            .authorizeHttpRequests(authz -> authz
                // Public registration and authentication endpoints
                .requestMatchers("/api/v1/users/register").permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/refresh").permitAll()

                // Health endpoints - public
                .requestMatchers("/api/v1/users/health/**").permitAll()
                .requestMatchers("/api/v1/auth/health/**").permitAll()

                // User profile management - authenticated users
                .requestMatchers("/api/v1/users/profile/**").authenticated()

                // User administration - admin role required
                .requestMatchers("/api/v1/users/admin/**").hasRole("ADMIN")

                // All other IAM endpoints require authentication
                .anyRequest().authenticated()
            );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

