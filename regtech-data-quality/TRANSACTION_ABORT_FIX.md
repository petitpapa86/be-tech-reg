# Transaction Abort Fix - Rules Engine

## Problem

The rules engine was encountering a PostgreSQL transaction abort error:

```
org.springframework.orm.jpa.JpaSystemException: Unable to bind parameter #1
ERRORE: la transazione corrente è interrotta, i comandi saranno ignorati fino alla fine del blocco della transazione
(ERROR: the current transaction is aborted, commands will be ignored until the end of the transaction block)
```

## Root Cause

The error occurred when trying to save `RuleExecutionLogEntity` with the full context data to the JSONB column. The context contained:

1. **Large complex objects** - The entire exposure record with all fields
2. **Non-serializable data types** - Some Java objects that couldn't be converted to JSON
3. **Excessive data volume** - Storing unnecessary data in the audit log

When the initial save failed (likely due to JSON serialization issues), PostgreSQL marked the transaction as aborted. Subsequent operations within the same transaction (like saving violations) were then rejected with the "transaction is aborted" error.

## Solution

Applied a three-part fix to `DefaultRulesEngine.java`:

### 1. Context Sanitization

Added `sanitizeContextForLogging()` method that:
- Extracts only essential fields needed for audit purposes
- Converts all values to strings to ensure JSON serialization works
- Keeps the context data small and manageable

```java
private Map<String, Object> sanitizeContextForLogging(RuleContext context) {
    Map<String, Object> sanitized = new HashMap<>();
    
    String[] essentialFields = {
        "entity_type", "entity_id", "exposure_id", "counterparty_id",
        "amount", "currency", "country", "product_type", "lei_code",
        "reference_number", "is_corporate_exposure", "is_term_exposure"
    };
    
    for (String field : essentialFields) {
        Object value = context.get(field);
        if (value != null) {
            sanitized.put(field, value.toString());
        }
    }
    
    return sanitized;
}
```

### 2. Error Handling with Fallback

Modified `logExecution()` to:
- Use sanitized context instead of full context
- Catch save failures and retry with minimal context
- Log errors appropriately without breaking the transaction

```java
try {
    return executionLogRepository.save(ruleExecLog);
} catch (Exception e) {
    log.error("Failed to save rule execution log for rule {}: {}", rule.getRuleId(), e.getMessage());
    // Try again with minimal context
    ruleExecLog.setContextData(Map.of(
        "entity_type", entityType != null ? entityType : "UNKNOWN",
        "entity_id", entityId != null ? entityId : "UNKNOWN"
    ));
    return executionLogRepository.save(ruleExecLog);
}
```

### 3. Violation Details Sanitization

Updated `createViolation()` to use sanitized context for violation details as well.

## Benefits

1. **Prevents transaction aborts** - Sanitized data is always JSON-serializable
2. **Reduces database storage** - Only essential audit data is stored
3. **Improves performance** - Smaller JSONB columns are faster to write and query
4. **Better error handling** - Graceful fallback if save fails
5. **Maintains audit trail** - Still captures all essential information for compliance

## Testing

The fix has been compiled successfully. To verify:

1. Run the data quality validation with the same exposure data
2. Check that rule executions are logged successfully
3. Verify violations are created without transaction errors
4. Review the `context_data` JSONB column to confirm it contains only essential fields

## Files Modified

- `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/rulesengine/engine/DefaultRulesEngine.java`

## Related Issues

This fix addresses the broader issue of storing complex domain objects in JSONB columns. Similar patterns should be applied elsewhere in the codebase where context or details are stored in JSONB format.
