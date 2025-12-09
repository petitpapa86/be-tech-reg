# Domain Value Objects Created for Ingestion Module

This document summarizes the new value objects created to replace primitive types in the ingestion module, following Domain-Driven Design (DDD) principles.

## Created Value Objects

### 1. ProgressPercentage
**Location**: `regtech-ingestion/domain/src/main/java/com/bcbs239/regtech/ingestion/domain/batch/ProgressPercentage.java`

**Purpose**: Replaces primitive `int` for progress percentage tracking.

**Key Features**:
- Validates percentage is between 0-100
- Factory methods: `create()`, `completed()`, `notStarted()`
- Helper methods: `isComplete()`, `hasStarted()`, `remaining()`, `asDecimal()`
- Human-readable string representation with "%" suffix

**Example Usage**:
```java
ProgressPercentage progress = ProgressPercentage.create(75).getValue().orElseThrow();
if (progress.isComplete()) {
    // Handle completion
}
int remaining = progress.remaining(); // Returns 25
```

---

### 2. ProcessingDuration
**Location**: `regtech-ingestion/domain/src/main/java/com/bcbs239/regtech/ingestion/domain/batch/ProcessingDuration.java`

**Purpose**: Replaces primitive `Long` for duration tracking in milliseconds.

**Key Features**:
- Validates duration is non-negative
- Factory methods: `create()`, `between()`, `fromStart()`, `zero()`
- Conversion methods: `toSeconds()`, `toMinutes()`, `toHours()`, `toDuration()`
- Comparison methods: `exceeds()`, `isZero()`
- Arithmetic: `plus()`
- Human-readable format (e.g., "2.5m", "45.3s")

**Example Usage**:
```java
ProcessingDuration duration = ProcessingDuration.between(startTime, endTime).getValue().orElseThrow();
String readable = duration.toHumanReadable(); // "2.5m"
if (duration.exceeds(maxDuration)) {
    // Handle slow processing
}
```

---

### 3. FileSizeBytes
**Location**: `regtech-ingestion/domain/src/main/java/com/bcbs239/regtech/ingestion/domain/batch/FileSizeBytes.java`

**Purpose**: Replaces primitive `long` for file size in bytes.

**Key Features**:
- Validates file size is non-negative
- Factory methods: `create()`, `fromMB()`, `fromKB()`, `zero()`
- Conversion methods: `toKB()`, `toMB()`, `toGB()`
- Comparison methods: `exceeds()`, `isWithinLimit()`
- Arithmetic: `plus()`
- Utility: `multiplierRelativeTo()` for size-based calculations
- Human-readable format (e.g., "45.32 MB", "1.2 GB")

**Example Usage**:
```java
FileSizeBytes fileSize = FileSizeBytes.create(52428800).getValue().orElseThrow();
double mb = fileSize.toMB(); // 50.0
double multiplier = fileSize.multiplierRelativeTo(FileSizeBytes.fromMB(10)); // 5.0
```

---

### 4. ProcessingStage
**Location**: `regtech-ingestion/domain/src/main/java/com/bcbs239/regtech/ingestion/domain/batch/ProcessingStage.java`

**Purpose**: Replaces primitive `String` for user-friendly stage names.

**Key Features**:
- Encapsulates mapping from `BatchStatus` to human-readable stages
- Pre-defined constants: `QUEUED`, `PARSING`, `ENRICHING`, `STORING`, `COMPLETED`, `FAILED`
- Factory methods: `create()`, `fromBatchStatus()`
- State checking: `isTerminal()`, `isProcessing()`, `isSuccessful()`, `isFailure()`

**Example Usage**:
```java
ProcessingStage stage = ProcessingStage.fromBatchStatus(BatchStatus.VALIDATED);
// stage.value() returns "Enriching"
if (stage.isProcessing()) {
    // Show progress indicator
}
```

---

### 5. ThroughputMetrics
**Location**: `regtech-ingestion/domain/src/main/java/com/bcbs239/regtech/ingestion/domain/batch/ThroughputMetrics.java`

**Purpose**: Replaces primitive calculations for `recordsPerSecond` and `megabytesPerSecond`.

**Key Features**:
- Encapsulates throughput calculations
- Factory methods: `create()`, `calculate()`, `zero()`
- Validation: ensures both metrics are non-negative
- Comparison: `meetsThreshold()`, `compareTo()`
- Automatic rounding to 2 decimal places
- Human-readable format (e.g., "1234.56 records/sec, 45.32 MB/sec")

**Example Usage**:
```java
ThroughputMetrics metrics = ThroughputMetrics.calculate(
    10000,  // records
    FileSizeBytes.fromMB(100),
    ProcessingDuration.create(30000).getValue().orElseThrow()
).getValue().orElseThrow();

// metrics.recordsPerSecond() returns 333.33
// metrics.megabytesPerSecond() returns 3.33
```

---

## Benefits of Using Value Objects

### 1. **Type Safety**
- Cannot accidentally mix up different numeric types
- Compiler catches errors at compile time

### 2. **Domain Semantics**
- Code is self-documenting
- `ProgressPercentage` is clearer than `int`
- `FileSizeBytes` is clearer than `long`

### 3. **Encapsulation**
- Business rules and validations are centralized
- Conversion logic is in one place
- Reduces code duplication

### 4. **Immutability**
- All value objects are immutable records
- Thread-safe by design
- Prevents accidental modification

### 5. **Rich Behavior**
- Value objects provide domain-specific methods
- Helper methods reduce boilerplate in application code
- Human-readable representations for logging

---

## Migration Impact

### Application Layer
The `BatchStatusQueryHandler` and `BatchStatusDto` should be updated to use these value objects instead of primitives.

### Domain Layer
The `IngestionBatch` aggregate and related entities can leverage these value objects for type safety.

### Infrastructure Layer
Persistence mappings may need to extract primitive values for database storage.

---

## Next Steps

1. Update `BatchStatusQueryHandler` to use new value objects
2. Update `BatchStatusDto` to expose value objects
3. Update `IngestionBatch` domain model if applicable
4. Add unit tests for each value object
5. Update documentation and API contracts

---

## Design Principles Applied

✅ **Domain-Driven Design**: Value objects model domain concepts  
✅ **Ubiquitous Language**: Names match business terminology  
✅ **Immutability**: Records ensure thread-safety  
✅ **Validation**: Factory methods enforce business rules  
✅ **Single Responsibility**: Each object has one clear purpose  
✅ **Rich Domain Model**: Objects contain behavior, not just data
