package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContextHolder;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.iam.application.authentication.*;
import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;

/**
 * Authentication Controller
 * 
 * Handles authentication endpoints: login, refresh, select-bank, and logout
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Configuration
public class AuthenticationController extends BaseController {

    private final LoginCommandHandler loginHandler;
    private final RefreshTokenCommandHandler refreshTokenHandler;
    private final SelectBankCommandHandler selectBankHandler;
    private final LogoutCommandHandler logoutHandler;

    public AuthenticationController(
        LoginCommandHandler loginHandler,
        RefreshTokenCommandHandler refreshTokenHandler,
        SelectBankCommandHandler selectBankHandler,
        LogoutCommandHandler logoutHandler
    ) {
        this.loginHandler = loginHandler;
        this.refreshTokenHandler = refreshTokenHandler;
        this.selectBankHandler = selectBankHandler;
        this.logoutHandler = logoutHandler;
    }

    /**
     * Handles user login requests
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    public ServerResponse loginHandler(ServerRequest request) throws ServletException, IOException {
        // 1. Parse request
        LoginRequest req = request.body(LoginRequest.class);

        // 2. Extract IP address
        String ipAddress = request.remoteAddress()
            .map(addr -> addr.getAddress().getHostAddress())
            .orElse("unknown");

        // 3. Create command
        Result<LoginCommand> commandResult = LoginCommand.create(
            req.email(),
            req.password(),
            ipAddress
        );

        // 4. Handle validation errors
        if (commandResult.isFailure()) {
            var error = commandResult.getError().get();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(
                error.getFieldErrors(),
                error.getMessage()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }

        // 5. Execute command
        Result<LoginResponse> result = loginHandler.handle(
            commandResult.getValue().get()
        );

        // 6. Convert to presentation DTO and handle result
        if (result.isSuccess()) {
            LoginResponseDto responseDto = LoginResponseDto.from(result.getValue().get());
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(responseDto),
                "Login successful",
                "login.success"
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(
                result.getError().get()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }
    }

    /**
     * Handles token refresh requests
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
     */
    public ServerResponse refreshTokenHandler(ServerRequest request) throws ServletException, IOException {
        // 1. Parse request
        RefreshTokenRequest req = request.body(RefreshTokenRequest.class);

        // 2. Create command
        Result<RefreshTokenCommand> commandResult = RefreshTokenCommand.create(
            req.refreshToken()
        );

        // 3. Handle validation errors
        if (commandResult.isFailure()) {
            var error = commandResult.getError().get();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(
                error.getFieldErrors(),
                error.getMessage()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }

        // 4. Execute command
        Result<RefreshTokenResponse> result = refreshTokenHandler.handle(
            commandResult.getValue().get()
        );

        // 5. Convert to presentation DTO and handle result
        if (result.isSuccess()) {
            RefreshTokenResponseDto responseDto = RefreshTokenResponseDto.from(result.getValue().get());
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(responseDto),
                "Token refreshed successfully",
                "refresh_token.success"
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(
                result.getError().get()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }
    }

    /**
     * Handles bank selection requests
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
     */
    public ServerResponse selectBankHandler(ServerRequest request) throws ServletException, IOException {
        // 1. Parse request
        SelectBankRequest req = request.body(SelectBankRequest.class);

        // 2. Create command
        Result<SelectBankCommand> commandResult = SelectBankCommand.create(
            req.userId(),
            req.bankId(),
            req.refreshToken()
        );

        // 3. Handle validation errors
        if (commandResult.isFailure()) {
            var error = commandResult.getError().get();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(
                error.getFieldErrors(),
                error.getMessage()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }

        // 4. Execute command
        Result<SelectBankResponse> result = selectBankHandler.handle(
            commandResult.getValue().get()
        );

        // 5. Convert to presentation DTO and handle result
        if (result.isSuccess()) {
            SelectBankResponseDto responseDto = SelectBankResponseDto.from(result.getValue().get());
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(responseDto),
                "Bank selected successfully",
                "select_bank.success"
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(
                result.getError().get()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }
    }

    /**
     * Handles logout requests
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    public ServerResponse logoutHandler(ServerRequest request) throws ServletException, IOException {
        // 1. Get user ID from security context
        var securityContext = SecurityContextHolder.getContext();
        String userId = securityContext != null ? securityContext.getUserId() : null;

        if (userId == null) {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = ResponseEntity.status(401)
                .body(ApiResponse.error().message("Unauthorized").build());
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }

        // 2. Parse request
        LogoutRequest req = request.body(LogoutRequest.class);

        // 3. Create command
        Result<LogoutCommand> commandResult = LogoutCommand.create(
            userId,
            req.refreshToken()
        );

        // 4. Handle validation errors
        if (commandResult.isFailure()) {
            var error = commandResult.getError().get();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(
                error.getFieldErrors(),
                error.getMessage()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }

        // 5. Execute command
        Result<LogoutResponse> result = logoutHandler.handle(
            commandResult.getValue().get()
        );

        // 6. Convert to presentation DTO and handle result
        if (result.isSuccess()) {
            LogoutResponseDto responseDto = LogoutResponseDto.from(result.getValue().get());
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(responseDto),
                "Logged out successfully",
                "logout.success"
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(
                result.getError().get()
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
        }
    }
}
