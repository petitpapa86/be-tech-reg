package com.bcbs239.regtech.dataquality.application.validation.consistency;

import com.bcbs239.regtech.dataquality.application.rulesengine.ExposureRecordBuilder;
import com.bcbs239.regtech.dataquality.application.validation.ExposureRuleValidator;
import com.bcbs239.regtech.dataquality.application.validation.ParallelExposureValidationCoordinator;
import com.bcbs239.regtech.dataquality.application.validation.ValidationBatchResult;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.ConsistencyCheckResult;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.ConsistencyCheckType;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.ConsistencyValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Example service showing how to integrate consistency checks into the validation flow.
 * 
 * This demonstrates:
 * 1. Standard per-exposure validation (using rules from database)
 * 2. Cross-field consistency checks (Check 1-4)
 * 3. Combined results with quality scores
 */
@Component
public class ConsistencyIntegrationExample {

    private final ParallelExposureValidationCoordinator coordinator;
    private final ExposureRuleValidator ruleValidator;

    public ConsistencyIntegrationExample(
        ParallelExposureValidationCoordinator coordinator,
        ExposureRuleValidator ruleValidator
    ) {
        this.coordinator = coordinator;
        this.ruleValidator = ruleValidator;
    }

    /**
     * Complete validation flow with consistency checks.
     * 
     * @param exposures List of exposure records
     * @param declaredCount Declared number of exposures (from batch metadata)
     * @param crmReferences List of CRM reference IDs (from external system)
     * @return Complete validation results including consistency checks
     */
    public ValidationBatchResult validateWithConsistency(
        List<ExposureRecord> exposures,
        Integer declaredCount,
        List<String> crmReferences
    ) {
        // Execute validation WITH consistency checks
        // This will:
        // 1. Run per-exposure rules (Completezza, Accuratezza, etc.)
        // 2. Execute cross-field consistency checks
        ValidationBatchResult result = coordinator.validateAllWithConsistency(
            exposures,
            ruleValidator,
            declaredCount,
            crmReferences
        );

        // Log consistency results (if performed)
        if (result.hasConsistencyChecks()) {
            logConsistencyResults(result.consistencyResult());
        }

        return result;
    }

    /**
     * Demonstrates how to use the results programmatically.
     */
    public void demonstrateUsage(ValidationBatchResult result) {
        // Check if consistency checks were performed
        if (!result.hasConsistencyChecks()) {
            System.out.println("ℹ Consistency checks were not performed (backward compatible mode)");
            return;
        }
        
        ConsistencyValidationResult consistency = result.consistencyResult();

        // Check overall pass/fail
        if (!consistency.allPassed()) {
            System.out.println("⚠ ATTENZIONE: Controlli di coerenza falliti!");
            System.out.printf("Score complessivo: %.1f%%\n", consistency.overallScore());
        }

        // Check specific dimensions
        for (ConsistencyCheckResult check : consistency.checks()) {
            switch (check.checkType()) {
                case EXPOSURE_COUNT_MATCH:
                    if (!check.passed()) {
                        System.out.println("❌ Conteggio esposizioni non corrisponde!");
                        // Take action: reject batch
                    }
                    break;

                case CRM_EXPOSURE_MAPPING:
                    if (!check.passed()) {
                        System.out.printf("❌ %d riferimenti CRM orfani trovati!\n", 
                            check.violations().size());
                        // Take action: flag for review
                    }
                    break;

                case LEI_COUNTERPARTY_CONSISTENCY:
                    if (!check.passed()) {
                        System.out.println("❌ Inconsistenza LEI-CounterpartyId!");
                        System.out.println("Questo può causare errori nel calcolo del Grande Esposizione!");
                        // Take action: critical error, must fix
                    }
                    break;

                case CURRENCY_CONSISTENCY:
                    if (!check.passed()) {
                        System.out.println("⚠ Mix valute rilevato");
                        // Take action: warning, may be expected
                    }
                    break;
            }
        }

        // Access per-exposure results
        System.out.printf("\nEsposizioni validate: %d\n", result.results().size());
        System.out.printf("Esposizioni con errori: %d\n",
            result.results().stream()
                .filter(r -> !r.validationErrors().isEmpty())
                .count());

        // Access specific exposure results
        result.exposureResults().forEach((exposureId, exposureResult) -> {
            if (!exposureResult.isValid()) {
                System.out.printf("Esposizione %s ha %d errori\n",
                    exposureId,
                    exposureResult.errors().size());
            }
        });
    }

    private void logConsistencyResults(ConsistencyValidationResult result) {
        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("DIMENSIONE 3: COERENZA (Consistency)");
        System.out.println("═══════════════════════════════════════════════════════════════════");

        for (ConsistencyCheckResult check : result.checks()) {
            String status = check.passed() ? "✓" : "✗";
            System.out.printf("\n%s %s - Score: %.0f%%\n",
                status,
                check.checkType().getDisplayName(),
                check.score());
            System.out.println("  " + check.summary());

            if (!check.violations().isEmpty()) {
                System.out.printf("  %d violazioni trovate\n", check.violations().size());
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.printf("RISULTATO: %s (Score: %.1f%%)\n",
            result.allPassed() ? "✓ PASS" : "✗ FAIL",
            result.overallScore());
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
    }
}
