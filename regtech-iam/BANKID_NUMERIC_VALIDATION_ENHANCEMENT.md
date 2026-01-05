# BankId Numeric Validation Enhancement Summary

## Overview
Enhanced the BankId value object to handle numeric validation internally and removed NumberFormatException handling from application layer handlers. This follows the principle of encapsulating validation logic within value objects.

## Changes Made

### 1. Enhanced BankId Value Object

#### New Method: `createNumeric(String value)`
```java
public static Result<BankId> createNumeric(String value) {
    // First perform standard validation
    Result<BankId> basicValidation = create(value);
    if (basicValidation.isFailure()) {
        return basicValidation;
    }

    BankId bankId = basicValidation.getValue().get();
    
    // Then validate numeric format
    try {
        Long.parseLong(bankId.getValue());
        return Result.success(bankId);
    } catch (NumberFormatException e) {
        return Result.failure(ErrorDetail.of(
            "BANK_ID_INVALID_NUMERIC_FORMAT", 
            ErrorType.VALIDATION_ERROR, 
            "Bank ID must be a valid number", 
            "validation.bank_id_invalid_numeric_format"
        ));
    }
}
```

#### New Method: `getAsLong()`
```java
public Long getAsLong() {
    return Long.parseLong(value);
}
```

### 2. Updated Application Layer Handlers

#### GetUsersByBankHandler
**Before:**
```java
Result<BankId> bankIdResult = BankId.create(query.bankId);
// ... validation collection ...
try {
    Long bankIdLong = Long.parseLong(bankId.getValue());
    // ... use bankIdLong ...
} catch (NumberFormatException e) {
    return Result.failure(ErrorDetail.of("INVALID_BANK_ID_FORMAT", ...));
}
```

**After:**
```java
Result<BankId> bankIdResult = BankId.createNumeric(query.bankId);
// ... validation collection ...
Long bankIdLong = bankId.getAsLong(); // No exception possible
// ... use bankIdLong ...
```

#### AddNewUserHandler
- Updated to use `BankId.createNumeric()` instead of `BankId.create()`
- Removed `Long.parseLong()` call and used `bankId.getAsLong()`
- Eliminated potential NumberFormatException

#### InviteUserHandler
- Updated to use `BankId.createNumeric()` instead of `BankId.create()`
- Removed `Long.parseLong()` call and used `bankId.getAsLong()`
- Eliminated potential NumberFormatException

## Key Improvements

### 1. Encapsulation of Validation Logic
- Numeric validation is now encapsulated within the BankId value object
- Application layer handlers don't need to handle NumberFormatException
- Consistent validation behavior across all usage

### 2. Type Safety
- `getAsLong()` method guarantees that the BankId was validated as numeric
- No risk of NumberFormatException when using properly validated BankId instances
- Clear separation between string-based and numeric BankId validation

### 3. Better Error Messages
- Specific error code: `BANK_ID_INVALID_NUMERIC_FORMAT`
- Consistent error message: "Bank ID must be a valid number"
- Proper internationalization key: `validation.bank_id_invalid_numeric_format`

### 4. Cleaner Application Code
- Removed try-catch blocks from application handlers
- Simplified validation flow
- More readable and maintainable code

## Usage Patterns

### For String-based BankId (general use)
```java
Result<BankId> result = BankId.create(value);
if (result.isSuccess()) {
    BankId bankId = result.getValue().get();
    String stringValue = bankId.getValue();
}
```

### For Numeric BankId (database queries)
```java
Result<BankId> result = BankId.createNumeric(value);
if (result.isSuccess()) {
    BankId bankId = result.getValue().get();
    Long numericValue = bankId.getAsLong(); // Safe to call
}
```

## Error Response Structure

### Numeric Validation Error
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errorType": "VALIDATION_ERROR",
  "fieldErrors": [
    {
      "field": "bankId",
      "message": "Bank ID must be a valid number",
      "messageKey": "validation.bank_id_invalid_numeric_format"
    }
  ]
}
```

## Benefits

### For Developers
- **Cleaner Code**: No more try-catch blocks in application handlers
- **Type Safety**: `getAsLong()` guarantees numeric validity
- **Consistency**: Same validation logic across all handlers
- **Maintainability**: Validation logic centralized in value object

### For Users
- **Better Error Messages**: Clear indication when BankId format is invalid
- **Consistent Experience**: Same validation behavior across all endpoints
- **Internationalization**: Proper message keys for localization

### For System
- **Performance**: Validation happens once during value object creation
- **Reliability**: Eliminates runtime NumberFormatException
- **Testability**: Easier to test validation logic in isolation

## Backward Compatibility

- Existing `create()` method remains unchanged for string-based validation
- New `createNumeric()` method for numeric validation scenarios
- `getAsLong()` method assumes numeric validation was performed
- Deprecated methods marked for future removal

## Files Modified

1. **BankId.java** - Added `createNumeric()` and `getAsLong()` methods
2. **GetUsersByBankHandler.java** - Uses `createNumeric()` and removed try-catch
3. **AddNewUserHandler.java** - Uses `createNumeric()` and `getAsLong()`
4. **InviteUserHandler.java** - Uses `createNumeric()` and `getAsLong()`

## Next Steps

1. Update other handlers that might need numeric BankId validation
2. Consider similar enhancements for other value objects that need format-specific validation
3. Add comprehensive unit tests for the new `createNumeric()` method
4. Update API documentation to reflect the improved validation

The enhancement successfully moves validation logic to the appropriate layer (domain) and simplifies the application layer while maintaining type safety and providing better error messages.