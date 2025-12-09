package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.accept;

/**
 * Authentication Routes Configuration
 * 
 * Defines routing for authentication endpoints with appropriate security attributes
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Configuration
public class AuthenticationRoutes {

    private final AuthenticationController controller;

    public AuthenticationRoutes(AuthenticationController controller) {
        this.controller = controller;
    }

    @Bean
    public RouterFunction<ServerResponse> authenticationRoutesConfig() {
        // Public routes (no authentication required)
        // Requirements: 7.5 - login, refresh, and select-bank are public endpoints
        RouterFunction<ServerResponse> publicRoutes = RouterFunctions
            .route(POST("/api/v1/auth/login").and(accept(MediaType.APPLICATION_JSON)),
                controller::loginHandler)
            .andRoute(POST("/api/v1/auth/refresh").and(accept(MediaType.APPLICATION_JSON)),
                controller::refreshTokenHandler)
            .andRoute(POST("/api/v1/auth/select-bank").and(accept(MediaType.APPLICATION_JSON)),
                controller::selectBankHandler);

        // Protected routes (requires authentication)
        // Requirements: 7.5 - logout requires authentication
        RouterFunction<ServerResponse> protectedRoutes = RouterFunctions
            .route(POST("/api/v1/auth/logout").and(accept(MediaType.APPLICATION_JSON)),
                controller::logoutHandler);

        // Apply security attributes and combine routes
        return RouterAttributes.asPublic(publicRoutes)
            .and(protectedRoutes);
    }
}
