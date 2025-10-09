package com.bcbs239.regtech.core.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modular security configuration that dynamically creates security filter chains
 * based on module registrations. Each bounded context registers its own security rules.
 */
@Configuration
@EnableWebSecurity
public class ModularSecurityConfiguration {

    @Autowired
    private SecurityConfigurationRegistry securityConfigurationRegistry;

    /**
     * IAM module security filter chain
     */
    @Bean
    @Order(1)
    public SecurityFilterChain iamSecurityFilterChain(HttpSecurity http) throws Exception {
        SecurityConfigurationRegistry.ModuleSecurityConfiguration iamConfig =
            securityConfigurationRegistry.getAllConfigurations().get("iam");
        if (iamConfig != null) {
            http.securityMatcher(iamConfig.getPathPatterns());
            iamConfig.configure(http);
            return http.build();
        }
        return null;
    }

    /**
     * Billing module security filter chain
     */
    @Bean
    @Order(2)
    public SecurityFilterChain billingSecurityFilterChain(HttpSecurity http) throws Exception {
        SecurityConfigurationRegistry.ModuleSecurityConfiguration billingConfig =
            securityConfigurationRegistry.getAllConfigurations().get("billing");
        if (billingConfig != null) {
            http.securityMatcher(billingConfig.getPathPatterns());
            billingConfig.configure(http);
            return http.build();
        }
        return null;
    }

    /**
     * Base security configuration for all modules - lowest priority
     */
    @Bean
    @Order(100) // Lower priority - catches remaining patterns
    public SecurityFilterChain baseSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Health endpoints - public
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/*/health/**").permitAll()

                // TEMPORARILY ALLOW ALL REQUESTS FOR TESTING
                // TODO: Implement proper authentication mechanism
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable()) // Modules can override
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}