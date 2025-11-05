package com.bcbs239.regtech.iam;

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
 */
@Configuration
@ComponentScan(basePackages = "com.bcbs239.regtech.iam")
public class IamModule {
}

