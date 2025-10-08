package com.bcbs239.regtech.iam.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for billing-related endpoints.
 * 
 * This configuration is part of the IAM module as it handles authentication
 * and authorization for billing operations.
 * 
 * Provides:
 * - Endpoint security for billing operations
 * - API key validation for webhook endpoints
 * - Rate limiting configuration for payment processing
 * - CORS configuration for cross-origin requests
 */
@Configuration
public class BillingSecurityConfiguration {

    @Value("${billing.webhook.stripe.endpoint-secret:}")
    private String stripeWebhookSecret;

    @Value("${billing.security.rate-limit.payment.requests-per-minute:10}")
    private int paymentRateLimit;

    @Value("${billing.security.rate-limit.webhook.requests-per-minute:100}")
    private int webhookRateLimit;

    /**
     * Configure security filter chain for billing endpoints
     */
    @Bean
    public SecurityFilterChain billingSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/billing/**")
            .authorizeHttpRequests(authz -> authz
                // Webhook endpoints - require signature validation but no authentication
                .requestMatchers("/api/v1/billing/webhooks/**").permitAll()
                
                // Health check endpoints - public access
                .requestMatchers("/api/v1/billing/health/**").permitAll()
                .requestMatchers("/api/v1/billing/webhooks/*/health").permitAll()
                
                // Payment processing endpoints - require authentication
                .requestMatchers("/api/v1/billing/process-payment").authenticated()
                
                // Subscription management endpoints - require authentication
                .requestMatchers("/api/v1/subscriptions/**").authenticated()
                
                // Monitoring endpoints - require admin role
                .requestMatchers("/api/v1/billing/monitoring/**").hasRole("ADMIN")
                
                // All other billing endpoints require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf
                // Disable CSRF for webhook endpoints as they use signature verification
                .ignoringRequestMatchers("/api/v1/billing/webhooks/**")
                .ignoringRequestMatchers("/api/v1/billing/health/**")
            )
            .cors(cors -> cors.configurationSource(billingCorsConfigurationSource()))
            .addFilterBefore(webhookSignatureValidationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(billingRateLimitingFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Webhook signature validation filter for Stripe webhooks
     */
    @Bean
    public WebhookSignatureValidationFilter webhookSignatureValidationFilter() {
        return new WebhookSignatureValidationFilter(stripeWebhookSecret);
    }

    /**
     * Rate limiting filter for billing endpoints
     */
    @Bean
    public BillingRateLimitingFilter billingRateLimitingFilter() {
        return new BillingRateLimitingFilter(paymentRateLimit, webhookRateLimit);
    }

    /**
     * CORS configuration for billing endpoints
     */
    @Bean
    public CorsConfigurationSource billingCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins in production, all origins in development
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Stripe-Signature",
            "X-API-Key"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/billing/**", configuration);
        
        return source;
    }
}