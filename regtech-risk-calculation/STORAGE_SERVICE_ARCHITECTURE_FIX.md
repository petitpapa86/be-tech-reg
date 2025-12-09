# Storage Service Architecture Fix

## Issue

The `CalculationResultsStorageServiceImpl` in the infrastructure layer was depending on the application layer, violating the dependency inversion principle of clean architecture.

**Problem**:
- Infrastructure layer → Application layer (WRONG!)
- `CalculationResultsStorageServiceImpl` imported:
  - `CalculationResultsJsonSerializer` (application)
  - `RiskCalculationResult` (application)
  - `CalculationResultsSerializationException` (application)
  - `CalculationResultsDeserializationException` (application)

## Solution

Moved the storage service interface and exceptions to the domain layer, and simplified the infrastructure implementation to only handle file storage concerns.

### Architecture Changes

**Before** (Incorrect):
```
Application Layer:
  - ICalculationResultsStorageService (interface)
  - CalculationResultsJsonSerializer
  - RiskCalculationResult
  - Serialization/Deserialization Exceptions
  
Infrastructure Layer:
  - CalculationResultsStorageServiceImpl
    ↓ (depends on application layer - WRONG!)
```

**After** (Correct):
```
Domain Layer:
  - ICalculationResultsStorageService (interface)
  - CalculationResultsSerializationException
  - CalculationResultsDeserializationException (already existed)
  
Application Layer:
  - CalculationResultsJsonSerializer (uses domain interface)
  - RiskCalculationResult
  
Infrastructure Layer:
  - CalculationResultsStorageServiceImpl (implements domain interface)
    ↑ (depends only on domain layer - CORRECT!)
```

### Changes Made

1. **Created Domain Interface**: `domain/storage/ICalculationResultsStorageService`
   - Simplified to work with JSON strings instead of application DTOs
   - Methods:
     - `storeCalculationResults(String jsonContent, String batchId)` 
     - `retrieveCalculationResultsJson(String batchId)`
     - `retrieveCalculationResultsRaw(String batchId)`

2. **Created Domain Exception**: `domain/storage/CalculationResultsSerializationException`
   - Moved from application layer to domain layer
   - Matches existing `CalculationResultsDeserializationException` in domain

3. **Updated Infrastructure Implementation**:
   - Removed dependency on `CalculationResultsJsonSerializer`
   - Removed dependency on `RiskCalculationResult`
   - Now only handles file storage and URI management
   - Serialization is handled by application layer before calling storage service

### Responsibility Separation

**Domain Layer** (ICalculationResultsStorageService):
- Defines the contract for storing/retrieving JSON files
- Knows about: JSON strings, batch IDs, storage URIs
- Does NOT know about: Application DTOs, serialization logic

**Application Layer** (CalculationResultsJsonSerializer):
- Handles serialization of `RiskCalculationResult` to JSON
- Calls domain storage service with serialized JSON
- Handles deserialization of JSON to `RiskCalculationResult`

**Infrastructure Layer** (CalculationResultsStorageServiceImpl):
- Implements domain storage interface
- Manages file storage operations
- Manages URI lookup from database
- Does NOT know about: Application DTOs, serialization logic

### Benefits

1. ✅ **Correct Dependencies**: Infrastructure → Domain (not Application)
2. ✅ **Single Responsibility**: Storage service only handles storage, not serialization
3. ✅ **Testability**: Can test storage independently of serialization
4. ✅ **Flexibility**: Can swap serialization strategies without changing storage
5. ✅ **Clean Architecture**: Follows dependency inversion principle

### Migration Notes

**Application Layer Usage**:
```java
// Before (WRONG - infrastructure depends on application):
Result<String> uri = storageService.storeCalculationResults(result);

// After (CORRECT - application uses domain interface):
String json = jsonSerializer.serialize(result);
Result<String> uri = storageService.storeCalculationResults(json, result.batchId());
```

**Retrieval**:
```java
// Before:
Result<RiskCalculationResult> result = storageService.retrieveCalculationResults(batchId);

// After:
Result<String> json = storageService.retrieveCalculationResultsJson(batchId);
RiskCalculationResult result = jsonSerializer.deserialize(json.value());
```

## Verification

The architecture now follows clean architecture principles:
- Domain layer defines interfaces
- Application layer implements business logic using domain interfaces
- Infrastructure layer implements domain interfaces
- Dependencies flow inward: Infrastructure → Domain ← Application

**Compilation**: ✅ No circular dependencies
**Architecture**: ✅ Follows dependency inversion principle
**Testability**: ✅ Each layer can be tested independently
