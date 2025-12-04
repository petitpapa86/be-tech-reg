package com.bcbs239.regtech.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for CORS configuration in Spring Framework 7.
 * 
 * Spring Framework 7 Behavior Changes:
 * - Pre-flight OPTIONS requests are NOT rejected when CORS configuration is empty (#31839)
 * - This test verifies that our explicit CORS configuration works correctly
 * - Tests verify origin validation, method validation, and header handling
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that pre-flight OPTIONS requests are handled correctly.
     * Spring Framework 7: Pre-flight requests should not be rejected with empty config.
     * We verify that our explicit config provides appropriate CORS headers.
     * 
     * Requirement 12.1, 12.2
     */
    @Test
    void testPreFlightRequest_ShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    /**
     * Test that allowed origins are validated correctly.
     * 
     * Requirement 12.3
     */
    @Test
    void testAllowedOrigin_ShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    /**
     * Test that disallowed origins are rejected.
     * 
     * Requirement 12.3, 12.5
     */
    @Test
    void testDisallowedOrigin_ShouldNotReturnCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/health")
                .header(HttpHeaders.ORIGIN, "http://evil.com"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /**
     * Test that allowed methods are included in pre-flight response.
     * 
     * Requirement 12.2
     */
    @Test
    void testAllowedMethods_ShouldBeIncludedInPreFlightResponse() throws Exception {
        mockMvc.perform(options("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, 
                    org.hamcrest.Matchers.containsString("POST")));
    }

    /**
     * Test that exposed headers are included in response.
     * 
     * Requirement 12.4
     */
    @Test
    void testExposedHeaders_ShouldBeIncludedInResponse() throws Exception {
        mockMvc.perform(get("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    /**
     * Test that credentials are allowed when configured.
     * 
     * Requirement 12.5
     */
    @Test
    void testAllowCredentials_ShouldBeTrue() throws Exception {
        mockMvc.perform(get("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    /**
     * Test that pre-flight requests with custom headers are handled.
     * 
     * Requirement 12.4
     */
    @Test
    void testPreFlightWithCustomHeaders_ShouldBeAllowed() throws Exception {
        mockMvc.perform(options("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Custom-Header,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    /**
     * Test that actual requests (non-preflight) include CORS headers.
     * 
     * Requirement 12.5
     */
    @Test
    void testActualRequest_ShouldIncludeCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header("X-Custom-Header", "value"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }
}
