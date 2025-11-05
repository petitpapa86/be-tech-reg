package com.bcbs239.regtech.billing.presentation.billing;

import com.bcbs239.regtech.core.security.authorization.AuthorizationService;
import com.bcbs239.regtech.core.security.authorization.Permission;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Example controller showing how billing module uses authorization service
 * to check permissions before performing operations.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingAuthorizationController {
    
    private final AuthorizationService authorizationService;
    
    public BillingAuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
    
    @PostMapping("/process-payment")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request) {
        // Check permission programmatically (in addition to Spring Security)
        if (!authorizationService.hasPermission(Permission.BILLING_PROCESS_PAYMENT)) {
            return ResponseEntity.status(403)
                .body("Insufficient permissions to process payments");
        }
        
        // Additional business logic checks
        if (request.getAmount() > 10000 && !authorizationService.hasPermission(Permission.BILLING_ADMIN)) {
            return ResponseEntity.status(403)
                .body("Large payments require billing admin permission");
        }
        
        // Process payment logic here
        return ResponseEntity.ok("Payment processed successfully");
    }
    
    @GetMapping("/invoices/{organizationId}")
    public ResponseEntity<?> getInvoices(@PathVariable String organizationId) {
        // Check if user can access this organization's data
        if (!authorizationService.canAccessOrganization(organizationId)) {
            return ResponseEntity.status(403)
                .body("Cannot access invoices for this organization");
        }
        
        // Check billing read permission
        if (!authorizationService.hasPermission(Permission.BILLING_VIEW_INVOICES)) {
            return ResponseEntity.status(403)
                .body("Insufficient permissions to view invoices");
        }
        
        // Get invoices logic here
        return ResponseEntity.ok("Invoices for organization: " + organizationId);
    }
    
    @PostMapping("/admin/configure-webhooks")
    public ResponseEntity<?> configureWebhooks(@RequestBody WebhookConfig config) {
        // Check multiple permissions
        if (!authorizationService.hasAllPermissions(
                Permission.BILLING_ADMIN, 
                Permission.BILLING_WEBHOOK_MANAGE)) {
            return ResponseEntity.status(403)
                .body("Requires both billing admin and webhook management permissions");
        }
        
        // Configure webhooks logic here
        return ResponseEntity.ok("Webhooks configured successfully");
    }
    
    @GetMapping("/reports/financial")
    public ResponseEntity<?> getFinancialReports() {
        // Check if user has any reporting permission
        if (!authorizationService.hasAnyPermission(
                Permission.REPORT_VIEW, 
                Permission.REPORT_CREATE, 
                Permission.BILLING_ADMIN)) {
            return ResponseEntity.status(403)
                .body("No reporting permissions found");
        }
        
        // Get reports logic here
        return ResponseEntity.ok("Financial reports");
    }
    
    // Example DTOs
    @Setter
    @Getter
    public static class PaymentRequest {
        private double amount;
        private String currency;

    }
    
    @Setter
    @Getter
    public static class WebhookConfig {
        private String url;
        private String secret;

    }
}

