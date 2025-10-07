# Architecture Cleanup Final - Design Document

## Overview

This design document outlines the comprehensive approach to eliminate all remaining architecture anti-patterns in the billing domain. The cleanup will ensure complete compliance with domain-driven design and functional programming principles.

## Architecture

### Current State Analysis

The billing domain currently contains several architectural violations:

1. **Domain Services**: `TaxCalculationService` and related services exist in domain layer
2. **Interface Dependencies**: `SimpleEmailService` interface exists in domain
3. **Result Objects**: Various result objects exist in domain packages (e.g., `LegalNoticeResult`)
4. **Empty Directories**: Some directories may become empty after cleanup

### Target Architecture

The target architecture will have:

1. **Pure Domain Layer**: Only domain entities, value objects, and repositories
2. **Functional Closures**: No interfaces, only function types for external dependencies
3. **Application Handlers**: All business logic in application layer handlers
4. **Clean Structure**: No empty directories or unused files

## Components and Interfaces

### Domain Services Cleanup

**Services to Remove/Move:**
- `TaxCalculationService` → Check if `CalculateTaxHandler` exists, remove if duplicate
- `TaxCalculationServiceInterface` → Remove interface, use functional closures
- Any other services in domain packages

**Approach:**
1. Identify all domain services
2. Check for existing application handlers
3. Remove duplicates or convert to handlers
4. Update all references

### Interface Elimination

**Interfaces to Replace:**
- `SimpleEmailService` → Replace with `Function<EmailRequest, Result<Void, ErrorDetail>>`
- Any other service interfaces in domain

**Functional Closure Pattern:**
```java
// Instead of interface
public interface SimpleEmailService {
    void sendEmail(String to, String subject, String body);
}

// Use functional closure
Function<EmailRequest, Result<Void, ErrorDetail>> sendEmail = emailRequest -> {
    // Implementation
};
```

### Result Objects Migration

**Objects to Move/Remove:**
- `LegalNoticeResult` → Move to application layer or remove if unused
- `CallScheduleResult` → Move to application layer or remove if unused
- Any other result objects in domain

**Migration Strategy:**
1. Identify all result objects in domain
2. Determine if they're used by application layer
3. Move to appropriate application package or remove
4. Update all references

## Data Models

### Functional Closure Types

```java
// Email sending closure
public record EmailRequest(String to, String subject, String body, String correlationId) {}
Function<EmailRequest, Result<Void, ErrorDetail>> sendEmail;

// Legal notice closure  
public record LegalNoticeRequest(BankId bankId, DunningLevel level, String correlationId) {}
Function<LegalNoticeRequest, Result<LegalNoticeResponse, ErrorDetail>> sendLegalNotice;

// Call scheduling closure
public record CallScheduleRequest(BankId bankId, LocalDateTime preferredTime, String correlationId) {}
Function<CallScheduleRequest, Result<CallScheduleResponse, ErrorDetail>> scheduleCall;
```

### Handler Patterns

```java
// Application layer handler
@Component
public class CalculateTaxHandler {
    
    private final Function<TaxCalculationRequest, Result<TaxCalculationResponse, ErrorDetail>> calculateTax;
    
    public CalculateTaxHandler(
        Function<TaxCalculationRequest, Result<TaxCalculationResponse, ErrorDetail>> calculateTax
    ) {
        this.calculateTax = calculateTax;
    }
    
    public Result<TaxCalculationResponse, ErrorDetail> handle(CalculateTaxCommand command) {
        // Handler implementation
    }
}
```

## Error Handling

### Result Pattern Consistency

All handlers and closures will use the consistent Result pattern:

```java
public sealed interface Result<T, E> permits Success<T, E>, Failure<T, E> {
    // Result implementation
}
```

### Error Types

Domain-specific error types will remain in domain:
- `BillingError` and `BillingErrorType` stay in domain
- Application-specific errors in application layer

## Testing Strategy

### Validation Approach

1. **Compilation Validation**: Ensure all code compiles after changes
2. **Architecture Tests**: Verify no domain services remain
3. **Functional Tests**: Ensure closures work correctly
4. **Integration Tests**: Verify handler functionality

### Test Updates

1. Update tests that reference moved/removed classes
2. Add tests for new functional closures
3. Verify handler tests still pass
4. Add architecture compliance tests

## Implementation Plan

### Phase 1: Analysis and Identification
1. Scan domain packages for services, interfaces, and result objects
2. Identify corresponding application handlers
3. Map dependencies and references
4. Create cleanup plan

### Phase 2: Service Cleanup
1. Remove duplicate domain services where handlers exist
2. Convert remaining services to handlers
3. Update all references and imports
4. Verify compilation

### Phase 3: Interface Elimination
1. Replace interfaces with functional closures
2. Update consuming classes to use Function types
3. Create factory methods for closures
4. Test functional implementations

### Phase 4: Result Object Migration
1. Move result objects to application layer
2. Remove unused result objects
3. Update all references
4. Verify domain purity

### Phase 5: Directory Cleanup
1. Remove empty directories
2. Clean up unused imports
3. Verify project structure
4. Run final validation

## Validation Criteria

### Architecture Compliance
- ✅ No services in domain layer
- ✅ No interfaces in domain layer  
- ✅ No result objects in domain layer
- ✅ All handlers in application layer
- ✅ Functional closures used consistently

### Code Quality
- ✅ All code compiles successfully
- ✅ No unused imports or references
- ✅ Consistent error handling patterns
- ✅ Clean directory structure
- ✅ Proper separation of concerns