package com.bcbs239.regtech.iam.presentation.users;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;

/**
 * Router configuration for User management endpoints.
 */
@Configuration
public class UserRoutes {

    private final UserController controller;

    public UserRoutes(UserController controller) {
        this.controller = controller;
    }

    @Bean
    public RouterFunction<ServerResponse> userRoutesConfig() {
        // Build registration route (public)
        RouterFunction<ServerResponse> registerRoute = RouterFunctions
                .route(POST("/api/v1/users/register"),
                        controller::registerUserHandler);

        // Build profile route (protected with permissions)
        RouterFunction<ServerResponse> profileRoute = RouterFunctions
                .route(GET("/api/v1/users/profile"),
                        controller::getUserProfileHandler);

        // Apply security attributes and combine
        return RouterAttributes.asPublic(registerRoute)
                .and(RouterAttributes.withPermissions(profileRoute, "users:read"));
    }
}
