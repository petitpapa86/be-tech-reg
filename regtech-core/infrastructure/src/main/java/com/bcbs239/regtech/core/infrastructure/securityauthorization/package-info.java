/**
 * Security & Authorization Infrastructure Capability
 *
 * This capability provides the infrastructure foundation for authentication,
 * authorization, and security policy enforcement across the regtech-core module.
 *
 * Key responsibilities:
 * - Security context management with Java 21 Scoped Values
 * - Permission-based access control
 * - Security policy enforcement
 * - Authentication result handling
 * - WebFlux functional routing security
 *
 * Components:
 * - SecurityContextHolder: Scoped Values-based security context management
 * - SecurityContext: Backward-compatible security utility
 * - DefaultSecurityContext: Security context implementation
 * - AnonymousAuthentication: Public endpoint authentication
 * - SimpleAuthentication: Basic authentication implementation
 * - PermissionCheckFilter: WebFlux authorization filter
 * - RouterFilterConfig: Route filtering configuration
 * - SecurityUtils: Security utility functions
 */
package com.bcbs239.regtech.core.infrastructure.securityauthorization;

