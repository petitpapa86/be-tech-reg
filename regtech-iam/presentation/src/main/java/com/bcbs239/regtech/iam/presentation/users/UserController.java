package com.bcbs239.regtech.iam.presentation.users;

import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContext;
import com.bcbs239.regtech.iam.application.users.RegisterUserCommand;
import com.bcbs239.regtech.iam.application.users.RegisterUserCommandHandler;
import com.bcbs239.regtech.iam.application.users.RegisterUserResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Set;

import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * Functional routing configuration for user management operations
 */
@Configuration
public class UserController extends BaseController {

    private final RegisterUserCommandHandler registerUserCommandHandler;

    public UserController(RegisterUserCommandHandler registerUserCommandHandler) {
        this.registerUserCommandHandler = registerUserCommandHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> userRoutes() {
        // Build registration route (public)
        RouterFunction<ServerResponse> registerRoute = RouterFunctions
                .route(POST("/api/v1/users/register").and(accept(MediaType.APPLICATION_JSON)),
                        this::registerUserHandler);

        // Build profile route (protected with permissions)
        RouterFunction<ServerResponse> profileRoute = RouterFunctions
                .route(GET("/api/v1/users/profile").and(accept(MediaType.APPLICATION_JSON)),
                        this::getUserProfileHandler);

        // Apply security attributes and combine
        return RouterAttributes.asPublic(registerRoute)
                .and(RouterAttributes.withPermissions(profileRoute, "users:read"));
    }

    private ServerResponse registerUserHandler(ServerRequest request) {
        try {
            RegisterUserRequest req = request.body(RegisterUserRequest.class);

            // Create command with validation
            Result<RegisterUserCommand> commandResult = RegisterUserCommand.create(
                    req.email(),
                    req.password(),
                    req.firstName(),
                    req.lastName(),
                    req.bankId(),
                    req.paymentMethodId(),
                    req.phone(),
                    req.address() != null ? new RegisterUserCommand.AddressInfo(
                            req.address().line1(),
                            req.address().line2(),
                            req.address().city(),
                            req.address().state(),
                            req.address().postalCode(),
                            req.address().country()
                    ) : null
            );

            if (commandResult.isFailure()) {
                ErrorDetail error = commandResult.getError().get();
                ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(
                        error.getFieldErrors(),
                        error.getMessage()
                );
                return ServerResponse.status(responseEntity.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseEntity.getBody());
            }

            RegisterUserCommand command = commandResult.getValue().get();
            Result<RegisterUserResponse> result = registerUserCommandHandler.handle(command);

            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                    result,
                    "User registered successfully",
                    "user.register.success"
            );

            return ServerResponse.status(responseEntity.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseEntity.getBody());

        } catch (Exception e) {
            return ServerResponse.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error().message(e.getMessage()).build());
        }
    }

    private ServerResponse getUserProfileHandler(ServerRequest request) {
        try {
            String userId = SecurityContext.getCurrentUserId();
            String bankId = SecurityContext.getCurrentBankId();
            Set<String> permissions = SecurityContext.getCurrentUserPermissions();

            UserProfileResponse profile = new UserProfileResponse(userId, bankId, permissions);
            ApiResponse<UserProfileResponse> response = ApiResponse.success(
                    profile,
                    "User profile retrieved successfully"
            );

            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            return ServerResponse.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error().message(e.getMessage()).build());
        }
    }

    public record RegisterUserRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String bankId,
            String paymentMethodId,
            String phone,
            AddressInfo address
    ) {}

    public record AddressInfo(
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {}

    public record UserProfileResponse(
            String userId,
            String bankId,
            Set<String> permissions
    ) {}
}