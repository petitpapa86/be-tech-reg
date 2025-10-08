# Cross-Module Authorization Examples

## How Modules Communicate About Roles and Permissions

### 1. Billing Module Checking Permissions

```java
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    
    private final AuthorizationService authorizationService;
    
    @PostMapping("/process-payment")
    @RequiresPermission(Permission.BILLING_PROCESS_PAYMENT)
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request) {
        // Additional business logic checks
        if (request.getAmount() > 10000 && !authorizationService.hasRole("BILLING_ADMIN")) {
            return ResponseEntity.status(403).body("Large payments require admin approval");
        }
        
        // Process payment...
        return ResponseEntity.ok("Payment processed");
    }
    
    @GetMapping("/invoices/{organizationId}")
    public ResponseEntity<?> getInvoices(@PathVariable String organizationId) {
        // Check organization access
        if (!authorizationService.canAccessOrganization(organizationId)) {
            return ResponseEntity.status(403).body("Access denied to organization");
        }
        
        // Check permission
        if (!authorizationService.hasPermission(Permission.BILLING_VIEW_INVOICES)) {
            return ResponseEntity.status(403).body("Cannot view invoices");
        }
        
        // Get invoices...
        return ResponseEntity.ok("Invoices for " + organizationId);
    }
}
```

### 2. Future Reporting Module Example

```java
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    
    private final AuthorizationService authorizationService;
    
    @GetMapping("/financial")
    @RequiresPermission(anyOf = {Permission.REPORT_VIEW, Permission.BILLING_ADMIN})
    public ResponseEntity<?> getFinancialReport() {
        // Method automatically checks permissions via annotation
        return ResponseEntity.ok("Financial report data");
    }
    
    @PostMapping("/export")
    public ResponseEntity<?> exportReport(@RequestBody ExportRequest request) {
        // Check multiple conditions
        boolean canExport = authorizationService.hasPermission(Permission.REPORT_EXPORT) ||
                           (authorizationService.hasPermission(Permission.REPORT_VIEW) && 
                            request.getFormat().equals("PDF"));
        
        if (!canExport) {
            return ResponseEntity.status(403).body("Cannot export in this format");
        }
        
        // Export logic...
        return ResponseEntity.ok("Report exported");
    }
    
    @DeleteMapping("/sensitive/{reportId}")
    @RequiresPermission(resourceType = "report", action = "delete")
    public ResponseEntity<?> deleteSensitiveReport(@PathVariable String reportId) {
        // Aspect automatically checks resource-based permission
        return ResponseEntity.ok("Report deleted");
    }
}
```

### 3. Compliance Module Example

```java
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {
    
    private final AuthorizationService authorizationService;
    
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam String organizationId) {
        // Multi-level permission check
        if (!authorizationService.canAccessOrganization(organizationId)) {
            return ResponseEntity.status(403).body("No access to organization");
        }
        
        if (!authorizationService.hasAnyPermission(
                Permission.COMPLIANCE_AUDIT, 
                Permission.SYSTEM_ADMIN)) {
            return ResponseEntity.status(403).body("No audit permissions");
        }
        
        // Get audit logs...
        return ResponseEntity.ok("Audit logs for " + organizationId);
    }
    
    @PostMapping("/risk-assessment")
    @RequiresPermission(Permission.COMPLIANCE_MANAGE)
    public ResponseEntity<?> performRiskAssessment(@RequestBody RiskRequest request) {
        // Additional role-based business logic
        if (request.getRiskLevel().equals("HIGH") && 
            !authorizationService.hasRole("COMPLIANCE_OFFICER")) {
            return ResponseEntity.status(403).body("High-risk assessments require compliance officer");
        }
        
        // Perform assessment...
        return ResponseEntity.ok("Risk assessment completed");
    }
}
```

## Permission Hierarchy Examples

### Basic User Permissions
```java
// What a basic USER can do:
- Permission.USER_READ (view own profile)
- Permission.BILLING_READ (view own billing info)
- Permission.BILLING_VIEW_INVOICES (view own invoices)
- Permission.REPORT_VIEW (view basic reports)
```

### Premium User Permissions
```java
// What a PREMIUM_USER can do (includes USER permissions plus):
- Permission.BILLING_PROCESS_PAYMENT (make payments)
- Permission.BILLING_MANAGE_SUBSCRIPTIONS (manage subscriptions)
- Permission.REPORT_EXPORT (export reports)
```

### Billing Admin Permissions
```java
// What a BILLING_ADMIN can do:
- All billing-related permissions
- Permission.BILLING_ADMIN (admin operations)
- Permission.BILLING_WEBHOOK_MANAGE (configure webhooks)
- Permission.REPORT_CREATE (create custom reports)
```

### System Admin Permissions
```java
// What an ADMIN can do:
- ALL permissions across all modules
- Permission.SYSTEM_ADMIN (system configuration)
- Permission.SYSTEM_MONITOR (system monitoring)
```

## Multi-Tenant Organization Access

```java
@Service
public class OrganizationAwareService {
    
    private final AuthorizationService authorizationService;
    
    public List<Invoice> getUserInvoices(String userId) {
        // Get user's organizations
        Set<String> userOrgs = getUserOrganizations(userId);
        
        // Filter invoices by accessible organizations
        return invoiceRepository.findAll().stream()
            .filter(invoice -> userOrgs.contains(invoice.getOrganizationId()))
            .filter(invoice -> authorizationService.hasPermission(Permission.BILLING_VIEW_INVOICES))
            .collect(Collectors.toList());
    }
    
    private Set<String> getUserOrganizations(String userId) {
        // This would typically come from the IAM module
        return organizationService.getUserOrganizations(userId);
    }
}
```

## Event-Driven Permission Updates

```java
// When user roles change in IAM, notify other modules
@EventListener
public class BillingPermissionUpdateHandler {
    
    @Autowired
    private BillingCacheService billingCacheService;
    
    @EventHandler
    public void handleUserRoleChanged(UserRoleChangedEvent event) {
        // Clear cached permissions for this user
        billingCacheService.clearUserPermissions(event.getUserId());
        
        // Update any billing-specific caches
        billingCacheService.refreshUserBillingAccess(event.getUserId());
    }
}
```

## Testing Authorization

```java
@Test
public void testBillingPermissions() {
    // Mock authorization service
    when(authorizationService.hasPermission(Permission.BILLING_PROCESS_PAYMENT))
        .thenReturn(true);
    when(authorizationService.canAccessOrganization("org-123"))
        .thenReturn(true);
    
    // Test controller
    ResponseEntity<?> response = billingController.processPayment(paymentRequest);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}

@Test
public void testInsufficientPermissions() {
    // Mock insufficient permissions
    when(authorizationService.hasPermission(Permission.BILLING_ADMIN))
        .thenReturn(false);
    
    // Test should fail
    ResponseEntity<?> response = billingController.adminOperation();
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

## Configuration

```yaml
# Application properties for authorization
regtech:
  authorization:
    cache:
      enabled: true
      ttl: 300 # 5 minutes
    multi-tenant:
      enabled: true
      default-organization: "default-org"
    permissions:
      strict-mode: true # Fail if permission not found
      audit-enabled: true # Log all permission checks
```

This architecture allows:
1. **IAM module** owns user roles and permissions
2. **Other modules** check permissions without knowing IAM internals
3. **Flexible permission system** that can grow with new modules
4. **Multi-tenant support** for organization-based access
5. **Event-driven updates** when permissions change
6. **Easy testing** with mockable authorization service