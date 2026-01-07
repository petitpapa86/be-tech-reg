# Comprehensive Code Extraction & Module Separation Plan

## Executive Summary

This plan combines two critical architectural improvements:
1. **Extract shared storage logic** to `regtech-core` (storage operations)
2. **Extract shared recommendation engine** to `regtech-core` (rules & recommendations)
3. **Separate module responsibilities** (data-quality processes, report-generation builds)

**Total Timeline**: 8 days (optimized by combining work)

---

## Table of Contents
1. [Overview](#overview)
2. [Problem Analysis](#problem-analysis)
3. [Unified Target Architecture](#unified-target-architecture)
4. [Merged Implementation Plan](#merged-implementation-plan)
5. [Testing Strategy](#testing-strategy)
6. [Success Criteria](#success-criteria)

---

## Overview

### What We're Fixing

**Three interconnected problems**:

#### Problem 1: Duplicate Storage Code
- ❌ `report-generation` has `S3ReportStorageService`, `LocalFileStorageService`
- ❌ `data-quality` has `LocalDetailedResultsReader`
- ✅ Both use `CoreS3Service` (good!) but have separate wrappers

#### Problem 2: Duplicate Recommendation Logic
- ❌ `report-generation` has `QualityRecommendationsGenerator` (449 lines, hardcoded thresholds)
- ❌ `data-quality` has `QualityWeights` only
- ❌ YAML config (`color-rules-config-COMPLETE.yaml`) has complete rules but not used by Java

#### Problem 3: Architectural Boundary Violation
- ❌ `report-generation` contains data processing logic (`ComprehensiveReportDataAggregator`)
- ❌ Should only build/format reports, not process data

### Unified Solution

**Create shared infrastructure in `regtech-core`**:

```
regtech-core/
├── domain/
│   ├── storage/                              ← NEW: Shared storage abstractions
│   │   ├── IStorageService.java
│   │   ├── StorageUri.java
│   │   └── StorageResult.java
│   │
│   ├── recommendations/                      ← NEW: Shared recommendation models
│   │   ├── RecommendationSection.java (moved)
│   │   ├── RecommendationRule.java
│   │   ├── RecommendationSeverity.java
│   │   └── QualityInsight.java
│   │
│   └── quality/                              ← NEW: Shared quality models
│       ├── QualityWeights.java (moved)
│       ├── QualityThresholds.java
│       └── QualityDimension.java (if not exists)
│
├── application/
│   ├── storage/
│   │   └── StorageService.java              ← NEW: Unified storage operations
│   │
│   └── recommendations/
│       ├── RecommendationEngine.java        ← NEW: Main recommendation service
│       ├── InsightRuleEvaluator.java
│       └── DimensionRecommendationService.java
│
└── infrastructure/
    ├── storage/
    │   ├── StorageServiceAdapter.java       ← NEW: S3/local implementation
    │   └── JsonStorageHelper.java           ← NEW: JSON parsing
    │
    └── recommendations/
        ├── YamlRecommendationRuleLoader.java ← NEW: Loads from YAML
        └── LocalizedRecommendationProvider.java
```

---

## Problem Analysis

### Current State - Duplicate Code Across Modules

#### 1. Storage Duplication

**report-generation** has:
```
infrastructure/filestorage/
├── S3ReportStorageService.java (403 lines)
│   ├── Uses CoreS3Service ✅
│   ├── Uploads to S3
│   ├── Downloads from S3
│   └── Generates presigned URLs
│
├── LocalFileStorageService.java
│   ├── Fallback to local filesystem
│   └── JSON parsing logic
│
└── IReportStorageService.java (interface)
    ├── uploadHtmlReport()
    ├── uploadXbrlReport()
    ├── fetchCalculationData()
    └── fetchQualityData()
```

**data-quality** has:
```
infrastructure/reporting/
└── LocalDetailedResultsReader.java
    ├── Reads from local filesystem
    ├── Parses JSON
    └── Similar logic to LocalFileStorageService
```

**Problems**:
- Storage operations duplicated
- JSON parsing duplicated
- URI handling duplicated
- Bug fix in one module won't apply to other

#### 2. Recommendation Logic Duplication

**report-generation** has:
```java
@Service
public class QualityRecommendationsGenerator {
    // 449 lines of hardcoded logic
    
    private RecommendationSection generateCriticalSituationSection() {
        // Hardcoded threshold: 60%
        if (qualityResults.getOverallScore().compareTo(new BigDecimal("60")) < 0) {
            // Hardcoded Italian text
            String content = "Il punteggio complessivo di qualità del <strong>%.1f%%</strong>...";
        }
    }
    
    private RecommendationSection generateCompletenessSection() {
        // Hardcoded threshold: 70%
        // More hardcoded Italian text
    }
}
```

**data-quality** has:
```java
public record QualityWeights(
    double completeness,    // 0.25 (25%)
    double accuracy,        // 0.25 (25%)
    double consistency,     // 0.20 (20%)
    // ... BCBS 239 recommendations
) {}
```

**YAML config** has (but not used):
```yaml
quality_insights:
  severity_thresholds:
    critical:
      overall_score_below: 65.0  # Different from Java's 60%!
      icon: "🚨"
      color_scheme: "red"
  
  insight_rules:
    - id: "critical_situation"
      priority: 1
      condition:
        type: "or"
        checks:
          - field: "overallScore"
            operator: "<"
            value: 65.0
      output:
        content_template_it: "Il punteggio complessivo..."
        content_template_en: "The overall quality score..."
```

**Problems**:
- Thresholds inconsistent (60% vs 65%, 70% vs 75%)
- Italian text hardcoded in Java, also in YAML
- YAML has complete rules but Java doesn't use them
- Can't update rules without code changes

#### 3. Architectural Problem

**report-generation** has data processing logic:
```java
@Service
public class ComprehensiveReportDataAggregator {
    // 787 lines - SHOULD BE IN data-quality!
    
    public ComprehensiveReportData fetchAllData() {
        // Fetches from S3/local
        String calculationJson = reportStorageService.fetchCalculationData(...);
        String qualityJson = reportStorageService.fetchQualityData(...);
        
        // Processes data (aggregation, validation)
        validateDataConsistency();
        
        return aggregatedData;
    }
}
```

**This violates separation of concerns**: Report generation should **read** processed data, not **process** it.

---

## Unified Target Architecture

### Phase 0: Extract ALL Shared Code to Core (3 days)

#### Part A: Shared Storage (1.5 days)

**Create unified storage abstraction**:

```java
// regtech-core/domain/storage/IStorageService.java
public interface IStorageService {
    Result<StorageResult> upload(String content, StorageUri uri, Map<String, String> metadata);
    Result<String> download(StorageUri uri);
    Result<String> downloadJson(StorageUri uri); // Parses JSON
    Result<PresignedUrl> generatePresignedUrl(StorageUri uri, Duration expiration);
}

// regtech-core/domain/storage/StorageUri.java
public record StorageUri(String uri) {
    // Parses: s3://bucket/key, file:///path/to/file, /relative/path
    
    public static StorageUri parse(String uri) { /* ... */ }
    public String getBucket() { /* for s3:// */ }
    public String getKey() { /* for s3:// */ }
    public String getFilePath() { /* for file:// */ }
    public StorageType getType() { /* S3, LOCAL, RELATIVE */ }
}

// regtech-core/infrastructure/storage/StorageServiceAdapter.java
@Service
public class StorageServiceAdapter implements IStorageService {
    private final CoreS3Service s3Service;
    private final JsonStorageHelper jsonHelper;
    
    @Override
    public Result<String> downloadJson(StorageUri uri) {
        Result<String> content = download(uri);
        if (content.isFailure()) return content;
        
        return jsonHelper.parse(content.getValue());
    }
}
```

#### Part B: Shared Recommendations (1.5 days)

**Create YAML-driven recommendation engine**:

```java
// regtech-core/domain/recommendations/RecommendationSection.java (MOVED)
public record RecommendationSection(
    String icon,
    String colorClass,
    String title,
    String content,
    List<String> bullets,
    RecommendationSeverity severity
) {}

// regtech-core/domain/quality/QualityThresholds.java (NEW - from YAML)
@Builder
public class QualityThresholds {
    private final double excellentThreshold;  // 90.0
    private final double acceptableThreshold; // 75.0
    private final double criticalScoreThreshold; // 65.0
    // ... all thresholds from YAML
}

// regtech-core/application/recommendations/RecommendationEngine.java
@Service
public class RecommendationEngine {
    private final YamlRecommendationRuleLoader yamlLoader;
    private final InsightRuleEvaluator ruleEvaluator;
    
    public List<RecommendationSection> generateRecommendations(
            QualityResults results, 
            Locale locale) {
        
        List<RecommendationRule> rules = yamlLoader.loadRules();
        QualityThresholds thresholds = yamlLoader.loadThresholds();
        
        return ruleEvaluator.evaluateRules(rules, thresholds, results, locale);
    }
}

// regtech-core/infrastructure/recommendations/YamlRecommendationRuleLoader.java
@Component
public class YamlRecommendationRuleLoader {
    public List<RecommendationRule> loadRules() {
        // Load from classpath:color-rules-config-COMPLETE.yaml
        // Parse quality_insights.insight_rules section
    }
    
    public QualityThresholds loadThresholds() {
        // Load all thresholds from YAML
        // dimension_scores, error_distribution, severity_thresholds
    }
}
```

---

### Phase 1: Update data-quality Module (2 days)

#### 1.1 Use Shared Storage
```java
// OLD: LocalDetailedResultsReader
@Component
public class LocalDetailedResultsReader {
    public String readJson(String filePath) { /* duplicate logic */ }
}

// NEW: Use shared IStorageService
@Service
public class QualityReportService {
    private final IStorageService storageService;
    
    public Result<QualityReport> loadReport(String batchId) {
        StorageUri uri = StorageUri.parse("s3://quality-reports/" + batchId + ".json");
        return storageService.downloadJson(uri)
            .map(json -> parseQualityReport(json));
    }
}
```

#### 1.2 Use Shared QualityWeights
```java
// OLD: regtech-data-quality/domain/quality/QualityWeights.java
// DELETE - now in core

// NEW: Import from core
import com.bcbs239.regtech.core.domain.quality.QualityWeights;

QualityWeights weights = QualityWeights.defaultWeights();
```

#### 1.3 Move Data Processing from report-generation
```java
// NEW: data-quality/application/processing/ProcessedDataAggregator.java
// (Moved from report-generation/ComprehensiveReportDataAggregator.java)
@Service
public class ProcessedDataAggregator {
    private final IStorageService storageService; // ← Use shared storage
    
    public Result<ProcessedBatchData> aggregateData(
            String calculationUri, 
            String qualityUri) {
        
        // Fetch using shared storage
        Result<String> calcData = storageService.downloadJson(
            StorageUri.parse(calculationUri)
        );
        Result<String> qualityData = storageService.downloadJson(
            StorageUri.parse(qualityUri)
        );
        
        // Process and store
        ProcessedBatchData processed = processData(calcData, qualityData);
        
        // Store processed result
        StorageUri processedUri = StorageUri.parse(
            "s3://processed-data/" + batchId + ".json"
        );
        storageService.upload(
            toJson(processed), 
            processedUri, 
            Map.of("batchId", batchId)
        );
        
        // Publish event
        eventBus.publish(new ProcessedDataReadyEvent(batchId, processedUri.uri()));
        
        return Result.success(processed);
    }
}
```

---

### Phase 2: Update report-generation Module (2 days)

#### 2.1 Delete Duplicate Storage Code
```bash
# Delete these files:
rm regtech-report-generation/domain/storage/IReportStorageService.java
rm regtech-report-generation/infrastructure/filestorage/S3ReportStorageService.java
rm regtech-report-generation/infrastructure/filestorage/LocalFileStorageService.java
```

#### 2.2 Use Shared Storage
```java
// OLD: ComprehensiveReportDataAggregator (DELETE - moved to data-quality)

// NEW: ReportBuilder (simplified)
@Service
public class ReportBuilder {
    private final IStorageService storageService; // ← Use shared storage
    private final RecommendationEngine recommendationEngine; // ← Use shared recommendations
    
    public Result<HtmlReport> buildReport(ProcessedDataReadyEvent event) {
        // Read processed data (no processing here!)
        Result<ProcessedBatchData> data = storageService.downloadJson(
            StorageUri.parse(event.getProcessedDataUri())
        ).map(json -> parseProcessedData(json));
        
        if (data.isFailure()) return Result.failure(data.getError());
        
        // Generate recommendations using shared engine
        List<RecommendationSection> recommendations = 
            recommendationEngine.generateRecommendations(
                data.getValue().getQualityResults(),
                Locale.forLanguageTag("it-IT")
            );
        
        // Build report (formatting only)
        return buildHtmlReport(data.getValue(), recommendations);
    }
}
```

#### 2.3 Use Shared Recommendations
```java
// OLD: QualityRecommendationsGenerator (DELETE - 449 lines)

// NEW: Use shared RecommendationEngine
private final RecommendationEngine recommendationEngine;

// In report building:
List<RecommendationSection> recommendations = 
    recommendationEngine.generateRecommendations(qualityResults, locale);
```

#### 2.4 Update ComprehensiveReportOrchestrator
```java
@Service
public class ComprehensiveReportOrchestrator {
    private final ReportBuilder reportBuilder; // Simplified
    private final RecommendationEngine recommendationEngine; // Shared
    
    // Remove: ComprehensiveReportDataAggregator (moved to data-quality)
    // Remove: QualityRecommendationsGenerator (now shared in core)
}
```

---

### Phase 3: Testing & Validation (1 day)

#### 3.1 Core Module Tests
```java
// Storage tests
@Test
void shouldUploadAndDownloadFromS3() {
    StorageUri uri = StorageUri.parse("s3://test-bucket/test.json");
    String content = "{\"test\": \"data\"}";
    
    Result<StorageResult> upload = storageService.upload(content, uri, Map.of());
    assertThat(upload.isSuccess()).isTrue();
    
    Result<String> download = storageService.downloadJson(uri);
    assertThat(download.getValue()).isEqualTo(content);
}

// Recommendation tests
@Test
void shouldLoadRulesFromYaml() {
    List<RecommendationRule> rules = yamlLoader.loadRules();
    
    assertThat(rules).hasSize(4); // 4 main rules in YAML
    assertThat(rules.get(0).getId()).isEqualTo("critical_situation");
}

@Test
void shouldGenerateRecommendationsWithYamlThresholds() {
    QualityResults results = createTestResults(55.0); // Critical score
    
    List<RecommendationSection> recommendations = 
        recommendationEngine.generateRecommendations(results, Locale.ITALIAN);
    
    // Should use 65% threshold from YAML, not 60% from old code
    assertThat(recommendations).isNotEmpty();
    assertThat(recommendations.get(0).title()).contains("Critica");
}
```

#### 3.2 Integration Tests
```java
@SpringBootTest
class DataQualityToReportGenerationIntegrationTest {
    
    @Test
    void shouldProcessDataAndGenerateReport() {
        // 1. data-quality processes data using shared storage
        String calculationUri = "s3://calculations/batch123.json";
        String qualityUri = "s3://quality/batch123.json";
        
        Result<ProcessedBatchData> processed = 
            dataAggregator.aggregateData(calculationUri, qualityUri);
        
        assertThat(processed.isSuccess()).isTrue();
        
        // 2. Event published
        verify(eventBus).publish(argThat(event -> 
            event instanceof ProcessedDataReadyEvent &&
            ((ProcessedDataReadyEvent) event).getBatchId().equals("batch123")
        ));
        
        // 3. report-generation reads processed data using shared storage
        Result<HtmlReport> report = reportBuilder.buildReport(
            new ProcessedDataReadyEvent("batch123", processed.getValue().getStorageUri())
        );
        
        assertThat(report.isSuccess()).isTrue();
        assertThat(report.getValue().getRecommendations()).isNotEmpty();
    }
}
```

---

## Merged Implementation Plan

### Timeline Overview

| Phase | Focus | Duration | Key Activities |
|-------|-------|----------|----------------|
| **Phase 0A** | Shared Storage | 1.5 days | Create IStorageService, StorageUri, StorageServiceAdapter |
| **Phase 0B** | Shared Recommendations | 1.5 days | Create RecommendationEngine, load from YAML |
| **Phase 1** | data-quality Updates | 2 days | Use shared storage + weights, move data processing |
| **Phase 2** | report-generation Updates | 2 days | Delete duplicates, use shared storage + recommendations |
| **Phase 3** | Testing & Validation | 1 day | Unit + integration tests |
| **TOTAL** | | **8 days** | |

### Detailed Phase Breakdown

#### Phase 0A: Shared Storage (Day 1-1.5)

**Day 1 Morning**: Core domain models
- [ ] Create `regtech-core/domain/storage/IStorageService.java`
- [ ] Create `regtech-core/domain/storage/StorageUri.java`
- [ ] Create `regtech-core/domain/storage/StorageResult.java`
- [ ] Create `regtech-core/domain/storage/StorageType.java` enum

**Day 1 Afternoon**: Infrastructure implementation
- [ ] Create `regtech-core/infrastructure/storage/StorageServiceAdapter.java`
- [ ] Create `regtech-core/infrastructure/storage/JsonStorageHelper.java`
- [ ] Add configuration: `application-core.yml`

**Day 1.5**: Testing
- [ ] Unit tests for StorageUri parsing
- [ ] Unit tests for StorageServiceAdapter
- [ ] Integration tests with Localstack (S3)

#### Phase 0B: Shared Recommendations (Day 2-3)

**Day 2 Morning**: Domain models
- [ ] Move `RecommendationSection.java` to core
- [ ] Create `RecommendationSeverity.java` enum
- [ ] Move `QualityWeights.java` to core
- [ ] Create `QualityThresholds.java`
- [ ] Create `RecommendationRule.java`
- [ ] Create `QualityInsight.java`

**Day 2 Afternoon**: YAML loader
- [ ] Copy `color-rules-config-COMPLETE.yaml` to core resources
- [ ] Add SnakeYAML dependency
- [ ] Create `YamlRecommendationRuleLoader.java`
- [ ] Implement `loadRules()` method
- [ ] Implement `loadThresholds()` method

**Day 3 Morning**: Application layer
- [ ] Create `RecommendationEngine.java`
- [ ] Create `InsightRuleEvaluator.java`
- [ ] Create `DimensionRecommendationService.java`
- [ ] Create `LocalizedRecommendationProvider.java`

**Day 3 Afternoon**: Testing
- [ ] Unit tests for YAML loading
- [ ] Unit tests for rule evaluation
- [ ] Unit tests for recommendation generation

#### Phase 1: data-quality Updates (Day 4-5)

**Day 4**: Storage migration
- [ ] Delete `LocalDetailedResultsReader.java`
- [ ] Update `pom.xml` to depend on core
- [ ] Replace all storage code with `IStorageService`
- [ ] Update import: `QualityWeights` from core
- [ ] Run module tests

**Day 5**: Data processing migration
- [ ] Move `ComprehensiveReportDataAggregator` from report-generation
- [ ] Rename to `ProcessedDataAggregator.java`
- [ ] Create `ProcessedBatchData.java` domain model
- [ ] Create `ProcessCalculationResultsUseCase.java`
- [ ] Update event publishing: `ProcessedDataReadyEvent`

#### Phase 2: report-generation Updates (Day 6-7)

**Day 6**: Delete duplicates
- [ ] Delete `IReportStorageService.java`
- [ ] Delete `S3ReportStorageService.java`
- [ ] Delete `LocalFileStorageService.java`
- [ ] Delete `QualityRecommendationsGenerator.java` (449 lines!)
- [ ] Delete `RecommendationSection.java` (now in core)
- [ ] Delete `ComprehensiveReportDataAggregator.java` (moved to data-quality)

**Day 6**: Use shared code
- [ ] Update `pom.xml` to depend on core
- [ ] Replace all storage with `IStorageService`
- [ ] Replace recommendations with `RecommendationEngine`
- [ ] Update imports: `RecommendationSection`, `QualityWeights` from core

**Day 7**: Simplify report building
- [ ] Create simplified `ReportBuilder.java`
- [ ] Update `ComprehensiveReportOrchestrator.java`
- [ ] Create `ProcessedDataReadyHandler.java`
- [ ] Delete coordination logic (no longer needed)

#### Phase 3: Testing (Day 8)

**Day 8 Morning**: Unit tests
- [ ] Core storage tests
- [ ] Core recommendation tests
- [ ] data-quality module tests
- [ ] report-generation module tests

**Day 8 Afternoon**: Integration tests
- [ ] End-to-end test: calculation → processing → report
- [ ] Verify no duplicate code remains
- [ ] Verify YAML thresholds used (not hardcoded)
- [ ] Performance testing

---

## Success Criteria

### Functional Requirements
✅ All storage operations through shared `IStorageService`  
✅ All recommendations from YAML rules (no hardcoded thresholds)  
✅ Italian/English localization working from YAML  
✅ data-quality processes data, report-generation builds reports  
✅ No duplicate storage code  
✅ No duplicate recommendation logic  

### Technical Requirements
✅ Zero hardcoded thresholds in Java (grep verification)  
✅ Single `IStorageService` implementation  
✅ Single `RecommendationEngine` implementation  
✅ YAML changes automatically apply to both modules  
✅ 100% test coverage for core shared code  

### Architecture Requirements
✅ Clean separation: data-quality (processing) vs report-generation (building)  
✅ Single source of truth for storage operations (core)  
✅ Single source of truth for business rules (YAML)  
✅ No circular dependencies  
✅ Consistent error handling via Result<T> pattern  

### Code Quality Metrics
✅ Lines deleted: ~1,500+ (duplicates removed)  
✅ Lines added: ~1,000 (shared infrastructure)  
✅ Net reduction: ~500 lines  
✅ Duplication ratio: 0% (DRY principle enforced)  
✅ Test coverage: >90% for core shared code  

---

## Migration Checklist

### Pre-Migration
- [ ] Backup current codebase
- [ ] Create feature branch: `feature/shared-code-extraction`
- [ ] Review YAML structure
- [ ] Map hardcoded thresholds to YAML paths
- [ ] Identify all duplicate storage code

### Phase 0A: Shared Storage
- [ ] Create core storage domain models
- [ ] Implement StorageServiceAdapter
- [ ] Add configuration
- [ ] Write unit tests
- [ ] Write integration tests with Localstack

### Phase 0B: Shared Recommendations
- [ ] Move RecommendationSection to core
- [ ] Create recommendation domain models
- [ ] Copy YAML to core resources
- [ ] Implement YamlRecommendationRuleLoader
- [ ] Create RecommendationEngine
- [ ] Write unit tests

### Phase 1: data-quality
- [ ] Delete duplicate storage code
- [ ] Update to use IStorageService
- [ ] Move QualityWeights to core
- [ ] Move data processing from report-generation
- [ ] Update event publishing
- [ ] Run module tests

### Phase 2: report-generation
- [ ] Delete all duplicate code (storage + recommendations)
- [ ] Update to use IStorageService
- [ ] Update to use RecommendationEngine
- [ ] Simplify report building (remove processing)
- [ ] Update ComprehensiveReportOrchestrator
- [ ] Run module tests

### Phase 3: Testing
- [ ] Run all core tests
- [ ] Run all module tests
- [ ] Run integration tests
- [ ] Verify no duplicates (grep search)
- [ ] Verify YAML usage (no hardcoded values)
- [ ] Performance testing

### Post-Migration
- [ ] Update documentation (copilot-instructions.md)
- [ ] Update MODULE_SEPARATION_REFACTORING_PLAN.md
- [ ] Create SHARED_CODE_GUIDE.md
- [ ] Code review
- [ ] Merge to main

---

## Rollback Plan

If critical issues arise:

### Option 1: Feature Flag
```yaml
regtech:
  core:
    shared-code:
      enabled: false  # Fallback to module-specific code
```

### Option 2: Git Revert
```powershell
# Revert entire feature branch
git revert feature/shared-code-extraction

# Or revert specific commits
git revert <commit-hash>
```

### Option 3: Gradual Rollout
1. Start with storage extraction only
2. Then add recommendations
3. Finally, module separation

---

## Conclusion

This comprehensive plan combines:
1. **Storage extraction** (from MODULE_SEPARATION plan Phase 0)
2. **Recommendation extraction** (from SHARED_RECOMMENDATION plan)
3. **Module separation** (from MODULE_SEPARATION plan Phases 1-3)

**Benefits**:
- ✅ Single migration effort (8 days vs 11.5 days separately)
- ✅ All duplicate code fixed at once
- ✅ Consistent architecture across all modules
- ✅ Single testing phase
- ✅ Reduced risk of conflicts
- ✅ ~500 net lines of code reduction

**Next Steps**: Review this comprehensive plan and approve to start Phase 0A (shared storage extraction).
