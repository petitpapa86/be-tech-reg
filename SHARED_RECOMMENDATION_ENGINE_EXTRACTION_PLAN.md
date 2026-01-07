# Shared Recommendation Engine Extraction Plan

## Overview

This document outlines the plan to extract **duplicate recommendation/rules logic** from both `regtech-data-quality` and `regtech-report-generation` modules into `regtech-core` as a **shared recommendation engine**.

## Problem Analysis

### Current State - Duplicate Code

#### 1. Report Generation Module Has:
- **`RecommendationSection.java`** (domain model for recommendations)
- **`QualityRecommendationsGenerator.java`** (449 lines) - Generates recommendations based on quality scores
  - `generateCriticalSituationSection()` - Hardcoded threshold: 60%
  - `generateDimensionSpecificSections()` - Multiple dimension-specific methods
  - `generateCompletenessSection()` - Hardcoded threshold: 70%
  - `generateAccuracySection()` - Hardcoded threshold: 70%
  - `generateConsistencySection()` - Hardcoded threshold: 80%
  - `generateTimelinessSection()`
  - `generateUniquenessSection()`
  - `generateValiditySection()`
  - Hardcoded Italian text for recommendations

#### 2. Data Quality Module Has:
- **`QualityWeights.java`** (domain model for dimension weights)
  - `defaultWeights()` - BCBS 239 recommended weights (25%, 25%, 20%, 15%, 10%, 5%)
  - Mentions "BCBS 239 recommendations" but thresholds not here

#### 3. YAML Configuration Has:
- **`color-rules-config-COMPLETE.yaml`** (677 lines) - SINGLE SOURCE OF TRUTH
  - Dimension score thresholds: excellent (90%), acceptable (75%), poor (<75%)
  - Error distribution thresholds: critical (>100), high (>50), medium (>10), low (≤10)
  - Overall grade thresholds: A (95%), B (85%), C (75%), D (65%), E (50%), F (<50%)
  - **Quality insights rules** (lines 338-535):
    - Severity thresholds (critical <65%, high <75%, medium <85%, low >85%)
    - Insight generation rules with Italian/English templates
    - Dimension-specific recommendations for all 6 dimensions
    - Action plan templates by severity level
  - BCBS 239 compliance principles with thresholds
  - Color schemes for each severity level
  - **Python script setup** (referenced in copilot_instructions)

### The Problem

1. **Duplicate Recommendation Logic**:
   - Report-generation: Hardcoded thresholds (60%, 70%, 80%)
   - YAML config: Different thresholds (65%, 75%, 85%, 90%)
   - **Inconsistency**: Report uses 70% for completeness, YAML uses 75% for acceptable
   
2. **Hardcoded Italian Text**:
   - Report-generation: Italian text embedded in Java code
   - YAML config: Proper i18n with `_it` and `_en` suffixes
   - No shared localization strategy

3. **Missing Domain Model**:
   - `QualityWeights` exists in data-quality (good!)
   - No shared `RecommendationRule` or `QualityThreshold` model
   - No shared `QualityInsight` or `ActionPlan` model

4. **Code Duplication Risk**:
   - If YAML thresholds change, Java code won't match
   - If recommendation logic changes, must update both modules
   - No single source of truth for business rules

---

## Target Architecture

### Phase 0: Extract to `regtech-core`

Create a new **shared recommendation engine** package structure:

```
regtech-core/
├── domain/
│   ├── recommendations/
│   │   ├── RecommendationSection.java (move from report-generation)
│   │   ├── RecommendationRule.java (NEW - represents a rule)
│   │   ├── RecommendationSeverity.java (NEW - enum: CRITICAL, HIGH, MEDIUM, LOW, SUCCESS)
│   │   ├── QualityInsight.java (NEW - domain model for insights)
│   │   └── ActionPlan.java (NEW - domain model for actions)
│   ├── quality/
│   │   ├── QualityWeights.java (move from data-quality)
│   │   ├── QualityThresholds.java (NEW - from YAML)
│   │   ├── QualityDimension.java (check if exists, might need to move)
│   │   └── QualityGrade.java (NEW - A, B, C, D, E, F)
│   └── bcbs239/
│       ├── Bcbs239Principle.java (NEW - represents principles 3-6)
│       └── ComplianceBadge.java (NEW - compliant/non-compliant)
│
├── application/
│   ├── recommendations/
│   │   ├── RecommendationEngine.java (NEW - main orchestrator)
│   │   ├── DimensionRecommendationService.java (NEW - dimension-specific logic)
│   │   ├── ActionPlanGenerator.java (NEW - generates action plans)
│   │   └── InsightRuleEvaluator.java (NEW - evaluates YAML rules)
│   └── quality/
│       ├── QualityScoreCalculator.java (check if exists)
│       └── QualityThresholdEvaluator.java (NEW - evaluates thresholds)
│
└── infrastructure/
    ├── recommendations/
    │   └── YamlRecommendationRuleLoader.java (NEW - loads from YAML)
    └── i18n/
        ├── RecommendationMessageSource.java (NEW - i18n support)
        └── LocalizedRecommendationProvider.java (NEW - provides localized text)
```

### Key Design Principles

1. **Configuration-Driven**: All thresholds and rules from YAML, not hardcoded
2. **Localization**: Proper i18n support for Italian/English (extensible)
3. **Domain-Driven**: Rich domain models (RecommendationRule, QualityInsight)
4. **Single Responsibility**: Each class has one clear purpose
5. **Open/Closed**: Easy to add new rules without modifying existing code

---

## Implementation Plan

### Step 1: Create Domain Models in Core (1 day)

#### 1.1 Move `RecommendationSection.java`
**From**: `regtech-report-generation/domain/generation/RecommendationSection.java`  
**To**: `regtech-core/domain/recommendations/RecommendationSection.java`

```java
package com.bcbs239.regtech.core.domain.recommendations;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RecommendationSection {
    private final String icon;
    private final String colorClass;
    private final String title;
    private final String content;
    private final List<String> bullets;
    private final RecommendationSeverity severity; // NEW - from YAML
}
```

#### 1.2 Create `RecommendationSeverity.java` (NEW)
```java
package com.bcbs239.regtech.core.domain.recommendations;

public enum RecommendationSeverity {
    CRITICAL("🚨", "red"),
    HIGH("⚠️", "orange"),
    MEDIUM("⚡", "yellow"),
    LOW("✓", "green"),
    SUCCESS("✅", "green"),
    INFO("📋", "blue");
    
    private final String icon;
    private final String colorClass;
    
    RecommendationSeverity(String icon, String colorClass) {
        this.icon = icon;
        this.colorClass = colorClass;
    }
    
    // Getters
}
```

#### 1.3 Move `QualityWeights.java`
**From**: `regtech-data-quality/domain/quality/QualityWeights.java`  
**To**: `regtech-core/domain/quality/QualityWeights.java`

Keep exact same code, just move package.

#### 1.4 Create `QualityThresholds.java` (NEW - from YAML)
```java
package com.bcbs239.regtech.core.domain.quality;

import lombok.Builder;
import lombok.Getter;

/**
 * Quality thresholds for dimension scores
 * Loaded from color-rules-config-COMPLETE.yaml
 */
@Getter
@Builder
public class QualityThresholds {
    // Dimension score thresholds
    private final double excellentThreshold;  // 90.0 from YAML
    private final double acceptableThreshold; // 75.0 from YAML
    
    // Error count thresholds
    private final int criticalErrorThreshold; // 100 from YAML
    private final int highErrorThreshold;     // 50 from YAML
    private final int mediumErrorThreshold;   // 10 from YAML
    
    // Overall score thresholds (for severity)
    private final double criticalScoreThreshold; // 65.0 from YAML
    private final double highScoreThreshold;     // 75.0 from YAML
    private final double mediumScoreThreshold;   // 85.0 from YAML
    
    // Grade thresholds
    private final double gradeA; // 95.0
    private final double gradeB; // 85.0
    private final double gradeC; // 75.0
    private final double gradeD; // 65.0
    private final double gradeE; // 50.0
    
    public static QualityThresholds fromYaml() {
        // Will be implemented by YamlRecommendationRuleLoader
        return QualityThresholds.builder()
            .excellentThreshold(90.0)
            .acceptableThreshold(75.0)
            .criticalErrorThreshold(100)
            .highErrorThreshold(50)
            .mediumErrorThreshold(10)
            .criticalScoreThreshold(65.0)
            .highScoreThreshold(75.0)
            .mediumScoreThreshold(85.0)
            .gradeA(95.0)
            .gradeB(85.0)
            .gradeC(75.0)
            .gradeD(65.0)
            .gradeE(50.0)
            .build();
    }
}
```

#### 1.5 Create `RecommendationRule.java` (NEW - from YAML insight_rules)
```java
package com.bcbs239.regtech.core.domain.recommendations;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

/**
 * Represents a recommendation rule from YAML configuration
 * Maps to quality_insights.insight_rules in color-rules-config-COMPLETE.yaml
 */
@Getter
@Builder
public class RecommendationRule {
    private final String id;                    // "critical_situation"
    private final int priority;                 // 1, 2, 3...
    private final RuleCondition condition;      // When to apply
    private final RuleOutput output;            // What to generate
    
    @Getter
    @Builder
    public static class RuleCondition {
        private final String type;              // "or", "and", "dimension_check"
        private final String field;             // "overallScore", "validationRate"
        private final String operator;          // "<", ">=", etc.
        private final double value;             // Threshold value
        private final Map<String, Object> checks; // For complex conditions
    }
    
    @Getter
    @Builder
    public static class RuleOutput {
        private final RecommendationSeverity severity;
        private final String titleIt;           // Italian title
        private final String titleEn;           // English title
        private final String contentTemplateIt; // Italian template with placeholders
        private final String contentTemplateEn; // English template
        private final Map<String, LocalizedRecommendation> dimensionRecommendations;
    }
    
    @Getter
    @Builder
    public static class LocalizedRecommendation {
        private final String italian;
        private final String english;
    }
}
```

#### 1.6 Create `QualityInsight.java` (NEW - generated insight)
```java
package com.bcbs239.regtech.core.domain.recommendations;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Locale;

/**
 * Domain model for a quality insight generated from rules
 */
@Getter
@Builder
public class QualityInsight {
    private final String ruleId;
    private final RecommendationSeverity severity;
    private final String icon;
    private final String title;
    private final String content;
    private final List<String> bulletPoints;
    private final Locale locale;
}
```

---

### Step 2: Create Infrastructure Layer (YAML Loader) (0.5 days)

#### 2.1 Create `YamlRecommendationRuleLoader.java`
```java
package com.bcbs239.regtech.core.infrastructure.recommendations;

import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Loads recommendation rules from color-rules-config-COMPLETE.yaml
 */
@Component
public class YamlRecommendationRuleLoader {
    
    private static final String CONFIG_FILE = "color-rules-config-COMPLETE.yaml";
    
    public List<RecommendationRule> loadRules() {
        try (InputStream input = new ClassPathResource(CONFIG_FILE).getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            
            // Extract quality_insights section
            Map<String, Object> insights = (Map<String, Object>) config.get("quality_insights");
            List<Map<String, Object>> insightRules = (List<Map<String, Object>>) insights.get("insight_rules");
            
            // Convert to RecommendationRule objects
            return insightRules.stream()
                .map(this::toRecommendationRule)
                .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load recommendation rules from YAML", e);
        }
    }
    
    public QualityThresholds loadThresholds() {
        try (InputStream input = new ClassPathResource(CONFIG_FILE).getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            
            // Extract dimension_scores.thresholds
            Map<String, Object> dimensionScores = (Map<String, Object>) config.get("dimension_scores");
            Map<String, Object> thresholds = (Map<String, Object>) dimensionScores.get("thresholds");
            
            Map<String, Object> excellent = (Map<String, Object>) thresholds.get("excellent");
            Map<String, Object> acceptable = (Map<String, Object>) thresholds.get("acceptable");
            
            // Extract error_distribution.thresholds
            Map<String, Object> errorDist = (Map<String, Object>) config.get("error_distribution");
            Map<String, Object> errorThresholds = (Map<String, Object>) errorDist.get("thresholds");
            
            // Extract severity thresholds
            Map<String, Object> insights = (Map<String, Object>) config.get("quality_insights");
            Map<String, Object> severityThresholds = (Map<String, Object>) insights.get("severity_thresholds");
            
            return QualityThresholds.builder()
                .excellentThreshold((Double) excellent.get("value"))
                .acceptableThreshold((Double) acceptable.get("value"))
                .criticalErrorThreshold((Integer) ((Map<String, Object>) errorThresholds.get("critical")).get("value"))
                .highErrorThreshold((Integer) ((Map<String, Object>) errorThresholds.get("high")).get("value"))
                .mediumErrorThreshold((Integer) ((Map<String, Object>) errorThresholds.get("medium")).get("value"))
                .criticalScoreThreshold((Double) ((Map<String, Object>) severityThresholds.get("critical")).get("overall_score_below"))
                .highScoreThreshold((Double) ((Map<String, Object>) severityThresholds.get("high")).get("overall_score_below"))
                .mediumScoreThreshold((Double) ((Map<String, Object>) severityThresholds.get("medium")).get("overall_score_below"))
                .gradeA(95.0) // From overall_grade section
                .gradeB(85.0)
                .gradeC(75.0)
                .gradeD(65.0)
                .gradeE(50.0)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load quality thresholds from YAML", e);
        }
    }
    
    private RecommendationRule toRecommendationRule(Map<String, Object> ruleMap) {
        // Implementation to convert YAML map to RecommendationRule
        // ... detailed conversion logic
    }
}
```

**Dependencies** (add to `regtech-core/pom.xml`):
```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
</dependency>
```

---

### Step 3: Create Application Layer (Recommendation Engine) (1 day)

#### 3.1 Create `RecommendationEngine.java` (Main Orchestrator)
```java
package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.domain.recommendations.*;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Main recommendation engine - replaces QualityRecommendationsGenerator
 * Generates recommendations based on YAML rules, not hardcoded logic
 */
@Service
@RequiredArgsConstructor
public class RecommendationEngine {
    
    private final InsightRuleEvaluator ruleEvaluator;
    private final DimensionRecommendationService dimensionService;
    private final ActionPlanGenerator actionPlanGenerator;
    private final LocalizedRecommendationProvider localizationProvider;
    
    /**
     * Generate recommendations for quality results
     * 
     * @param qualityResults Quality validation results
     * @param locale Locale for localization (it_IT or en_US)
     * @return List of recommendation sections
     */
    public List<RecommendationSection> generateRecommendations(
            QualityResults qualityResults, 
            Locale locale) {
        
        // Evaluate all rules and generate insights
        List<QualityInsight> insights = ruleEvaluator.evaluateRules(qualityResults, locale);
        
        // Convert insights to recommendation sections
        return insights.stream()
            .map(this::toRecommendationSection)
            .limit(6) // Max 6 sections for readability
            .toList();
    }
    
    private RecommendationSection toRecommendationSection(QualityInsight insight) {
        return RecommendationSection.builder()
            .icon(insight.getIcon())
            .colorClass(insight.getSeverity().getColorClass())
            .title(insight.getTitle())
            .content(insight.getContent())
            .bullets(insight.getBulletPoints())
            .severity(insight.getSeverity())
            .build();
    }
}
```

#### 3.2 Create `InsightRuleEvaluator.java`
```java
package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.domain.recommendations.*;
import com.bcbs239.regtech.core.infrastructure.recommendations.YamlRecommendationRuleLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluates recommendation rules from YAML against quality results
 */
@Service
@RequiredArgsConstructor
public class InsightRuleEvaluator {
    
    private final YamlRecommendationRuleLoader ruleLoader;
    private final LocalizedRecommendationProvider localizationProvider;
    
    private List<RecommendationRule> cachedRules;
    
    public List<QualityInsight> evaluateRules(QualityResults qualityResults, Locale locale) {
        if (cachedRules == null) {
            cachedRules = ruleLoader.loadRules();
        }
        
        return cachedRules.stream()
            .sorted(Comparator.comparingInt(RecommendationRule::getPriority))
            .filter(rule -> evaluateCondition(rule.getCondition(), qualityResults))
            .map(rule -> generateInsight(rule, qualityResults, locale))
            .collect(Collectors.toList());
    }
    
    private boolean evaluateCondition(
            RecommendationRule.RuleCondition condition, 
            QualityResults qualityResults) {
        
        return switch (condition.getType()) {
            case "or" -> evaluateOrCondition(condition, qualityResults);
            case "and" -> evaluateAndCondition(condition, qualityResults);
            case "dimension_check" -> evaluateDimensionCheck(condition, qualityResults);
            default -> false;
        };
    }
    
    private QualityInsight generateInsight(
            RecommendationRule rule, 
            QualityResults qualityResults, 
            Locale locale) {
        
        RecommendationRule.RuleOutput output = rule.getOutput();
        
        // Get localized title
        String title = locale.getLanguage().equals("it") 
            ? output.getTitleIt() 
            : output.getTitleEn();
        
        // Get localized content with variable substitution
        String content = locale.getLanguage().equals("it")
            ? output.getContentTemplateIt()
            : output.getContentTemplateEn();
        
        content = substituteVariables(content, qualityResults);
        
        return QualityInsight.builder()
            .ruleId(rule.getId())
            .severity(output.getSeverity())
            .icon(output.getSeverity().getIcon())
            .title(title)
            .content(content)
            .bulletPoints(generateBulletPoints(rule, qualityResults, locale))
            .locale(locale)
            .build();
    }
    
    private String substituteVariables(String template, QualityResults results) {
        // Replace {overallScore}, {validExposures}, {totalExposures}, etc.
        return template
            .replace("{overallScore}", String.format("%.1f", results.getOverallScore()))
            .replace("{validExposures}", String.valueOf(results.getValidExposures()))
            .replace("{totalExposures}", String.valueOf(results.getTotalExposures()));
            // ... more substitutions
    }
}
```

#### 3.3 Create `DimensionRecommendationService.java`
```java
package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Provides dimension-specific recommendations
 * Uses YAML configuration, not hardcoded logic
 */
@Service
@RequiredArgsConstructor
public class DimensionRecommendationService {
    
    private final LocalizedRecommendationProvider localizationProvider;
    
    public List<String> getRecommendationsForDimension(
            QualityDimension dimension, 
            double score, 
            int errorCount,
            Locale locale) {
        
        // Load recommendations from YAML via localizationProvider
        return localizationProvider.getDimensionRecommendations(dimension, locale);
    }
}
```

#### 3.4 Create `LocalizedRecommendationProvider.java`
```java
package com.bcbs239.regtech.core.infrastructure.i18n;

import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.infrastructure.recommendations.YamlRecommendationRuleLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides localized recommendation text from YAML configuration
 */
@Component
@RequiredArgsConstructor
public class LocalizedRecommendationProvider {
    
    private final YamlRecommendationRuleLoader ruleLoader;
    
    public List<String> getDimensionRecommendations(QualityDimension dimension, Locale locale) {
        // Load from YAML: quality_insights.insight_rules[].output.recommendations
        Map<String, Object> recommendations = loadDimensionRecommendations();
        
        Map<String, String> dimensionRecs = (Map<String, String>) recommendations.get(dimension.name().toLowerCase());
        
        String key = locale.getLanguage().equals("it") ? "it" : "en";
        String text = dimensionRecs.get(key);
        
        // Split into bullet points (assume semicolon-separated in YAML)
        return List.of(text.split(";"));
    }
    
    private Map<String, Object> loadDimensionRecommendations() {
        // Load from YAML and cache
        // ...
        return Map.of(); // Placeholder
    }
}
```

---

### Step 4: Update Module Dependencies (0.5 days)

#### 4.1 Update `regtech-data-quality/pom.xml`
```xml
<dependencies>
    <!-- Remove duplicate QualityWeights - now in core -->
    <!-- Add core dependency if not present -->
    <dependency>
        <groupId>com.bcbs239.regtech</groupId>
        <artifactId>regtech-core</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

#### 4.2 Update `regtech-report-generation/pom.xml`
```xml
<dependencies>
    <!-- Add core dependency -->
    <dependency>
        <groupId>com.bcbs239.regtech</groupId>
        <artifactId>regtech-core</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

#### 4.3 Delete Duplicate Classes

**From regtech-report-generation**:
- ❌ Delete: `domain/generation/RecommendationSection.java`
- ❌ Delete: `application/generation/QualityRecommendationsGenerator.java`

**From regtech-data-quality**:
- ❌ Delete: `domain/quality/QualityWeights.java`

#### 4.4 Update Import Statements

**In regtech-report-generation**:
```java
// OLD
import com.bcbs239.regtech.reportgeneration.domain.generation.RecommendationSection;

// NEW
import com.bcbs239.regtech.core.domain.recommendations.RecommendationSection;
```

**In regtech-data-quality**:
```java
// OLD
import com.bcbs239.regtech.dataquality.domain.quality.QualityWeights;

// NEW
import com.bcbs239.regtech.core.domain.quality.QualityWeights;
```

---

### Step 5: Update Module Code to Use Shared Engine (1 day)

#### 5.1 Update Report Generation - `ComprehensiveReportOrchestrator.java`

**Before**:
```java
private final QualityRecommendationsGenerator recommendationsGenerator;

// In buildReport():
List<RecommendationSection> recommendations = 
    recommendationsGenerator.generateRecommendations(reportData.getQualityResults());
```

**After**:
```java
private final RecommendationEngine recommendationEngine;

// In buildReport():
Locale locale = Locale.forLanguageTag("it-IT"); // From configuration or request
List<RecommendationSection> recommendations = 
    recommendationEngine.generateRecommendations(reportData.getQualityResults(), locale);
```

#### 5.2 Update Data Quality - Quality Score Calculation

**Check if QualityScores.java exists and uses QualityWeights**:
```java
// Should now import from core
import com.bcbs239.regtech.core.domain.quality.QualityWeights;

// Usage remains the same
QualityWeights weights = QualityWeights.defaultWeights();
```

---

### Step 6: Copy YAML to Core Resources (0.5 days)

#### 6.1 Copy Configuration File
```powershell
# Copy YAML to core module resources
Copy-Item `
    "c:\Users\alseny\Desktop\react projects\regtech\color-rules-config-COMPLETE.yaml" `
    "c:\Users\alseny\Desktop\react projects\regtech\regtech-core\domain\src\main\resources\color-rules-config-COMPLETE.yaml"
```

#### 6.2 Create Configuration Properties
**New file**: `regtech-core/domain/src/main/resources/application-core.yml`
```yaml
regtech:
  core:
    recommendations:
      config-file: color-rules-config-COMPLETE.yaml
      cache-rules: true
      cache-ttl-seconds: 3600
    localization:
      default-locale: it_IT
      supported-locales:
        - it_IT
        - en_US
```

---

### Step 7: Testing (1 day)

#### 7.1 Unit Tests - Core Module
```java
@Test
void shouldLoadRulesFromYaml() {
    YamlRecommendationRuleLoader loader = new YamlRecommendationRuleLoader();
    List<RecommendationRule> rules = loader.loadRules();
    
    assertThat(rules).hasSize(4); // 4 main rules in YAML
    assertThat(rules.get(0).getId()).isEqualTo("critical_situation");
    assertThat(rules.get(0).getPriority()).isEqualTo(1);
}

@Test
void shouldLoadThresholdsFromYaml() {
    YamlRecommendationRuleLoader loader = new YamlRecommendationRuleLoader();
    QualityThresholds thresholds = loader.loadThresholds();
    
    assertThat(thresholds.getExcellentThreshold()).isEqualTo(90.0);
    assertThat(thresholds.getAcceptableThreshold()).isEqualTo(75.0);
    assertThat(thresholds.getCriticalScoreThreshold()).isEqualTo(65.0);
}

@Test
void shouldGenerateRecommendationsFromEngine() {
    QualityResults results = createTestResults(55.0); // Critical score
    
    List<RecommendationSection> recommendations = 
        recommendationEngine.generateRecommendations(results, Locale.ITALIAN);
    
    assertThat(recommendations).isNotEmpty();
    assertThat(recommendations.get(0).getTitle()).contains("Situazione Critica");
    assertThat(recommendations.get(0).getSeverity()).isEqualTo(RecommendationSeverity.CRITICAL);
}
```

#### 7.2 Integration Tests - Report Generation
```java
@Test
void shouldGenerateReportWithSharedRecommendations() {
    // Create report with quality data
    ComprehensiveReportData data = createTestData();
    
    HtmlReport report = reportOrchestrator.buildReport(data, Locale.ITALIAN);
    
    // Verify recommendations are present and localized
    assertThat(report.getRecommendations()).isNotEmpty();
    assertThat(report.getRecommendations().get(0).getContent()).contains("punteggio");
}
```

#### 7.3 Integration Tests - Data Quality
```java
@Test
void shouldUseSharedQualityWeights() {
    QualityWeights weights = QualityWeights.defaultWeights();
    
    assertThat(weights.completeness()).isEqualTo(0.25);
    assertThat(weights.accuracy()).isEqualTo(0.25);
    
    // Verify weights sum to 1.0
    double sum = weights.completeness() + weights.accuracy() + 
                 weights.consistency() + weights.timeliness() + 
                 weights.uniqueness() + weights.validity();
    assertThat(sum).isEqualTo(1.0);
}
```

---

## Benefits of This Extraction

### 1. **Single Source of Truth**
- ✅ All thresholds from YAML (90%, 75%, 65%, etc.)
- ✅ No hardcoded values in Java
- ✅ Consistent across modules

### 2. **Configuration-Driven**
- ✅ Change thresholds without code changes
- ✅ Easy to add new rules in YAML
- ✅ Python script can generate/update YAML

### 3. **Proper Localization**
- ✅ Italian/English support from YAML
- ✅ Easy to add more languages
- ✅ No hardcoded text in code

### 4. **DRY Principle**
- ✅ No duplicate recommendation logic
- ✅ Shared `QualityWeights` across modules
- ✅ Shared `RecommendationSection` model

### 5. **Testability**
- ✅ Easy to test rule evaluation
- ✅ Can mock YAML loader
- ✅ Clear domain models

### 6. **Maintainability**
- ✅ Change rules in one place (YAML)
- ✅ Update both modules automatically
- ✅ Clear separation of concerns

---

## Migration Checklist

### Pre-Migration
- [ ] Review YAML structure (quality_insights section)
- [ ] Identify all hardcoded thresholds in report-generation
- [ ] Map YAML rules to Java domain models
- [ ] Plan package structure in core

### Phase 0: Core Domain Models
- [ ] Create `regtech-core/domain/recommendations/` package
- [ ] Create `regtech-core/domain/quality/` package
- [ ] Move `RecommendationSection.java` to core
- [ ] Create `RecommendationSeverity.java` enum
- [ ] Move `QualityWeights.java` to core
- [ ] Create `QualityThresholds.java` value object
- [ ] Create `RecommendationRule.java` domain model
- [ ] Create `QualityInsight.java` domain model
- [ ] Run unit tests for domain models

### Phase 1: Core Infrastructure
- [ ] Add SnakeYAML dependency to core `pom.xml`
- [ ] Copy `color-rules-config-COMPLETE.yaml` to core resources
- [ ] Create `YamlRecommendationRuleLoader.java`
- [ ] Implement `loadRules()` method
- [ ] Implement `loadThresholds()` method
- [ ] Create unit tests for YAML loader
- [ ] Verify YAML parsing works

### Phase 2: Core Application Layer
- [ ] Create `RecommendationEngine.java` main service
- [ ] Create `InsightRuleEvaluator.java` rule evaluator
- [ ] Create `DimensionRecommendationService.java`
- [ ] Create `LocalizedRecommendationProvider.java`
- [ ] Implement rule evaluation logic
- [ ] Implement variable substitution in templates
- [ ] Create unit tests for recommendation engine
- [ ] Create integration tests with mock data

### Phase 3: Update Modules
- [ ] Update `regtech-report-generation/pom.xml` (add core dependency)
- [ ] Delete `RecommendationSection.java` from report-generation
- [ ] Delete `QualityRecommendationsGenerator.java` (449 lines)
- [ ] Update `ComprehensiveReportOrchestrator.java` to use `RecommendationEngine`
- [ ] Update all import statements in report-generation
- [ ] Update `regtech-data-quality/pom.xml` (add core dependency)
- [ ] Delete `QualityWeights.java` from data-quality
- [ ] Update all import statements in data-quality
- [ ] Run module compilation tests

### Phase 4: Testing
- [ ] Run core module unit tests (domain models)
- [ ] Run core module unit tests (YAML loader)
- [ ] Run core module unit tests (recommendation engine)
- [ ] Run data-quality module tests
- [ ] Run report-generation module tests
- [ ] Run full integration tests
- [ ] Verify Italian localization works
- [ ] Verify English localization works
- [ ] Verify recommendations match YAML rules

### Phase 5: Documentation
- [ ] Update MODULE_SEPARATION_REFACTORING_PLAN.md with shared engine
- [ ] Update `.github/copilot-instructions.md` with core recommendation engine
- [ ] Create `SHARED_RECOMMENDATION_ENGINE_GUIDE.md` (usage guide)
- [ ] Add JavaDoc comments to all new classes
- [ ] Update README.md with new architecture

### Post-Migration Validation
- [ ] No duplicate `RecommendationSection` classes
- [ ] No duplicate `QualityWeights` classes
- [ ] No hardcoded thresholds in Java (check with grep)
- [ ] All recommendation text from YAML, not Java strings
- [ ] Consistent thresholds across report-generation and data-quality
- [ ] YAML changes automatically apply to both modules
- [ ] Python script can still update YAML without breaking Java

---

## Timeline

| Phase | Tasks | Duration | Dependencies |
|-------|-------|----------|--------------|
| **Phase 0** | Create core domain models | 1 day | None |
| **Phase 1** | Create YAML loader infrastructure | 0.5 days | Phase 0 |
| **Phase 2** | Create recommendation engine application layer | 1 day | Phase 0, Phase 1 |
| **Phase 3** | Update module dependencies and code | 0.5 days | Phase 0, Phase 1, Phase 2 |
| **Phase 4** | Testing (unit + integration) | 1 day | Phase 3 |
| **Phase 5** | Documentation | 0.5 days | Phase 4 |
| **TOTAL** | | **4.5 days** | |

---

## Success Criteria

### Functional Requirements
✅ All recommendations generated from YAML rules, not hardcoded logic  
✅ Thresholds consistent across both modules (90%, 75%, 65%)  
✅ Italian and English localization working  
✅ No duplicate `RecommendationSection` or `QualityWeights` classes  
✅ YAML changes automatically apply to both modules  

### Technical Requirements
✅ Zero hardcoded thresholds in Java (verified by grep)  
✅ All recommendation text externalized to YAML  
✅ Proper domain models in core (RecommendationRule, QualityThresholds)  
✅ Clean separation: domain → application → infrastructure  
✅ 100% test coverage for core recommendation engine  

### Architecture Requirements
✅ Single source of truth for business rules (YAML)  
✅ Shared domain models in regtech-core  
✅ No circular dependencies between modules  
✅ Configuration-driven, not code-driven  
✅ Easy to add new rules without code changes  

---

## Rollback Plan

If issues arise during migration:

1. **Keep old code in Git history**:
   ```powershell
   git checkout HEAD~1 -- regtech-report-generation/application/generation/QualityRecommendationsGenerator.java
   ```

2. **Feature flag in configuration**:
   ```yaml
   regtech:
     recommendations:
       use-shared-engine: false  # Fallback to old hardcoded logic
   ```

3. **Gradual rollout**: Start with report-generation only, then data-quality

---

## Conclusion

This extraction plan addresses the **duplicate recommendation/rules logic** across modules by:

1. **Creating a shared recommendation engine** in `regtech-core`
2. **Loading all rules from YAML configuration** (single source of truth)
3. **Eliminating hardcoded thresholds** in Java code
4. **Providing proper localization** (Italian/English from YAML)
5. **Following DRY principle** - no duplicate logic

The result is a **maintainable, testable, configuration-driven recommendation system** that can be shared by all modules and easily updated via YAML changes.

---

**Next Steps**: Review this plan with the team and get approval to start Phase 0 (domain model creation).
