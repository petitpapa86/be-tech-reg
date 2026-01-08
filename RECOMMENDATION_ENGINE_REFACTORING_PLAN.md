# Recommendation Engine Refactoring Plan

## 🎯 Objective

**Move recommendation generation logic from report-generation module to data-quality module**, ensuring proper separation of concerns:
- **Data-Quality**: PROCESSES data and GENERATES recommendations
- **Report-Generation**: READS processed data and FORMATS reports (HTML/XBRL)

---

## 📋 Executive Summary

### Current Architecture (INCORRECT ❌)
```
Report-Generation Module
└── ComprehensiveReportOrchestrator
    ├── RecommendationEngine dependency ❌ WRONG LOCATION
    ├── generateInsights() called during report building ❌ WRONG
    └── Business logic mixed with presentation ❌ WRONG
```

### Target Architecture (CORRECT ✅)
```
Data-Quality Module (Processing Layer)
└── ValidateBatchQualityCommandHandler
    ├── RecommendationEngine dependency ✅ CORRECT LOCATION
    ├── generateInsights() after quality scoring ✅ CORRECT
    └── Store recommendations with quality results ✅ CORRECT
        ↓
Storage Layer (S3/Local)
└── Quality Results JSON + Recommendations
        ↓
Report-Generation Module (Presentation Layer)
└── ComprehensiveReportOrchestrator
    ├── READ quality results with recommendations ✅ CORRECT
    ├── mapToRecommendationSections() - format to HTML ✅ CORRECT
    └── NO business logic - ONLY formatting ✅ CORRECT
```

---

## 🔍 Current State Analysis

### Data-Quality Module (WHERE CHANGES ARE NEEDED)

#### File: `ValidateBatchQualityCommandHandler.java`
**Location**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/validation/`

**Current Flow** (lines 56-148):
```java
1. Create QualityReport aggregate
2. Download exposures from S3
3. Validate exposures (parallel processing)
4. Calculate quality scores (via aggregate)
5. Store detailed results to S3
6. Complete validation ✅

❌ MISSING: Generate recommendations after scoring
❌ MISSING: Include recommendations in S3 storage
```

**Key Integration Point** (line 133):
```java
// After quality scores are calculated
QualityScores scores = report.getScores();

// ❌ MISSING: Generate recommendations HERE
// ✅ SHOULD ADD: Call RecommendationEngine.generateInsights()
// ✅ SHOULD ADD: Store recommendations with validation results
```

**Storage Logic** (line 137-148):
```java
Map<String, String> metadata = Map.of(
    "batch-id", command.batchId().value(),
    "overall-score", String.valueOf(scores.overallScore()),
    "grade", scores.grade().name(),
    // ... more metadata
);

Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(
    command.batchId(), validation, metadata
);

// ❌ MISSING: Store recommendations in S3
```

---

### Report-Generation Module (WHERE CHANGES MUST BE REVERTED)

#### File: `ComprehensiveReportOrchestrator.java`
**Location**: `regtech-report-generation/application/src/main/java/com/bcbs239/regtech/reportgeneration/application/generation/`

**Current INCORRECT Integration** (lines 58, 136-156):
```java
// Line 58 - Dependency injection
private final RecommendationEngine recommendationEngine; // ❌ REMOVE THIS

// Lines 136-156 - Recommendation generation during report building
BigDecimal overallScore = reportData.getQualityResults().getOverallScore();
Map<core.QualityDimension, BigDecimal> dimensionScoresCore = 
    convertDimensionScores(reportData.getQualityResults().getDimensionScores()); // ❌ REMOVE
String languageCode = "it";

List<QualityInsight> insights = recommendationEngine.generateInsights(
    overallScore, dimensionScoresCore, languageCode
); // ❌ REMOVE - this is business logic in presentation

List<RecommendationSection> recommendations = mapToRecommendationSections(insights);
```

**Should Be Changed To** (CORRECT):
```java
// Line 136-156 replacement
// ✅ CORRECT: Just read pre-generated recommendations
List<QualityInsight> insights = reportData.getQualityResults().getRecommendations();
List<RecommendationSection> recommendations = mapToRecommendationSections(insights);
```

**Methods to Handle** (lines 405-543):

| Method | Lines | Action | Reason |
|--------|-------|--------|--------|
| `convertDimensionScores()` | 405-426 (22 lines) | ⚠️ MOVE to data-quality | Domain translation belongs in processing |
| `mapToRecommendationSections()` | 428-460 (33 lines) | ✅ KEEP | Presentation formatting is correct here |
| `getSeverityIcon()` | 462-479 (17 lines) | ✅ KEEP | Visual presentation logic |
| `getSeverityColorClass()` | 481-497 (17 lines) | ✅ KEEP | CSS styling logic |

---

### Storage Layer (WHERE FORMAT MUST BE EXTENDED)

#### File: `QualityResults.java` (Report-Generation Domain)
**Location**: `regtech-report-generation/domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/generation/`

**Current Structure** (lines 28-45):
```java
@Getter
public class QualityResults {
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant timestamp;
    private final Integer totalExposures;
    private final Integer validExposures;
    private final Integer totalErrors;
    private final Map<QualityDimension, BigDecimal> dimensionScores;
    private final List<Object> batchErrors;
    private final List<ExposureResult> exposureResults;
    
    // ❌ MISSING: Recommendations field
    
    // Computed properties
    private final BigDecimal overallScore;
    private final QualityGrade overallGrade;
    private final ComplianceStatus complianceStatus;
    private final AttentionLevel attentionLevel;
}
```

**Required Addition**:
```java
// ✅ ADD: Recommendations field
private final List<QualityInsight> recommendations;

// ✅ UPDATE: Constructor to accept recommendations
public QualityResults(
    @NonNull BatchId batchId,
    @NonNull BankId bankId,
    @NonNull Instant timestamp,
    @NonNull Integer totalExposures,
    @NonNull Integer validExposures,
    @NonNull Integer totalErrors,
    @NonNull Map<QualityDimension, BigDecimal> dimensionScores,
    @NonNull List<Object> batchErrors,
    @NonNull List<ExposureResult> exposureResults,
    @NonNull List<QualityInsight> recommendations // ✅ ADD THIS
) {
    // ... existing initialization
    this.recommendations = recommendations != null ? recommendations : List.of();
}

// ✅ ADD: Getter
public List<QualityInsight> getRecommendations() {
    return recommendations;
}
```

---

## 🔧 Implementation Plan

### Phase 1: Add Recommendations to Data-Quality (NEW FUNCTIONALITY)

#### Task 1.1: Add RecommendationEngine Dependency
**File**: `ValidateBatchQualityCommandHandler.java`

**Change**:
```java
// Add to constructor
private final RecommendationEngine recommendationEngine;

public ValidateBatchQualityCommandHandler(
    IQualityReportRepository qualityReportRepository,
    S3StorageService s3StorageService,
    DataQualityRulesService rulesService,
    ParallelExposureValidationCoordinator coordinator,
    BaseUnitOfWork unitOfWork,
    RuleViolationRepository violationRepository,
    RecommendationEngine recommendationEngine // ✅ ADD THIS
) {
    // ... existing assignments
    this.recommendationEngine = recommendationEngine;
}
```

#### Task 1.2: Move Domain Translation Method
**Source**: `ComprehensiveReportOrchestrator.java` (lines 405-426)
**Target**: Create new file `QualityDimensionMapper.java` in data-quality module

**New File**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/validation/QualityDimensionMapper.java`

```java
package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.domain.quality.QualityDimension as CoreQualityDimension;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps data-quality domain QualityDimension to regtech-core QualityDimension
 * for use with RecommendationEngine.
 */
@Component
public class QualityDimensionMapper {
    
    public Map<CoreQualityDimension, BigDecimal> toCoreQualityDimensions(
            Map<QualityDimension, BigDecimal> dataqQualityDimensions) {
        
        return dataqQualityDimensions.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> CoreQualityDimension.valueOf(entry.getKey().name()),
                Map.Entry::getValue
            ));
    }
}
```

#### Task 1.3: Generate Recommendations After Scoring
**File**: `ValidateBatchQualityCommandHandler.java`
**Location**: After line 133 (after scores calculation)

**Add**:
```java
// Line 133: After quality scores are calculated
QualityScores scores = report.getScores();

// ✅ ADD: Generate recommendations using RecommendationEngine
Map<CoreQualityDimension, BigDecimal> dimensionScoresCore = 
    qualityDimensionMapper.toCoreQualityDimensions(scores.dimensionScores());

String languageCode = "it"; // Italian for BCBS 239

List<QualityInsight> recommendations = recommendationEngine.generateInsights(
    scores.overallScore(),
    dimensionScoresCore,
    languageCode
);

logger.info("Generated {} quality recommendations for batchId={} with overall score={}",
    recommendations.size(), command.batchId().value(), scores.overallScore());
```

#### Task 1.4: Update S3 Storage to Include Recommendations
**File**: `ValidateBatchQualityCommandHandler.java` OR `S3StorageService.java`

**Current** (line 137-148):
```java
Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(
    command.batchId(), validation, metadata
);
```

**Option A: Extend storeDetailedResults method signature**
```java
// Update S3StorageService interface
Result<S3Reference> storeDetailedResults(
    BatchId batchId, 
    ValidationResult validation, 
    Map<String, String> metadata,
    List<QualityInsight> recommendations // ✅ ADD THIS
);

// Call site
Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(
    command.batchId(), validation, metadata, recommendations
);
```

**Option B: Store recommendations as separate section in JSON**
```java
// In S3StorageService implementation
public Result<S3Reference> storeDetailedResults(..., List<QualityInsight> recommendations) {
    QualityResultsJson json = new QualityResultsJson();
    json.setBatchId(batchId.value());
    json.setValidationResults(validation);
    json.setRecommendations(recommendations); // ✅ ADD THIS
    json.setMetadata(metadata);
    
    // ... serialize and upload to S3
}
```

---

### Phase 2: Update QualityResults Domain to Include Recommendations

#### Task 2.1: Add Recommendations Field
**File**: `regtech-report-generation/domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/generation/QualityResults.java`

**Change** (after line 38):
```java
@Getter
public class QualityResults {
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant timestamp;
    private final Integer totalExposures;
    private final Integer validExposures;
    private final Integer totalErrors;
    private final Map<QualityDimension, BigDecimal> dimensionScores;
    private final List<Object> batchErrors;
    private final List<ExposureResult> exposureResults;
    
    // ✅ ADD: Recommendations from data-quality processing
    private final List<QualityInsight> recommendations;
    
    // Computed properties (existing)
    private final BigDecimal overallScore;
    private final QualityGrade overallGrade;
    private final ComplianceStatus complianceStatus;
    private final AttentionLevel attentionLevel;
    
    public QualityResults(
            @NonNull BatchId batchId,
            @NonNull BankId bankId,
            @NonNull Instant timestamp,
            @NonNull Integer totalExposures,
            @NonNull Integer validExposures,
            @NonNull Integer totalErrors,
            @NonNull Map<QualityDimension, BigDecimal> dimensionScores,
            @NonNull List<Object> batchErrors,
            @NonNull List<ExposureResult> exposureResults,
            List<QualityInsight> recommendations) { // ✅ ADD: nullable for backward compat
        
        // ... existing assignments
        this.recommendations = recommendations != null ? recommendations : List.of();
        
        // ... derived calculations
    }
}
```

#### Task 2.2: Update ComprehensiveReportDataAggregator
**File**: `regtech-report-generation/application/src/main/java/com/bcbs239/regtech/reportgeneration/application/generation/ComprehensiveReportDataAggregator.java`

**Find**: `fetchQualityData()` method (around line 164-220)

**Update**: Parse recommendations from JSON when reading S3/local storage

```java
private QualityResults fetchQualityData(QualityEventData event, String canonicalBankId) {
    // ... existing S3/local download logic
    
    // Parse JSON
    QualityResultsJson qualityJson = objectMapper.readValue(content, QualityResultsJson.class);
    
    // ✅ ADD: Extract recommendations from JSON
    List<QualityInsight> recommendations = qualityJson.getRecommendations() != null 
        ? qualityJson.getRecommendations() 
        : List.of();
    
    return new QualityResults(
        batchId,
        bankId,
        timestamp,
        totalExposures,
        validExposures,
        totalErrors,
        dimensionScores,
        batchErrors,
        exposureResults,
        recommendations // ✅ ADD THIS
    );
}
```

---

### Phase 3: Simplify Report-Generation (REMOVE BUSINESS LOGIC)

#### Task 3.1: Remove RecommendationEngine Dependency
**File**: `ComprehensiveReportOrchestrator.java`

**Remove** (line 58):
```java
- private final RecommendationEngine recommendationEngine;
```

**Update Constructor**:
```java
public ComprehensiveReportOrchestrator(
    IGeneratedReportRepository reportRepository,
    ComprehensiveReportDataAggregator dataAggregator,
    IStorageService storageService,
    BatchEventTracker batchEventTracker,
    BaseUnitOfWork unitOfWork
    // REMOVED: RecommendationEngine recommendationEngine
) {
    this.reportRepository = reportRepository;
    this.dataAggregator = dataAggregator;
    this.storageService = storageService;
    this.batchEventTracker = batchEventTracker;
    this.unitOfWork = unitOfWork;
    // REMOVED: this.recommendationEngine = recommendationEngine;
}
```

#### Task 3.2: Simplify Recommendation Logic
**File**: `ComprehensiveReportOrchestrator.java`

**Replace** (lines 136-156):
```java
// OLD (WRONG - business logic in presentation):
BigDecimal overallScore = reportData.getQualityResults().getOverallScore();
Map<core.QualityDimension, BigDecimal> dimensionScoresCore = 
    convertDimensionScores(reportData.getQualityResults().getDimensionScores());
String languageCode = "it";

List<QualityInsight> insights = recommendationEngine.generateInsights(
    overallScore, dimensionScoresCore, languageCode
);

List<RecommendationSection> recommendations = mapToRecommendationSections(insights);

// NEW (CORRECT - just read and format):
List<QualityInsight> insights = reportData.getQualityResults().getRecommendations();
List<RecommendationSection> recommendations = mapToRecommendationSections(insights);

log.info("Formatting {} pre-generated recommendations from quality processing [batchId:{}]", 
    recommendations.size(), batchId);
```

#### Task 3.3: Remove Domain Translation Method
**File**: `ComprehensiveReportOrchestrator.java`

**Delete** (lines 405-426):
```java
// DELETE THIS ENTIRE METHOD (moved to data-quality)
private Map<com.bcbs239.regtech.core.domain.quality.QualityDimension, BigDecimal> 
        convertDimensionScores(...) {
    // ... 22 lines
}
```

#### Task 3.4: Keep Presentation Methods (NO CHANGES)
**File**: `ComprehensiveReportOrchestrator.java`

**Keep** (lines 428-497): These are pure presentation logic ✅
- `mapToRecommendationSections()` - Formats QualityInsight to HTML sections
- `getSeverityIcon()` - Returns emoji icons
- `getSeverityColorClass()` - Returns CSS class names

---

### Phase 4: Testing & Validation

#### Task 4.1: Unit Tests - Data-Quality Module
**File**: `ValidateBatchQualityCommandHandlerTest.java`

**Test Cases to Add**:
```java
@Test
void shouldGenerateRecommendationsAfterValidation() {
    // Given: Command with valid batch
    // When: Validation completes successfully
    // Then: RecommendationEngine.generateInsights() was called
    // And: Recommendations stored in S3 with validation results
}

@Test
void shouldHandleRecommendationGenerationFailureGracefully() {
    // Given: RecommendationEngine throws exception
    // When: Validation completes
    // Then: Empty recommendations list stored
    // And: Validation still succeeds
}

@Test
void shouldMapDimensionScoresCorrectly() {
    // Given: Data-quality dimension scores
    // When: Mapped to core dimensions
    // Then: All dimensions correctly converted
}
```

#### Task 4.2: Unit Tests - Report-Generation Module
**File**: `ComprehensiveReportOrchestratorTest.java`

**Test Cases to Update**:
```java
@Test
void shouldReadPreGeneratedRecommendations() {
    // Given: QualityResults with pre-generated recommendations
    // When: Report generation starts
    // Then: Recommendations read from quality results
    // And: No call to RecommendationEngine
}

@Test
void shouldFormatRecommendationsToHtml() {
    // Given: List of QualityInsight objects
    // When: mapToRecommendationSections() called
    // Then: RecommendationSection objects created with correct icons/colors
}

@Test
void shouldHandleMissingRecommendations() {
    // Given: QualityResults with empty recommendations
    // When: Report generation proceeds
    // Then: Empty recommendations section in report
    // And: No errors thrown
}
```

#### Task 4.3: Integration Tests
**File**: `ComprehensiveReportGenerationIntegrationTest.java`

**Test Scenario**:
```java
@Test
void shouldGenerateReportWithRecommendationsFromQualityProcessing() {
    // 1. Trigger quality validation (data-quality module)
    // 2. Verify recommendations generated and stored
    // 3. Trigger report generation (report-generation module)
    // 4. Verify report includes recommendations from step 2
    // 5. Verify recommendations match quality processing output
}
```

---

## 📊 Impact Analysis

### Files to Modify

| File | Module | Lines Changed | Type |
|------|--------|---------------|------|
| `ValidateBatchQualityCommandHandler.java` | data-quality | +15 lines | Add recommendation generation |
| `QualityDimensionMapper.java` | data-quality | +22 lines | New file (move from report-gen) |
| `S3StorageService.java` | data-quality | +10 lines | Store recommendations |
| `QualityResults.java` | report-generation | +5 lines | Add recommendations field |
| `ComprehensiveReportDataAggregator.java` | report-generation | +5 lines | Parse recommendations from JSON |
| `ComprehensiveReportOrchestrator.java` | report-generation | -35 lines | Remove business logic |
| Test files | both | +50 lines | New test cases |

### Code Metrics

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Report-Gen Business Logic | 97 lines | 65 lines | -32 lines ✅ |
| Data-Quality Processing | 148 lines | 185 lines | +37 lines ✅ |
| Total Lines | 245 lines | 250 lines | +5 lines |
| **Architectural Violations** | **1 (major)** | **0** | **-1 ✅** |

### Benefits

✅ **Proper Separation of Concerns**: Processing in data-quality, presentation in report-generation  
✅ **Single Responsibility**: Each module has ONE clear job  
✅ **Testability**: Business logic and presentation logic tested separately  
✅ **Maintainability**: Changes to recommendation logic don't affect report formatting  
✅ **Reusability**: Recommendations can be used by other consumers (APIs, dashboards)  
✅ **Performance**: Recommendations generated once during validation, not per report

---

## 🚀 Implementation Sequence

### Day 1 (4 hours): Data-Quality Module Updates
1. ⏳ Create `QualityDimensionMapper` (move from report-generation)
2. ⏳ Add `RecommendationEngine` dependency to `ValidateBatchQualityCommandHandler`
3. ⏳ Generate recommendations after quality scoring
4. ⏳ Update S3 storage to include recommendations
5. ⏳ Write unit tests for recommendation generation

### Day 2 (3 hours): Storage Layer Updates
1. ⏳ Add `recommendations` field to `QualityResults` domain object
2. ⏳ Update JSON serialization/deserialization
3. ⏳ Update `ComprehensiveReportDataAggregator` to parse recommendations
4. ⏳ Test storage and retrieval end-to-end

### Day 3 (2 hours): Report-Generation Simplification
1. ⏳ Remove `RecommendationEngine` dependency from `ComprehensiveReportOrchestrator`
2. ⏳ Delete `convertDimensionScores()` method
3. ⏳ Simplify recommendation logic (just read from QualityResults)
4. ⏳ Update unit tests
5. ⏳ Run all tests (data-quality + report-generation)

### Day 4 (2 hours): Testing & Documentation
1. ⏳ Integration testing (end-to-end validation → report generation)
2. ⏳ Fix COMPREHENSIVE_CODE_EXTRACTION_PLAN.md
3. ⏳ Fix PHASE_0B_RECOMMENDATION_ENGINE_COMPLETE.md
4. ⏳ Update architecture diagrams
5. ⏳ Final verification

**Total Estimated Time**: 11 hours (1.5 days)

---

## ⚠️ Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking changes to QualityResults JSON format | High | Add recommendations as optional field, default to empty list |
| Existing reports fail to read recommendations | Medium | Backward compatibility - handle missing field gracefully |
| S3 storage size increases | Low | Recommendations are small (KB range), negligible impact |
| Integration tests fail | Medium | Update test fixtures to include recommendations |
| Performance impact on validation | Low | Recommendation generation is fast (<100ms) |

---

## ✅ Success Criteria

1. ✅ RecommendationEngine integrated into data-quality module
2. ✅ Recommendations generated during quality validation
3. ✅ Recommendations stored with quality results (S3/local)
4. ✅ Report-generation reads pre-generated recommendations
5. ✅ No business logic in report-generation (only formatting)
6. ✅ All tests passing (data-quality + report-generation)
7. ✅ Module boundaries respected (processing vs presentation)
8. ✅ Documentation reflects correct architecture

---

## 📚 References

- **Clean Architecture Guide**: [CLEAN_ARCH_GUIDE.md](./CLEAN_ARCH_GUIDE.md)
- **Data-Quality Module**: `regtech-data-quality/`
- **Report-Generation Module**: `regtech-report-generation/`
- **RecommendationEngine**: `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/recommendations/`
- **YAML Config**: `regtech-core/infrastructure/src/main/resources/quality-recommendations-config.yaml`

---

## 🎓 Lessons Learned

### What Went Wrong in Phase 0B Day 3-4
- ❌ Misunderstood architectural boundaries (processing vs presentation)
- ❌ Integrated RecommendationEngine into wrong module (report-generation)
- ❌ Mixed business logic (recommendation generation) with presentation (HTML formatting)
- ❌ Created 300+ lines of documentation for incorrect architecture

### How to Prevent This in Future
- ✅ Always clarify module responsibilities BEFORE implementation
- ✅ Use architectural principle as checklist: "data-quality processes, report-generation presents"
- ✅ Verify integration point with module owner/architect
- ✅ Create architectural diagram BEFORE coding
- ✅ Review module boundaries during code review

---

**Plan Status**: ⏳ AWAITING USER APPROVAL  
**Next Step**: User reviews plan, then we begin Phase 1 implementation  
**Estimated Completion**: 1.5 days after approval

