package com.bcbs239.regtech.iam.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * IAM (Identity and Access Management) Module Configuration
 *
 * This module provides comprehensive identity management capabilities including:
 * - User registration and authentication
 * - Bank selection and association
 * - User management and profile operations
 * - OAuth2 integration for external authentication
 * - Security and authorization services
 *
 * The module is organized using Domain-Driven Design principles with
 * business capabilities as the primary organizational structure.
 * 
 * Note: Entity scanning and JPA repository configuration is handled by
 * ModularJpaConfiguration in regtech-core-infrastructure to avoid conflicts.
 */
@Configuration
@ComponentScan(basePackages = "com.bcbs239.regtech.iam")
public class IamModule {

    // IAM-specific beans only
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public JwtTokenProvider jwtTokenProvider(@Value("${jwt.secret}") String secret) {
//        return new JwtTokenProvider(secret);
//    }
}

