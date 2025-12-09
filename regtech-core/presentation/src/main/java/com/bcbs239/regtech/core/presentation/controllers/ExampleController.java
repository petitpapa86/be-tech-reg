package com.bcbs239.regtech.core.presentation.controllers;

import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example controller demonstrating the shared API response structure.
 * This shows how all bounded contexts can use consistent response envelopes.
 */
@RestController
@RequestMapping("/api/v1/examples")
public class ExampleController {

    /**
     * Example success response
     */
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuccessExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", 123);
        data.put("username", "john.doe");
        data.put("email", "john.doe@example.com");

        ApiResponse<Map<String, Object>> response = ResponseUtils.success(data, "User retrieved successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Example validation error response
     */
    @PostMapping("/validation-error")
    public ResponseEntity<ApiResponse<Void>> createUserWithValidationError(@RequestBody Map<String, Object> request) {
        // Simulate validation errors
        List<FieldError> fieldErrors = Arrays.asList(
            ResponseUtils.fieldError("email", "INVALID_EMAIL", "Email format is invalid"),
            ResponseUtils.fieldError("password", "TOO_WEAK", "Password must be at least 8 characters", "validation.password.too_weak")
        );

        ApiResponse<Void> response = ResponseUtils.validationError(fieldErrors, "Validation failed");

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Example business rule error response
     */
    @PostMapping("/business-error")
    public ResponseEntity<ApiResponse<Void>> createUserWithBusinessError(@RequestBody Map<String, Object> request) {
        // Simulate business rule violation
        ApiResponse<Void> response = ResponseUtils.businessRuleError(
            "Email address is already registered",
            "business.user.email_exists"
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Example system error response
     */
    @GetMapping("/system-error")
    public ResponseEntity<ApiResponse<Void>> getSystemError() {
        // Simulate system error
        ApiResponse<Void> response = ResponseUtils.systemError(
            "Database connection failed. Please try again later."
        );

        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * Example authentication error response
     */
    @GetMapping("/auth-error")
    public ResponseEntity<ApiResponse<Void>> getAuthenticationError() {
        ApiResponse<Void> response = ResponseUtils.authenticationError(
            "Authentication required. Please provide valid credentials."
        );

        return ResponseEntity.status(401).body(response);
    }

    /**
     * Example not found error response
     */
    @GetMapping("/not-found")
    public ResponseEntity<ApiResponse<Void>> getNotFoundError() {
        ApiResponse<Void> response = ResponseUtils.notFoundError(
            "The requested resource was not found."
        );

        return ResponseEntity.status(404).body(response);
    }

    /**
     * Example using custom builder
     */
    @GetMapping("/custom")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("items", Arrays.asList("item1", "item2", "item3"));
        data.put("total", 3);

        Map<String, Object> customMeta = new HashMap<>();
        customMeta.put("page", 1);
        customMeta.put("pageSize", 10);
        customMeta.put("totalPages", 1);

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>success()
                .data(data)
                .message("Items retrieved successfully")
                .messageKey("api.items.retrieved")
                .meta(ResponseUtils.createMeta(customMeta))
                .build();

        return ResponseEntity.ok(response);
    }
}

