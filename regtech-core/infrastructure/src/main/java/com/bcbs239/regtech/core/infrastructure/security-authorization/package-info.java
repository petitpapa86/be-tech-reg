/**
 * Security & Authorization Infrastructure Capability
 *
 * This capability provides the infrastructure foundation for authentication,
 * authorization, and security policy enforcement across the regtech-core module.
 *
 * Key responsibilities:
 * - User authentication and authorization
 * - Permission-based access control
 * - Security policy enforcement
 * - JWT token processing and validation
 * - Security context management
 *
 * Components:
 * - PermissionService, JwtPermissionService: Permission checking services
 * - PermissionAuthorizationFilter: Authorization filter for web requests
 * - RequirePermissions: Permission annotation for method-level security
 * - SecurityConfiguration, ModularSecurityConfiguration, SharedSecurityConfiguration: Security configuration
 * - SecurityConfigurationRegistry, SecurityConfigurationRegistryImpl: Security config management
 * - SecurityContext, SecurityUtils: Security utilities and context
 * - authorization/ directory: Authorization-related utilities and helpers
 */
package com.bcbs239.regtech.core.infrastructure.securityauthorization;