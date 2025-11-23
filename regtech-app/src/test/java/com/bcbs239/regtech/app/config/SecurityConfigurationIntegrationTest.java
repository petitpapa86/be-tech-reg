package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import com.bcbs239.regtech.iam.infrastructure.security.SecurityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to verify security configuration across all modules.
 * Tests that SecurityFilter loads public paths from configuration,
 * public paths are accessible without authentication, and protected paths
 * require authentication.
 * 
 * Requirements: 11.2, 11.3, 11.4, 12.2, 12.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("development")
class SecurityConfigurationIntegrationTest {

    @Autowired
    private IAMProperties iamProperties;

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void securityFilterLoadsPublicPathsFromConfiguration() {
        assertNotNull(iamProperties, "IAMProperties should be loaded");
        assertNotNull(iamProperties.getSecurity(), "Security configuration should exist");
        
        List<String> publicPaths = iamProperties.getSecurity().getPublicPaths();
        assertNotNull(publicPaths, "Public paths should be loaded");
        assertFalse(publicPaths.isEmpty(), "Public paths should not be empty");
        
        // Verify essential public paths are present
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("/health")), 
            "Health endpoints should be public");
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("/auth/")), 
            "Authentication endpoints should be public");
    }

    @Test
    void securityFilterIsInitialized() {
        assertNotNull(securityFilter, "SecurityFilter should be initialized");
    }

    @Test
    void publicPathsIncludeAllModuleHealthEndpoints() {
        List<String> publicPaths = iamProperties.getSecurity().getPublicPaths();
        
        // Verify all module health endpoints are public
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("ingestion") && p.contains("health")), 
            "Ingestion health endpoint should be public");
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("data-quality") && p.contains("health")), 
            "Data quality health endpoint should be public");
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("risk-calculation") && p.contains("health")), 
            "Risk calculation health endpoint should be public");
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("report-generation") && p.contains("health")), 
            "Report generation health endpoint should be public");
    }

    @Test
    void publicPathsIncludeAuthenticationEndpoints() {
        List<String> publicPaths = iamProperties.getSecurity().getPublicPaths();
        
        // Verify authentication endpoints are public
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("/auth/login")), 
            "Login endpoint should be public");
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("/auth/register") || 
                                                      p.contains("/users/register")), 
            "Registration endpoint should be public");
    }

    @Test
    void publicPathsIncludeActuatorEndpoints() {
        List<String> publicPaths = iamProperties.getSecurity().getPublicPaths();
        
        // Verify actuator health endpoint is public
        assertTrue(publicPaths.stream().anyMatch(p -> p.contains("/actuator/health")), 
            "Actuator health endpoint should be public");
    }

    @Test
    void publicPathsAreAccessibleWithoutAuthentication() throws Exception {
        // Test that actuator health endpoint is accessible without authentication
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void protectedPathsRequireAuthentication() throws Exception {
        // Test that a protected endpoint returns 401 without authentication
        // Note: This assumes there's at least one protected endpoint
        // If all endpoints are public in development, this test may need adjustment
        
        // Try to access a typical protected endpoint
        mockMvc.perform(get("/api/v1/protected-resource"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publicPathConfigurationIsValid() {
        List<String> publicPaths = iamProperties.getSecurity().getPublicPaths();
        
        // Verify all paths start with /
        for (String path : publicPaths) {
            assertTrue(path.startsWith("/"), 
                String.format("Public path '%s' should start with '/'", path));
        }
        
        // Verify wildcard patterns are valid
        for (String path : publicPaths) {
            if (path.contains("*")) {
                assertTrue(path.endsWith("/**") || path.endsWith("/*"), 
                    String.format("Public path '%s' has invalid wildcard pattern", path));
            }
        }
    }

    @Test
    void jwtConfigurationIsLoaded() {
        assertNotNull(iamProperties.getSecurity().getJwt(), "JWT configuration should exist");
        assertNotNull(iamProperties.getSecurity().getJwt().getSecret(), 
            "JWT secret should be configured");
        assertTrue(iamProperties.getSecurity().getJwt().getExpiration() > 0, 
            "JWT expiration should be positive");
    }

    @Test
    void passwordPolicyConfigurationIsLoaded() {
        var passwordConfig = iamProperties.getSecurity().getPassword();
        assertNotNull(passwordConfig, "Password configuration should exist");
        assertTrue(passwordConfig.getMinLength() >= 8, 
            "Password minimum length should be at least 8");
    }

    @Test
    void authorizationConfigurationIsLoaded() {
        assertNotNull(iamProperties.getAuthorization(), "Authorization configuration should exist");
        assertNotNull(iamProperties.getAuthorization().getCache(), 
            "Authorization cache configuration should exist");
        assertNotNull(iamProperties.getAuthorization().getMultiTenant(), 
            "Multi-tenant configuration should exist");
        assertNotNull(iamProperties.getAuthorization().getPermissions(), 
            "Permissions configuration should exist");
    }

    /**
     * Test RouterAttributes permissions enforcement
     * This is a conceptual test - actual implementation depends on route definitions
     */
    @Test
    void routerAttributesConfigurationIsConsistent() {
        // Verify that security configuration supports RouterAttributes pattern
        // The actual enforcement is tested in module-specific integration tests
        
        assertNotNull(iamProperties.getSecurity().getPublicPaths(), 
            "Public paths configuration supports RouterAttributes pattern");
        assertNotNull(iamProperties.getAuthorization().getPermissions(), 
            "Permissions configuration supports RouterAttributes pattern");
        
        assertTrue(iamProperties.getAuthorization().getPermissions().isStrictMode(), 
            "Strict mode should be enabled for permission checks");
        assertTrue(iamProperties.getAuthorization().getPermissions().isAuditEnabled(), 
            "Audit should be enabled for permission checks");
    }
}
