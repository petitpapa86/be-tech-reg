package com.bcbs239.regtech.iam.presentation.users;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContextHolder;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.iam.application.users.RegisterUserCommand;
import com.bcbs239.regtech.iam.application.users.RegisterUserCommandHandler;
import com.bcbs239.regtech.iam.application.users.RegisterUserResponse;
import io.micrometer.observation.annotation.Observed;
import jakarta.servlet.ServletException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Controller handling user management operations logic.
 * Routing is handled by UserRoutes.
 */
@Component
public class UserController extends BaseController {

    private final RegisterUserCommandHandler registerUserCommandHandler;

    public UserController(RegisterUserCommandHandler registerUserCommandHandler) {
        this.registerUserCommandHandler = registerUserCommandHandler;
    }

    @Observed(name = "iam.api.users.register", contextualName = "register-user")
    public ServerResponse registerUserHandler(ServerRequest request) throws ServletException, IOException {

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
                assert responseEntity.getBody() != null;
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

        assert responseEntity.getBody() != null;
        return ServerResponse.status(responseEntity.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseEntity.getBody());


    }

    @Observed(name = "iam.api.users.profile", contextualName = "get-user-profile")
    public ServerResponse getUserProfileHandler(ServerRequest request) {
        try {
            var ctx = SecurityContextHolder.getContext();
            String userId = ctx != null ? ctx.getUserId() : null;
            String bankId = ctx != null && ctx.getAuthentication() != null ? ctx.getAuthentication().getBankId() : null;
            Set<String> permissions = ctx != null && ctx.getAuthentication() != null ? ctx.getAuthentication().getPermissions() : Set.of();

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