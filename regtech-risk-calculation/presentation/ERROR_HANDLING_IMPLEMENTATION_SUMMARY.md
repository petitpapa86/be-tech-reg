# Risk Calculation Presentation Layer Error Handling - Implementation Summary

## Overview

This document summarizes the implementation of custom exceptions and error handlers for the risk calculation presentation layer, completing Task 10 from the implementation plan.

## Requirements Addressed

- **Requirement 2.5**: Controllers encounter errors and use proper HTTP status codes and error responses
- **Requirement 7.1**: Validation errors return 400 Bad Request with detailed error messages
- **Requirement 7.2**: Business logic errors return appropriate 4xx status codes
- **Requirement 7.3**: System errors return 500 Internal Server Error without exposing internal details
- **Requirement 7.4**: Errors include error codes, messages, and timestamps
- **Requirement 7.5**: Errors are logged with sufficient context for debugging

## Implementation Details

### 1. Custom Exceptions

#### BatchNotFoundException
**File**: `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exceptions/BatchNotFoundException.java`

- Thrown when a requested batch cannot be found
- Results in HTTP 404 Not Found response
- Includes the batch ID that was not found
- Provides context for logging and error responses

**Usage Example**:
```java
throw new BatchNotFoundException(batchId, 
    String.format("Portfolio analysis not found for batch: %s", batchId));
```

#### CalculationNotCompletedException
**File**: `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exceptions/CalculationNotCompletedException.java`

- Thrown when calculation is requested but not yet complete
- Results in HTTP 202 Accepted response (processing in progress)
- Includes batch ID and current processing state
- Indicates to clients that they should retry later

**Usage Example**:
```java
throw new CalculationNotCompletedException(batchId, ProcessingState.IN_PROGRESS,
    String.format("Calculation for batch %s is not yet complete", batchId));
```

### 2. Global Error Handler

#### RiskCalculationErrorHandler
**File**: `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exceptions/RiskCalculationErrorHandler.java`

A @ControllerAdvice component that provides centralized exception handling for all risk calculation endpoints.

**Exception Handlers**:

1. **BatchNotFoundException** → HTTP 404 Not Found
   - Error code: `BATCH_NOT_FOUND`
   - Includes batch ID in metadata
   - Logs as warning level

2. **CalculationNotCompletedException** → HTTP 202 Accepted
   - Error code: `CALCULATION_NOT_COMPLETE`
   - Includes batch ID, current state, and retry-after suggestion
   - Logs as info level (not an error, just processing)

3. **MappingException** → HTTP 500 Internal Server Error
   - Error code: `MAPPING_ERROR`
   - Generic message to client (no internal details exposed)
   - Logs full exception details for debugging

4. **IllegalArgumentException** → HTTP 400 Bad Request
   - Error code: `INVALID_REQUEST`
   - Includes specific validation error message
   - Logs as warning level

5. **Exception** (catch-all) → HTTP 500 Internal Server Error
   - Error code: `SYSTEM_ERROR`
   - Generic message: "An unexpected error occurred. Please try again later."
   - Logs full exception details including type and stack trace

**Error Response Format**:
All errors follow the consistent ApiResponse format:
```json
{
  "success": false,
  "message": "Error message",
  "type": "ERROR_TYPE",
  "meta": {
    "timestamp": "2024-12-02T10:30:00Z",
    "version": "1.0",
    "apiVersion": "v1",
    "errorCode": "ERROR_CODE",
    "batchId": "batch-123",
    "currentState": "IN_PROGRESS",
    "retryAfter": 30
  }
}
```

### 3. Controller Updates

Updated controllers to use the new exception handling approach:

#### PortfolioAnalysisController
**File**: `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/analysis/PortfolioAnalysisController.java`

**Changes**:
- Removed try-catch blocks (handled by @ControllerAdvice)
- Throw `BatchNotFoundException` when portfolio analysis not found
- Simplified controller methods to focus on business logic
- Improved type safety with specific return types

**Before**:
```java
public ResponseEntity<?> getPortfolioAnalysis(@PathVariable String batchId) {
    try {
        Optional<PortfolioAnalysisResponseDTO> analysis = 
            portfolioAnalysisQueryService.getPortfolioAnalysis(batchId);
        
        if (analysis.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analysis.get());
    } catch (Exception e) {
        return handleSystemError(e);
    }
}
```

**After**:
```java
public ResponseEntity<PortfolioAnalysisResponseDTO> getPortfolioAnalysis(@PathVariable String batchId) {
    PortfolioAnalysisResponseDTO analysis = portfolioAnalysisQueryService.getPortfolioAnalysis(batchId)
        .orElseThrow(() -> new BatchNotFoundException(batchId, 
            String.format("Portfolio analysis not found for batch: %s", batchId)));
    
    return ResponseEntity.ok(analysis);
}
```

#### ExposureResultsController
**File**: `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exposures/ExposureResultsController.java`

**Changes**:
- Removed try-catch blocks
- Removed custom ErrorResponse record (using global error handling)
- Simplified parameter validation
- Improved type safety with specific return types

### 4. HTTP Status Code Mapping

| Exception | HTTP Status | Error Code | Use Case |
|-----------|-------------|------------|----------|
| BatchNotFoundException | 404 Not Found | BATCH_NOT_FOUND | Batch doesn't exist |
| CalculationNotCompletedException | 202 Accepted | CALCULATION_NOT_COMPLETE | Processing in progress |
| MappingException | 500 Internal Server Error | MAPPING_ERROR | Data transformation failure |
| IllegalArgumentException | 400 Bad Request | INVALID_REQUEST | Invalid parameters |
| Exception (generic) | 500 Internal Server Error | SYSTEM_ERROR | Unexpected errors |

### 5. Logging Strategy

**Log Levels**:
- **WARN**: Expected errors (batch not found, invalid parameters)
- **INFO**: Normal processing states (calculation not complete)
- **ERROR**: System errors and unexpected exceptions

**Log Context**:
All log entries include:
- Batch ID (when applicable)
- Request path
- Error message
- Exception details (for errors)
- Processing state (for calculation not complete)

**Example Log Entries**:
```
WARN  - Batch not found: batchId=batch-123, path=uri=/api/v1/risk-calculation/portfolio-analysis/batch-123
INFO  - Calculation not yet complete: batchId=batch-456, state=IN_PROGRESS, path=uri=/api/v1/risk-calculation/exposures/batch-456/classified
ERROR - Mapping error occurred: message=Failed to map exposure, path=uri=/api/v1/risk-calculation/exposures/batch-789/protected
```

## Security Considerations

1. **No Internal Details Exposed**: System errors return generic messages to clients while logging full details
2. **Consistent Error Format**: All errors follow the same structure, preventing information leakage
3. **Appropriate Status Codes**: Correct HTTP status codes help clients handle errors appropriately
4. **Detailed Logging**: Full context logged for security monitoring and debugging

## Testing Recommendations

### Unit Tests
- Test each exception handler returns correct HTTP status code
- Test error response format includes all required fields
- Test logging behavior for each exception type
- Test metadata includes appropriate context

### Integration Tests
- Test end-to-end error scenarios
- Test batch not found returns 404
- Test calculation in progress returns 202
- Test invalid parameters return 400
- Test system errors return 500 with generic message

## Files Created

1. `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exceptions/BatchNotFoundException.java`
   - Custom exception for missing batches

2. `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exceptions/CalculationNotCompletedException.java`
   - Custom exception for incomplete calculations

3. `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exceptions/RiskCalculationErrorHandler.java`
   - Global exception handler with @ControllerAdvice

4. `regtech-risk-calculation/presentation/ERROR_HANDLING_IMPLEMENTATION_SUMMARY.md`
   - This document

## Files Modified

1. `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/analysis/PortfolioAnalysisController.java`
   - Updated to use new exception handling

2. `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/exposures/ExposureResultsController.java`
   - Updated to use new exception handling

## Benefits

1. **Consistency**: All endpoints return errors in the same format
2. **Maintainability**: Centralized error handling reduces code duplication
3. **Type Safety**: Controllers use specific return types instead of wildcards
4. **Clarity**: Clear separation between different error types
5. **Debugging**: Comprehensive logging with context
6. **Client Experience**: Appropriate HTTP status codes and helpful error messages

## Next Steps

1. ✅ Custom exceptions implemented
2. ✅ Global error handler implemented with @ControllerAdvice
3. ✅ Controllers updated to use new exceptions
4. ✅ Logging implemented with appropriate levels and context
5. ⏭️ Write unit tests for error handlers
6. ⏭️ Write integration tests for error scenarios
7. ⏭️ Update API documentation with error responses

## Conclusion

Task 10 has been successfully completed. The risk calculation presentation layer now has:
- Custom exceptions for domain-specific error scenarios
- A global error handler that provides consistent error responses
- Proper HTTP status code mapping
- Comprehensive logging with context
- Updated controllers that leverage the new error handling

The implementation follows the established patterns from other modules (IAM, core) and provides a production-ready error handling system that is secure, maintainable, and user-friendly.
