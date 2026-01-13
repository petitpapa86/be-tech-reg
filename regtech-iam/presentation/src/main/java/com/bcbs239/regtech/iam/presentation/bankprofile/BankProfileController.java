package com.bcbs239.regtech.iam.presentation.bankprofile;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.iam.application.bankprofile.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.servlet.ServletException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * Bank Profile Routes (Route Functions)
 * 
 * Following the pattern from UserController in regtech-iam
 */
@Configuration
@RequiredArgsConstructor
public class BankProfileController {
    
    private final GetBankProfileHandler getBankProfileHandler;
    private final UpdateBankProfileHandler updateBankProfileHandler;
    private final ResetBankProfileHandler resetBankProfileHandler;
    private final Validator validator;
    
    @Bean
    public RouterFunction<ServerResponse> bankProfileRoutes() {
        return route()
                .GET("/api/v1/configuration/bank-profile/{bankId}", 
                        accept(MediaType.APPLICATION_JSON), 
                        this::getBankProfile)
                .PUT("/api/v1/configuration/bank-profile/{bankId}", 
                        accept(MediaType.APPLICATION_JSON), 
                        this::updateBankProfile)
                .POST("/api/v1/configuration/bank-profile/{bankId}/reset", 
                        accept(MediaType.APPLICATION_JSON), 
                        this::resetBankProfile)
                .build();
    }
    
    /**
     * GET /api/v1/configuration/bank-profile/{bankId}
     */
    private ServerResponse getBankProfile(ServerRequest request) {
        Long bankId = Long.parseLong(request.pathVariable("bankId"));
        var profileMaybe = getBankProfileHandler.handle(bankId);
        
        if (profileMaybe.isEmpty()) {
            return ServerResponse.notFound().build();
        }
        
        var response = BankProfileResponse.from(profileMaybe.getValue());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * PUT /api/v1/configuration/bank-profile/{bankId}
     */
    private ServerResponse updateBankProfile(ServerRequest request) throws ServletException {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            
            // Parse request body
            BankProfileRequest dto = request.body(BankProfileRequest.class);
            
            // Validate
            var errors = new BeanPropertyBindingResult(dto, "bankProfileRequest");
            validator.validate(dto, errors);
            
            if (errors.hasErrors()) {
                var errorMessages = errors.getAllErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .toList();
                return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ErrorResponse(String.join(", ", errorMessages)));
            }
            
            // TODO: Get actual user from security context
            String currentUser = "system";
            
            // Execute command
            Result<com.bcbs239.regtech.iam.domain.bankprofile.BankProfile> result = 
                    updateBankProfileHandler.handle(dto.toCommand(bankId, currentUser));
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ErrorResponse(result.getError().map(ErrorDetail::getMessage).orElse("Unknown error")));
            }
            
            var response = BankProfileResponse.from(result.getValueOrThrow());
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            
        } catch (IOException e) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse("Invalid request body: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/v1/configuration/bank-profile/{bankId}/reset
     */
    private ServerResponse resetBankProfile(ServerRequest request) {
        Long bankId = Long.parseLong(request.pathVariable("bankId"));
        
        // TODO: Get actual user from security context
        String currentUser = "system";
        
        // Execute command - no try-catch, let exceptions propagate
        var profile = resetBankProfileHandler.handle(bankId, currentUser);
        
        var response = BankProfileResponse.from(profile);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    private record ErrorResponse(String error) {}
}
