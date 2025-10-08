# Role Architecture Consolidation

## Problem Solved

We had duplicate role definitions:
- `regtech-core/security/authorization/Role.java` - System-level roles
- `regtech-iam/domain/users/Bcbs239Role.java` - Business-specific roles

This created confusion and potential inconsistencies.

## Solution: Layered Role Architecture

### 1. **Core Layer** (`regtech-core`)
- **Purpose**: Cross-module authorization and technical permissions
- **Contains**: System roles with fine-grained permissions
- **Used by**: All modules for authorization checks

### 2. **Domain Layer** (`regtech-iam`)
- **Purpose**: Business-specific role definitions and mappings
- **Contains**: RegTech BCBS239 business roles
- **Used by**: Business logic and user management

### 3. **Mapping Layer** (`regtech-iam`)
- **Purpose**: Bridge between business and technical roles
- **Contains**: `RoleMapping` service
- **Used by**: Converting between business and system roles

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Core Authorization                        │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │   Permission    │    │           Role                  │ │
│  │                 │    │                                 │ │
│  │ • USER_READ     │    │ • BCBS239_VIEWER               │ │
│  │ • BCBS239_*     │    │ • BCBS239_DATA_ANALYST         │ │
│  │ • BILLING_*     │    │ • BCBS239_COMPLIANCE_OFFICER   │ │
│  │ • SYSTEM_*      │    │ • ADMIN                        │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ uses
┌─────────────────────────────────────────────────────────────┐
│                  IAM Domain Layer                           │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  Bcbs239Role    │    │        RoleMapping              │ │
│  │                 │    │                                 │ │
│  │ • VIEWER        │◄───┤ • toCoreRole()                  │ │
│  │ • DATA_ANALYST  │    │ • fromCoreRole()                │ │
│  │ • RISK_MANAGER  │    │ • isBcbs239Role()               │ │
│  │ • COMPLIANCE_*  │    │                                 │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Permission Structure

### Core Permissions (Technical)
```java
// System-level permissions
Permission.USER_READ = "user:read"
Permission.SYSTEM_ADMIN = "system:admin"

// RegTech-specific permissions
Permission.BCBS239_UPLOAD_FILES = "bcbs239:upload-files"
Permission.BCBS239_GENERATE_REPORTS = "bcbs239:generate-reports"
Permission.BCBS239_MANAGE_VIOLATIONS = "bcbs239:manage-violations"
```

### Business Roles (Domain)
```java
// Business role with mapped permissions
Bcbs239Role.COMPLIANCE_OFFICER(4, Set.of(
    Permission.BCBS239_UPLOAD_FILES,
    Permission.BCBS239_GENERATE_REPORTS,
    Permission.BCBS239_MANAGE_VIOLATIONS,
    // ... more permissions
))
```

## Role Mapping Examples

### Business to Technical
```java
// User assigned BCBS239 business role
Bcbs239Role businessRole = Bcbs239Role.COMPLIANCE_OFFICER;

// Maps to core technical role
Role coreRole = RoleMapping.toCoreRole(businessRole);
// Result: Role.BCBS239_COMPLIANCE_OFFICER

// Core role has all technical permissions
Set<String> permissions = coreRole.getPermissions();
// Contains: bcbs239:upload-files, bcbs239:generate-reports, etc.
```

### Authorization Check
```java
// Authorization service uses core roles
@PostMapping("/upload-data")
@RequiresPermission(Permission.BCBS239_UPLOAD_FILES)
public ResponseEntity<?> uploadData() {
    // Business logic
}

// Or programmatic check
if (authorizationService.hasPermission(Permission.BCBS239_UPLOAD_FILES)) {
    // Allow upload
}
```

## Usage Patterns

### 1. **User Role Assignment** (Business Layer)
```java
// Assign business role to user
UserRole userRole = UserRole.createFromBcbs239Role(
    userId, 
    Bcbs239Role.COMPLIANCE_OFFICER, 
    "bank-123"
);

// Internally maps to core role
Role coreRole = userRole.getRole(); // BCBS239_COMPLIANCE_OFFICER
```

### 2. **Authorization Check** (Technical Layer)
```java
// Authorization service works with core roles
public boolean hasPermission(String permission) {
    Set<String> userPermissions = getCurrentUserPermissions();
    return userPermissions.contains(permission);
}
```

### 3. **Cross-Module Communication**
```java
// Billing module checks RegTech permissions
@PostMapping("/billing/regtech-reports")
public ResponseEntity<?> generateBillingReport() {
    if (!authorizationService.hasPermission(Permission.BCBS239_GENERATE_REPORTS)) {
        return ResponseEntity.status(403).body("Access denied");
    }
    // Generate report
}
```

## Benefits

### 1. **Clear Separation of Concerns**
- **Business logic** uses `Bcbs239Role` for domain operations
- **Technical authorization** uses core `Role` for permission checks
- **Clean mapping** between business and technical concepts

### 2. **Cross-Module Compatibility**
- All modules use same core `Permission` constants
- Consistent authorization across bounded contexts
- Easy to add new modules with their own business roles

### 3. **Maintainability**
- Single source of truth for permissions in core
- Business roles can evolve independently
- Clear mapping between layers

### 4. **Flexibility**
- Easy to add new RegTech roles
- Can support multiple business domains (BCBS239, CCAR, etc.)
- Extensible permission system

## Migration Guide

### From Old Structure
```java
// Before - mixed roles
@Autowired
private UserRoleRepository userRoleRepository;

// After - use consolidated approach
@Autowired
private JpaUserRepository userRepository;

// Business role assignment
UserRole userRole = UserRole.createFromBcbs239Role(
    userId, Bcbs239Role.COMPLIANCE_OFFICER, organizationId
);

// Technical authorization
if (authorizationService.hasPermission(Permission.BCBS239_MANAGE_VIOLATIONS)) {
    // Business logic
}
```

## Future Extensions

### 1. **Additional Business Domains**
```java
// Can add other regulatory frameworks
public enum CcarRole {
    STRESS_TEST_ANALYST,
    CAPITAL_PLANNER,
    // Maps to core roles via CcarRoleMapping
}
```

### 2. **Dynamic Permissions**
```java
// Can add runtime permission configuration
public class DynamicPermissionService {
    public Set<String> getCustomPermissions(String organizationId) {
        // Load org-specific permissions
    }
}
```

### 3. **Role Hierarchies**
```java
// Can implement role inheritance
public class RoleHierarchy {
    public boolean inheritsFrom(Role child, Role parent) {
        // Check if child role inherits parent permissions
    }
}
```

This consolidated architecture provides a solid foundation for authorization that scales across multiple bounded contexts while maintaining clear separation between business and technical concerns.

## Why Roles Are in Core/IAM and NOT in Billing

### The Core Principle: Separation of Responsibilities

Understanding **why** roles are distributed this way is crucial for maintaining clean architecture:

#### **IAM Bounded Context** = "Who can do what?"
- **Owns**: User identity, authentication, role assignment
- **Responsibility**: Managing users and their roles
- **Domain**: Identity and Access Management

#### **Core Module** = "What can be done?"
- **Owns**: System-wide permissions and authorization rules
- **Responsibility**: Defining what actions exist in the system
- **Domain**: Cross-cutting security concerns

#### **Billing Bounded Context** = "How to charge for services?"
- **Owns**: Payments, invoices, subscriptions
- **Responsibility**: Financial transactions
- **Domain**: Billing and payments

### What Would Break If Roles Were in Billing

#### ❌ **Bad Design - Roles in Billing:**
```java
// regtech-billing/BillingRole.java
public enum BillingRole {
    BILLING_ADMIN,
    PAYMENT_PROCESSOR,
    INVOICE_VIEWER
}

// Problem 1: What about users who don't use billing?
// Problem 2: How does IAM know about billing roles?
// Problem 3: How does reporting module check billing permissions?
```

**Critical Issues:**
1. **Circular Dependency**: IAM would need to know about billing roles
2. **Tight Coupling**: Every module would depend on billing for authorization
3. **Single Responsibility Violation**: Billing doing both payments AND authorization
4. **Scalability**: Adding new modules becomes complex

#### ✅ **Good Design - Current Architecture:**
```java
// Core: Defines WHAT can be done (permissions)
Permission.BILLING_PROCESS_PAYMENT = "billing:process-payment"
Permission.BCBS239_UPLOAD_FILES = "bcbs239:upload-files"

// Core: Defines WHO can do it (system roles)
Role.BILLING_ADMIN = has billing permissions
Role.BCBS239_COMPLIANCE_OFFICER = has RegTech permissions

// IAM: Manages WHO gets assigned roles
UserRole userRole = UserRole.create(userId, Role.BILLING_ADMIN, orgId);

// Billing: Checks permissions for business logic
@RequiresPermission(Permission.BILLING_PROCESS_PAYMENT)
public void processPayment() { ... }
```

### Architecture Responsibilities Visualization

```
┌─────────────────────────────────────────────────────────────┐
│                        CORE MODULE                          │
│  "What actions exist in the system?"                       │
│                                                             │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │   Permission    │    │           Role                  │ │
│  │                 │    │                                 │ │
│  │ • billing:*     │    │ • BILLING_ADMIN                │ │
│  │ • bcbs239:*     │    │ • BCBS239_COMPLIANCE_OFFICER   │ │
│  │ • report:*      │    │ • REPORT_VIEWER                │ │
│  │ • user:*        │    │ • USER                         │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ uses
┌─────────────────────────────────────────────────────────────┐
│                     IAM MODULE                              │
│  "Who gets assigned what roles?"                           │
│                                                             │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │      User       │    │        UserRole                 │ │
│  │                 │    │                                 │ │
│  │ • john@bank.com │◄───┤ • userId + Role.BILLING_ADMIN   │ │
│  │ • jane@bank.com │    │ • userId + Role.BCBS239_VIEWER  │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ checks permissions
┌─────────────────────────────────────────────────────────────┐
│                   BILLING MODULE                            │
│  "How to process payments?"                                 │
│                                                             │
│  @RequiresPermission(Permission.BILLING_PROCESS_PAYMENT)    │
│  public void processPayment() {                             │
│      // Business logic for payment processing              │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

### Real-World Authorization Flow

**Scenario: "Jane needs to process a payment"**

1. **IAM Module** (User Management):
   ```java
   // Admin assigns Jane a billing role
   User jane = userRepository.findByEmail("jane@bank.com");
   UserRole janeRole = UserRole.create(
       jane.getId(), 
       Role.BILLING_ADMIN,  // Core role
       "bank-123"
   );
   ```

2. **Core Module** (Permission Definition):
   ```java
   // Core defines what BILLING_ADMIN can do
   Role.BILLING_ADMIN = Set.of(
       Permission.BILLING_PROCESS_PAYMENT,
       Permission.BILLING_VIEW_INVOICES,
       Permission.BILLING_MANAGE_SUBSCRIPTIONS
   );
   ```

3. **Billing Module** (Business Logic):
   ```java
   // Billing checks permission before processing
   @PostMapping("/process-payment")
   @RequiresPermission(Permission.BILLING_PROCESS_PAYMENT)
   public ResponseEntity<?> processPayment() {
       // Jane can access this because:
       // 1. IAM assigned her BILLING_ADMIN role
       // 2. Core defines BILLING_ADMIN has BILLING_PROCESS_PAYMENT
       // 3. Authorization service validates the chain
   }
   ```

4. **Authorization Flow**:
   ```java
   // When Jane calls /process-payment:
   authorizationService.hasPermission("billing:process-payment")
   → Gets Jane's roles from IAM
   → Checks if BILLING_ADMIN role has "billing:process-payment" 
   → Returns true → Access granted
   ```

### Why This Design Works

#### 1. **Single Responsibility**
- **IAM**: "I manage users and assign roles"
- **Core**: "I define what roles can do"  
- **Billing**: "I process payments (if user has permission)"

#### 2. **Loose Coupling**
- Billing doesn't know about users or roles
- IAM doesn't know about payment processing
- Core provides the contract between them

#### 3. **Scalability**
```java
// Easy to add new modules
// regtech-reporting/ReportController.java
@RequiresPermission(Permission.REPORT_VIEW)  // Uses core permission
public ResponseEntity<?> generateReport() {
    // No dependency on IAM or Billing
}
```

#### 4. **Cross-Module Authorization**
```java
// Billing can check RegTech permissions
@PostMapping("/billing/compliance-report")
public ResponseEntity<?> generateComplianceReport() {
    if (!authorizationService.hasPermission(Permission.BCBS239_GENERATE_REPORTS)) {
        return ResponseEntity.status(403).body("Need compliance role");
    }
    // Generate billing report for compliance
}
```

### What Would Break With Wrong Design

#### Problem 1: Circular Dependencies
```java
// IAM would need to import billing
import com.bcbs239.regtech.billing.BillingRole; // ❌ Wrong direction

// Reporting would need billing for authorization  
import com.bcbs239.regtech.billing.BillingRole; // ❌ Why does reporting need billing?
```

#### Problem 2: Tight Coupling
```java
// Every module becomes dependent on billing
regtech-iam → depends on → regtech-billing
regtech-reporting → depends on → regtech-billing  
regtech-compliance → depends on → regtech-billing
// This violates bounded context independence
```

#### Problem 3: Business Logic Mixing
```java
// Billing module would have two responsibilities:
// 1. Process payments (its job)
// 2. Define who can do what (not its job)
```

### Summary: The Architecture "Why"

The role architecture follows **Domain-Driven Design** principles:

- **Core**: Shared kernel for cross-cutting concerns (authorization)
- **IAM**: Bounded context for identity management  
- **Billing**: Bounded context for payment processing
- **Clean separation**: Each module has one responsibility
- **Loose coupling**: Modules communicate through well-defined contracts

This design allows you to:
- ✅ Add new modules without changing existing ones
- ✅ Scale authorization independently of business logic
- ✅ Maintain clean bounded context boundaries
- ✅ Support complex cross-module permissions
- ✅ Follow single responsibility principle
- ✅ Avoid circular dependencies

**The roles are in Core (technical permissions) and IAM (user management) because that's where they belong according to their responsibilities, not in Billing which should focus on payments.**