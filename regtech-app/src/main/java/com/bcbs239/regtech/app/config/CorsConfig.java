package com.bcbs239.regtech.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for the RegTech application.
 * 
 * Spring Framework 7 Behavior Change:
 * - As of Spring Framework 7 (#31839), CORS pre-flight requests are NOT rejected
 *   when CORS configuration is empty
 * - This configuration explicitly defines allowed origins, methods, and headers
 *   to maintain security policies
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String[] allowedHeaders;

    @Value("${cors.exposed-headers:X-Correlation-ID,Authorization}")
    private String[] exposedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * Configures CORS for the application.
     * 
     * Spring Framework 7 Changes:
     * - Pre-flight OPTIONS requests are handled according to new behavior
     * - Empty CORS configuration no longer rejects requests (but we provide explicit config)
     * - CORS origin validation maintains existing security policies
     * - Pre-flight response headers include appropriate CORS headers
     * - Cross-origin request enforcement follows configured policies
     * 
     * @return CorsConfigurationSource with explicit CORS policies
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins (Requirement 12.3)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Set allowed methods (Requirement 12.2)
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        
        // Set allowed headers (Requirement 12.4)
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
        
        // Set exposed headers (Requirement 12.4)
        configuration.setExposedHeaders(Arrays.asList(exposedHeaders));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);
        
        // Set max age for pre-flight requests (Requirement 12.2)
        configuration.setMaxAge(maxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
