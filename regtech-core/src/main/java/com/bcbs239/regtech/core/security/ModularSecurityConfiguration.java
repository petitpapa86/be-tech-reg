package com.bcbs239.regtech.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Modular security configuration that allows each bounded context
 * to register its own security rules while maintaining separation of concerns.
 */
@Configuration
@EnableWebSecurity
public class ModularSecurityConfiguration {

    /**
     * Base security configuration for all modules
     */
    @Bean
    @Order(100) // Lower priority - catches remaining patterns
    public SecurityFilterChain baseSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Health endpoints - public
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/*/health/**").permitAll()
                
                // Default - require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable()) // Modules can override
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * IAM module security configuration
     */
    @Bean
    @Order(1)
    public SecurityFilterChain iamSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/users/**", "/api/v1/auth/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/users/register").permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/v1/auth/**").authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * Billing module security configuration
     */
    @Bean
    @Order(2)
    public SecurityFilterChain billingSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/billing/**", "/api/v1/subscriptions/**")
            .authorizeHttpRequests(authz -> authz
                // Webhook endpoints - signature validation only
                .requestMatchers("/api/v1/billing/webhooks/**").permitAll()
                
                // Payment processing - authenticated
                .requestMatchers("/api/v1/billing/process-payment").authenticated()
                
                // Subscription management - authenticated
                .requestMatchers("/api/v1/subscriptions/**").authenticated()
                
                // Monitoring - admin only
                .requestMatchers("/api/v1/billing/monitoring/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/billing/webhooks/**")
            )
            .addFilterBefore(webhookSignatureValidationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(billingRateLimitingFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebhookSignatureValidationFilter webhookSignatureValidationFilter() {
        return new WebhookSignatureValidationFilter();
    }

    @Bean
    public BillingRateLimitingFilter billingRateLimitingFilter() {
        return new BillingRateLimitingFilter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Global CORS configuration
        return new GlobalCorsConfigurationSource();
    }
}