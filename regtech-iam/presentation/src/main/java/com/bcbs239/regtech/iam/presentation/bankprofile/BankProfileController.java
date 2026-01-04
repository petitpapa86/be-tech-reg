package com.bcbs239.regtech.iam.presentation.bankprofile;

import com.bcbs239.regtech.iam.application.bankprofile.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        
        var response = BankProfileResponse.from(profileMaybe.get());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * PUT /api/v1/configuration/bank-profile/{bankId}
     */
    private ServerResponse updateBankProfile(ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            
            // Parse request body
            BankProfileRequest dto = request.body(BankProfileRequest.class);
            
            // Validate
            var errors = new BeanPropertyBindingResult(dto, "bankProfileRequest");
            validator.validate(dto, errors);
            
            if (errors.hasErrors()) {
                var errorMessages = errors.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
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
                        .body(new ErrorResponse(result.getError()));
            }
            
            var response = BankProfileResponse.from(result.getValue());
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            
        } catch (IOException e) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse("Invalid request body: " + e.getMessage()));
        }
    }
    
    private record ErrorResponse(String error) {}
}
