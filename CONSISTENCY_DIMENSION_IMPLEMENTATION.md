# DIMENSIONE 3: COERENZA (Consistency) - Implementation Guide

## Overview

This implementation adds **cross-field consistency checks** to the data quality validation framework. These checks validate that data is **uniform across different sources and over time**.

## Implementation Architecture

### Component Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                    Validation Flow                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ParallelExposureValidationCoordinator                          │
│  ├─ validateAll(exposures, validator, declaredCount, crmRefs)  │
│  │                                                               │
│  ├─ Step 1: Per-Exposure Validation                            │
│  │   └─ Execute rules from database for each exposure          │
│  │       (Completezza, Accuratezza, etc.)                       │
│  │                                                               │
│  ├─ Step 2: Cross-Field Consistency Checks                     │
│  │   └─ ConsistencyValidator.validate()                        │
│  │       ├─ Check 1: Conteggio Esposizioni                     │
│  │       ├─ Check 2: Mappatura CRM → Esposizioni              │
│  │       ├─ Check 3: LEI ↔ counterpartyId (1:1)               │
│  │       └─ Check 4: Coerenza Valutaria                        │
│  │                                                               │
│  └─ Return: ValidationBatchResult                               │
│      ├─ results: List<ValidationResults>                        │
│      ├─ exposureResults: Map<ExposureId, Result>               │
│      └─ consistencyResult: ConsistencyValidationResult         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## The Four Consistency Checks

### Check 1: Conteggio Esposizioni (Exposure Count Verification)

**Purpose**: Verify that declared `total_exposures` matches actual array length.

**Implementation**:
```java
private ConsistencyCheckResult checkExposureCount(
    List<ExposureRecord> exposures,
    Integer declaredCount
) {
    int actualCount = exposures.size();
    boolean passed = declaredCount.equals(actualCount);
    double score = passed ? 100.0 : 0.0;
    
    // Create violation if mismatch
    if (!passed) {
        builder.addViolation(ConsistencyViolation.builder()
            .violationType("COUNT_MISMATCH")
            .affectedEntity("Batch")
            .expectedValue(String.valueOf(declaredCount))
            .actualValue(String.valueOf(actualCount))
            .description("Il conteggio dichiarato non corrisponde al numero effettivo")
            .build());
    }
    
    return builder.build();
}
```

**Example Output**:
```
Check 1: Conteggio Esposizioni
Dichiarato (total_exposures): 4
Effettivo (array length): 4
Match: ✓ Coerente
Score: 100%
```

### Check 2: Mappatura CRM → Esposizioni (CRM to Exposure Mapping)

**Purpose**: Verify that every CRM reference corresponds to an existing exposure.

**Implementation**:
```java
private ConsistencyCheckResult checkCrmMapping(
    List<ExposureRecord> exposures,
    List<String> crmReferences
) {
    Set<String> exposureIds = exposures.stream()
        .map(ExposureRecord::exposureId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    List<ConsistencyViolation> violations = new ArrayList<>();
    int matchedCount = 0;

    for (int i = 0; i < crmReferences.size(); i++) {
        String crmRef = crmReferences.get(i);
        if (exposureIds.contains(crmRef)) {
            matchedCount++;
        } else {
            // Create violation for orphan CRM reference
            violations.add(ConsistencyViolation.builder()
                .violationType("ORPHAN_CRM_REFERENCE")
                .affectedEntity("CRM #" + (i + 1))
                .expectedValue("Esposizione esistente")
                .actualValue(crmRef)
                .description("CRM non trovato in array esposizioni")
                .build());
        }
    }

    double score = (matchedCount * 100.0) / crmReferences.size();
    boolean passed = score >= 90.0; // 90% threshold
    
    return builder.build();
}
```

**Example Output (Failure)**:
```
Check 2: Mappatura CRM → Esposizioni
CRM dichiarati: 4

CRM #1:
├─ exposure_id: "EXP_001_2024"
├─ Esposizione trovata: ✓
└─ Match: ✓

CRM #2:
├─ exposure_id: "EXP_003_2024"
├─ Ricerca in esposizioni array...
├─ Esposizione trovata: ❌ NON ESISTE!
└─ Errore: Riferimento orfano

CRM abbinati: 1/4 (25%)
CRM orfani: 3
Score: 25% FAIL
```

### Check 3: Relazione LEI ↔ counterpartyId (1:1 Relationship)

**Purpose**: Verify that same LEI always maps to same counterpartyId.

**Principle**: A LEI (Legal Entity Identifier) uniquely identifies a legal entity. Therefore, the same LEI must always have the same counterpartyId.

**Implementation**:
```java
private ConsistencyCheckResult checkLeiCounterpartyConsistency(
    List<ExposureRecord> exposures
) {
    // Group exposures by LEI
    Map<String, Set<String>> leiToCounterpartyIds = new HashMap<>();
    
    for (ExposureRecord exposure : exposures) {
        if (exposure.counterpartyLei() != null && !exposure.counterpartyLei().isBlank()) {
            leiToCounterpartyIds
                .computeIfAbsent(exposure.counterpartyLei(), k -> new HashSet<>())
                .add(exposure.counterpartyId());
        }
    }

    List<ConsistencyViolation> violations = new ArrayList<>();
    
    for (Map.Entry<String, Set<String>> entry : leiToCounterpartyIds.entrySet()) {
        String lei = entry.getKey();
        Set<String> counterpartyIds = entry.getValue();

        if (counterpartyIds.size() > 1) {
            // Multiple counterpartyIds for same LEI - violation!
            violations.add(ConsistencyViolation.builder()
                .violationType("LEI_COUNTERPARTY_MISMATCH")
                .affectedEntity("LEI: " + lei)
                .expectedValue("Stesso counterpartyId per stesso LEI")
                .actualValue("Multiple IDs: " + String.join(", ", counterpartyIds))
                .description("Stesso LEI deve sempre avere stesso counterpartyId")
                .build());
        }
    }
    
    return builder.build();
}
```

**Example Output (Failure)**:
```
Check 3: Relazione LEI ↔ counterpartyId (1:1)
Principio: Stesso LEI = Stesso counterpartyId

Analisi:
LEI "549300ABCDEF12345678" appare in:
├─ EXP_001: counterpartyId "CORP12345" ✓
├─ EXP_002: counterpartyId "CORP12345" ✓
├─ EXP_003: counterpartyId "CORP12344" ❌ DIVERSO!
└─ EXP_004: counterpartyId "CORP12344" ❌ DIVERSO!

Problema:
Stesso LEI (stessa entità legale)
MA counterpartyId diversi (CORP12345 vs CORP12344)

Impatto:
Sistema non riconoscerà EXP_003/004 come stessa controparte
→ Non aggregherà esposizioni
→ Calcolo GE ERRATO

Score: 50% FAIL
```

### Check 4: Coerenza Valutaria (Currency Consistency)

**Purpose**: Verify currency consistency across exposures.

**Implementation**:
```java
private ConsistencyCheckResult checkCurrencyConsistency(
    List<ExposureRecord> exposures
) {
    Map<String, Long> currencyDistribution = exposures.stream()
        .filter(e -> e.currency() != null)
        .collect(Collectors.groupingBy(
            ExposureRecord::currency,
            Collectors.counting()
        ));

    // Check if there's a dominant currency (>90% of exposures)
    Optional<Map.Entry<String, Long>> dominantCurrency = 
        currencyDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue());

    if (dominantCurrency.isPresent()) {
        double dominantPercentage = 
            (dominantCurrency.get().getValue() * 100.0) / totalWithCurrency;

        if (dominantPercentage < 90.0 && currencyDistribution.size() > 1) {
            // Multiple currencies without clear dominance
            score = dominantPercentage;
            passed = false;
            // Create violations...
        }
    }
    
    return builder.build();
}
```

## Usage Examples

### Basic Usage

```java
@Service
public class ValidationService {
    
    private final ParallelExposureValidationCoordinator coordinator;
    private final ExposureRuleValidator validator;

    public ValidationBatchResult validateBatch(
        List<ExposureRecord> exposures,
        Integer declaredCount,
        List<String> crmReferences
    ) {
        // Execute validation with consistency checks
        return coordinator.validateAll(
            exposures,
            validator,
            declaredCount,    // Optional: from batch metadata
            crmReferences     // Optional: from CRM system
        );
    }
}
```

### Backward Compatibility

The original `validateAll` method without consistency parameters is still available:

```java
// Old signature (still works)
ValidationBatchResult result = coordinator.validateAll(
    exposures,
    validator
);

// Result will have empty consistency checks (all pass with 100% score)
```

### Processing Results

```java
ValidationBatchResult result = validateBatch(...);

// Check overall consistency
if (!result.consistencyResult().allPassed()) {
    System.out.println("⚠ Consistency checks failed!");
    System.out.printf("Score: %.1f%%\n", 
        result.consistencyResult().overallScore());
    
    // Handle specific failures
    for (ConsistencyCheckResult check : result.consistencyResult().checks()) {
        if (!check.passed()) {
            handleConsistencyFailure(check);
        }
    }
}

// Access per-exposure validation results
result.results().forEach(validationResult -> {
    // Process individual exposure validation
});

// Access aggregated exposure results
result.exposureResults().forEach((exposureId, exposureResult) -> {
    if (!exposureResult.isValid()) {
        // Handle validation errors
    }
});
```

## Test Coverage

Complete test suite in `ConsistencyValidatorTest`:

- ✅ Check 1: Count Match (Success & Failure)
- ✅ Check 2: CRM Mapping (Success & Failure with orphans)
- ✅ Check 3: LEI Consistency (Success & Failure with mismatches)
- ✅ Check 4: Currency Consistency (Uniform & Mixed)
- ✅ Complete scenario matching provided example

Run tests:
```bash
mvn test -Dtest=ConsistencyValidatorTest
```

## Integration Points

### 1. Database Integration

The `declaredCount` and `crmReferences` can come from:

- **Batch metadata table**: Store `total_exposures` in `ingestion_batches` table
- **CRM system**: External API or database table with CRM references
- **File metadata**: JSON header with batch information

### 2. Quality Report Integration

Consistency check results should be included in quality reports:

```java
QualityReport report = QualityReport.builder()
    .batchId(batchId)
    .overallScore(calculateOverallScore(result))
    .dimensionScores(Map.of(
        QualityDimension.COMPLETENESS, completenessScore,
        QualityDimension.ACCURACY, accuracyScore,
        QualityDimension.CONSISTENCY, result.consistencyResult().overallScore()
    ))
    .consistencyDetails(result.consistencyResult())
    .build();
```

### 3. Business Rules Engine

While per-exposure rules come from the database, consistency checks are **hardcoded** because they:

1. Require access to ALL exposures (not single exposure)
2. Are regulatory requirements (BCBS 239)
3. Don't change frequently
4. Need special aggregation logic

## Performance Considerations

### Memory Usage

Consistency checks process the entire batch in memory:
- ✅ Check 1: O(1) - just count
- ✅ Check 2: O(n) - single pass with HashSet
- ✅ Check 3: O(n) - single pass with HashMap
- ✅ Check 4: O(n) - single pass with grouping

**Total**: O(n) time complexity, O(n) space complexity

### Execution Order

```
1. Prefetch rules (once per batch)
2. Validate exposures in parallel (using virtual threads)
3. Execute consistency checks (sequential, after validation)
4. Return combined results
```

Consistency checks run **after** per-exposure validation to ensure all exposure IDs are available.

## API Changes

### ValidationBatchResult

**Before**:
```java
public record ValidationBatchResult(
    List<ValidationResults> results,
    Map<String, ExposureValidationResult> exposureResults
) {}
```

**After**:
```java
public record ValidationBatchResult(
    List<ValidationResults> results,
    Map<String, ExposureValidationResult> exposureResults,
    ConsistencyValidationResult consistencyResult  // NEW
) {}
```

### ParallelExposureValidationCoordinator

**New overload**:
```java
public ValidationBatchResult validateAll(
    List<ExposureRecord> exposures,
    ExposureRuleValidator validator,
    Integer declaredCount,      // NEW (optional)
    List<String> crmReferences  // NEW (optional)
)
```

**Original method** (unchanged for backward compatibility):
```java
public ValidationBatchResult validateAll(
    List<ExposureRecord> exposures,
    ExposureRuleValidator validator
)
```

## Summary

This implementation provides:

✅ **Four cross-field consistency checks** as per BCBS 239 requirements  
✅ **Detailed violation tracking** with specific error messages  
✅ **Integration with existing validation flow** (non-breaking changes)  
✅ **Comprehensive test coverage** matching provided examples  
✅ **Performance optimized** for large batches  
✅ **Clear documentation** and usage examples  

The consistency checks complement the existing per-exposure validation rules, providing a complete data quality framework covering all six dimensions:

1. ✅ Completezza (Completeness)
2. ✅ Accuratezza (Accuracy)
3. ✅ **Coerenza (Consistency)** ← This implementation
4. ✅ Validità (Validity)
5. ✅ Tempestività (Timeliness)
6. ✅ Unicità (Uniqueness)
