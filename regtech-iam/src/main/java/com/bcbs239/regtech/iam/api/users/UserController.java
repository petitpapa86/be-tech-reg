package com.bcbs239.regtech.iam.api.users;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.iam.application.createuser.RegisterUserCommand;
import com.bcbs239.regtech.iam.application.createuser.RegisterUserCommandHandler;
import com.bcbs239.regtech.iam.application.createuser.RegisterUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for user management operations
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController {

    private final RegisterUserCommandHandler registerUserCommandHandler;

    public UserController(RegisterUserCommandHandler registerUserCommandHandler) {
        this.registerUserCommandHandler = registerUserCommandHandler;
    }

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<? extends ApiResponse<?>> registerUser(
            @RequestBody RegisterUserRequest request) {

        // Create command with validation
        Result<RegisterUserCommand> commandResult = RegisterUserCommand.create(
            request.email(),
            request.password(),
            request.firstName(),
            request.lastName(),
            request.bankId(),
            request.paymentMethodId(),
            request.phone(),
            request.address() != null ? new RegisterUserCommand.AddressInfo(
                request.address().line1(),
                request.address().line2(),
                request.address().city(),
                request.address().state(),
                request.address().postalCode(),
                request.address().country()
            ) : null
        );

        if (commandResult.isFailure()) {
            ErrorDetail error = commandResult.getError().get();
            return handleValidationError(error.getFieldErrors(), error.getMessage());
        }

        RegisterUserCommand command = commandResult.getValue().get();

        // Handle the command
        Result<RegisterUserResponse> result = registerUserCommandHandler.handle(command);

        return handleResult(result, "User registered successfully", "user.register.success");
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
    ) {}

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
    ) {}
}