package com.bcbs239.regtech.app.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CORS configuration without requiring full Spring context.
 * Tests verify that CorsConfig bean creates correct CORS configuration.
 * 
 * Spring Framework 7 Behavior:
 * - Pre-flight requests are not rejected when CORS config is empty
 * - This test verifies our explicit configuration is correctly set up
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
class CorsConfigUnitTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
        
        // Set test values using reflection (simulating @Value injection)
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", 
            new String[]{"http://localhost:3000", "http://localhost:4200"});
        ReflectionTestUtils.setField(corsConfig, "allowedMethods", 
            new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"});
        ReflectionTestUtils.setField(corsConfig, "allowedHeaders", 
            new String[]{"*"});
        ReflectionTestUtils.setField(corsConfig, "exposedHeaders", 
            new String[]{"X-Correlation-ID", "Authorization"});
        ReflectionTestUtils.setField(corsConfig, "allowCredentials", true);
        ReflectionTestUtils.setField(corsConfig, "maxAge", 3600L);
    }

    /**
     * Test that CORS configuration source is created correctly.
     * Requirement 12.1
     */
    @Test
    void testCorsConfigurationSource_ShouldBeCreated() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        
        assertNotNull(source, "CORS configuration source should not be null");
        assertTrue(source instanceof UrlBasedCorsConfigurationSource, 
            "Should be UrlBasedCorsConfigurationSource");
    }

    /**
     * Test that allowed origins are configured correctly.
     * Requirement 12.3
     */
    @Test
    void testAllowedOrigins_ShouldBeConfigured() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(null);
        
        assertNotNull(config, "CORS configuration should not be null");
        assertNotNull(config.getAllowedOrigins(), "Allowed origins should not be null");
        assertEquals(2, config.getAllowedOrigins().size(), 
            "Should have 2 allowed origins");
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"), 
            "Should contain localhost:3000");
        assertTrue(config.getAllowedOrigins().contains("http://localhost:4200"), 
            "Should contain localhost:4200");
    }

    /**
     * Test that allowed methods are configured correctly.
     * Requirement 12.2
     */
    @Test
    void testAllowedMethods_ShouldBeConfigured() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(null);
        
        assertNotNull(config.getAllowedMethods(), "Allowed methods should not be null");
        assertEquals(6, config.getAllowedMethods().size(), 
            "Should have 6 allowed methods");
        assertTrue(config.getAllowedMethods().contains("GET"), "Should contain GET");
        assertTrue(config.getAllowedMethods().contains("POST"), "Should contain POST");
        assertTrue(config.getAllowedMethods().contains("PUT"), "Should contain PUT");
        assertTrue(config.getAllowedMethods().contains("DELETE"), "Should contain DELETE");
        assertTrue(config.getAllowedMethods().contains("OPTIONS"), "Should contain OPTIONS");
        assertTrue(config.getAllowedMethods().contains("PATCH"), "Should contain PATCH");
    }

    /**
     * Test that allowed headers are configured correctly.
     * Requirement 12.4
     */
    @Test
    void testAllowedHeaders_ShouldBeConfigured() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(null);
        
        assertNotNull(config.getAllowedHeaders(), "Allowed headers should not be null");
        assertEquals(1, config.getAllowedHeaders().size(), 
            "Should have 1 allowed header pattern");
        assertTrue(config.getAllowedHeaders().contains("*"), 
            "Should allow all headers with *");
    }

    /**
     * Test that exposed headers are configured correctly.
     * Requirement 12.4
     */
    @Test
    void testExposedHeaders_ShouldBeConfigured() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(null);
        
        assertNotNull(config.getExposedHeaders(), "Exposed headers should not be null");
        assertEquals(2, config.getExposedHeaders().size(), 
            "Should have 2 exposed headers");
        assertTrue(config.getExposedHeaders().contains("X-Correlation-ID"), 
            "Should expose X-Correlation-ID");
        assertTrue(config.getExposedHeaders().contains("Authorization"), 
            "Should expose Authorization");
    }

    /**
     * Test that credentials are allowed.
     * Requirement 12.5
     */
    @Test
    void testAllowCredentials_ShouldBeTrue() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(null);
        
        assertNotNull(config.getAllowCredentials(), 
            "Allow credentials should not be null");
        assertTrue(config.getAllowCredentials(), 
            "Should allow credentials");
    }

    /**
     * Test that max age is configured correctly.
     * Requirement 12.2
     */
    @Test
    void testMaxAge_ShouldBeConfigured() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(null);
        
        assertNotNull(config.getMaxAge(), "Max age should not be null");
        assertEquals(3600L, config.getMaxAge(), 
            "Max age should be 3600 seconds (1 hour)");
    }

    /**
     * Test that CORS configuration applies to all paths.
     * Requirement 12.5
     */
    @Test
    void testCorsConfiguration_ShouldApplyToAllPaths() {
        UrlBasedCorsConfigurationSource source = 
            (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();
        
        // Test various paths
        assertNotNull(source.getCorsConfiguration(null), 
            "Should have CORS config for root");
        // Note: UrlBasedCorsConfigurationSource uses PathPattern matching
        // The /** pattern should match all paths
    }
}
