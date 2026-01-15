package com.bcbs239.regtech.iam.presentation.bankprofile;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.iam.application.bankprofile.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.servlet.ServletException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.List;

/**
 * Bank Profile Controller
 * <p>
 * Handles business logic for bank profile operations.
 * Routing is handled by BankProfileRoutes.
 */
@Component
@RequiredArgsConstructor
public class BankProfileController extends BaseController {

    private final GetBankProfileHandler getBankProfileHandler;
    private final UpdateBankProfileHandler updateBankProfileHandler;
    private final ResetBankProfileHandler resetBankProfileHandler;
    private final Validator validator;

    /**
     * GET /api/v1/configuration/bank-profile/{bankId}
     */
    public ServerResponse getBankProfile(ServerRequest request) {
        String bankId = request.pathVariable("bankId");
        var profileMaybe = getBankProfileHandler.handle(bankId);

        if (profileMaybe.isEmpty()) {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(
                    ErrorDetail.of("BANK_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, "Bank profile not found for ID: " + bankId, "bankprofile.notfound")
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseEntity.getBody());
        }

        BankProfileResponse response = BankProfileResponse.from(profileMaybe.getValue());
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(response),
                "Bank profile retrieved successfully",
                "bankprofile.get.success"
        );
        assert responseEntity.getBody() != null;
        return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());

    }

    /**
     * PUT /api/v1/configuration/bank-profile
     */
    public ServerResponse updateBankProfile(ServerRequest request) throws ServletException, IOException {
        // 1. Parse request body
        BankProfileRequest dto = request.body(BankProfileRequest.class);

        // 2. Validate DTO at the boundary
        var errors = new BeanPropertyBindingResult(dto, "bankProfileRequest");
        validator.validate(dto, errors);

        if (errors.hasErrors()) {
            List<FieldError> presentationErrors = errors.getFieldErrors().stream()
                    .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage(), fe.getCode()))
                    .toList();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleValidationError(
                    presentationErrors,
                    "Validation failed for bank profile update"
            );
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseEntity.getBody());
        }

        // TODO: Get actual user from security context
        String currentUser = "system";

        // 3. Execute command via handler
        Result<com.bcbs239.regtech.iam.domain.bankprofile.BankProfile> result =
                updateBankProfileHandler.handle(dto.toCommand(currentUser));

        // 4. Handle domain/business results
        if (result.isFailure()) {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(result.getError().get());
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseEntity.getBody());
        }

        // 5. Convert to response DTO and return success
        BankProfileResponse response = BankProfileResponse.from(result.getValueOrThrow());
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(response),
                "Bank profile updated successfully",
                "bankprofile.update.success"
        );
        assert responseEntity.getBody() != null;
        return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());

    }

    /**
     * POST /api/v1/configuration/bank-profile/{bankId}/reset
     */
    public ServerResponse resetBankProfile(ServerRequest request) {
        String bankId = request.pathVariable("bankId");

        // TODO: Get actual user from security context
        String currentUser = "system";

        // Execute command - no try-catch, let exceptions propagate
        var profile = resetBankProfileHandler.handle(bankId, currentUser);

        var response = BankProfileResponse.from(profile);
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                Result.success(response),
                "Bank profile reset successfully",
                "bankprofile.reset.success"
        );
        assert responseEntity.getBody() != null;
        return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());

    }
}
