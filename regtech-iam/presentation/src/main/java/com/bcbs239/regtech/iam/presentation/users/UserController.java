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
import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.Set;

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
        // Public route for user registration
        RouterFunction<ServerResponse> registerRoute = RouterFunctions.route(
                RequestPredicates.POST("/api/v1/users/register").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                this::registerUserHandler
        );

        // Protected route for user profile (requires users:read permission)
        RouterFunction<ServerResponse> profileRoute = RouterFunctions.route(
                RequestPredicates.GET("/api/v1/users/profile").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                this::getUserProfileHandler
        ).withAttribute("permissions", new String[]{"users:read"});

        // Combine routes: both registration and profile are public
        return RouterAttributes.asPublic(registerRoute).and(RouterAttributes.asPublic(profileRoute));
    }

    private ServerResponse registerUserHandler(ServerRequest request) throws ServletException, IOException {
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
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(error.getFieldErrors(), error.getMessage());
            return ServerResponse.status(responseEntity.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(responseEntity.getBody());
        }

        RegisterUserCommand command = commandResult.getValue().get();

        // Handle the command
        Result<RegisterUserResponse> result = registerUserCommandHandler.handle(command);

        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(result, "User registered successfully", "user.register.success");
        return ServerResponse.status(responseEntity.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(responseEntity.getBody());

    }

    private ServerResponse getUserProfileHandler(ServerRequest request) {
        // Get user context from SecurityContext (set by PermissionAuthorizationFilter)
        String userId = SecurityContext.getCurrentUserId();
        String bankId = SecurityContext.getCurrentBankId();
        Set<String> permissions = SecurityContext.getCurrentUserPermissions();

        // Create a simple profile response
        UserProfileResponse profile = new UserProfileResponse(userId, bankId, permissions);

        ApiResponse<UserProfileResponse> response = ApiResponse.success(profile, "User profile retrieved successfully");
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    /**
     * Request DTO for user registration with Stripe payment information
     */
    public record RegisterUserRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String bankId,
            String paymentMethodId,
            // Optional Stripe-related fields
            String phone,
            AddressInfo address
    ) {
    }

    /**
     * Address information for Stripe customer creation and tax compliance
     */
    public record AddressInfo(
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
    }

    /**
     * Response DTO for user profile
     */
    public record UserProfileResponse(
            String userId,
            String bankId,
            Set<String> permissions
    ) {
    }
}

