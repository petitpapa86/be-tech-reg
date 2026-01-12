package com.bcbs239.regtech.dataquality.application.validation.consistency;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for ConsistencyValidator - DIMENSIONE: COERENZA (CONSISTENCY)
 * 
 * Tests the four consistency checks:
 * 1. Conteggio Esposizioni
 * 2. Mappatura CRM → Esposizioni
 * 3. Relazione LEI ↔ counterpartyId (1:1)
 * 4. Coerenza Valutaria
 */
@DisplayName("DIMENSIONE 3: COERENZA (Consistency) - Cross-Field Checks")
class ConsistencyValidatorTest {

    private ConsistencyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConsistencyValidator();
    }

    @Test
    @DisplayName("Check 1: Conteggio Esposizioni - Match ✓")
    void testExposureCountMatch_Success() {
        // Arrange
        List<ExposureRecord> exposures = createExposures(4);
        Integer declaredCount = 4;

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, declaredCount, null);

        // Assert
        assertThat(result.allPassed()).isTrue();
        assertThat(result.overallScore()).isEqualTo(100.0);
        
        ConsistencyCheckResult countCheck = findCheck(result, ConsistencyCheckType.EXPOSURE_COUNT_MATCH);
        assertThat(countCheck.passed()).isTrue();
        assertThat(countCheck.score()).isEqualTo(100.0);
        assertThat(countCheck.summary()).contains("✓ Coerente");
    }

    @Test
    @DisplayName("Check 1: Conteggio Esposizioni - Mismatch ✗")
    void testExposureCountMismatch_Failure() {
        // Arrange
        List<ExposureRecord> exposures = createExposures(4);
        Integer declaredCount = 5; // Dichiarato 5, ma ne abbiamo solo 4

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, declaredCount, null);

        // Assert
        assertThat(result.allPassed()).isFalse();
        
        ConsistencyCheckResult countCheck = findCheck(result, ConsistencyCheckType.EXPOSURE_COUNT_MATCH);
        assertThat(countCheck.passed()).isFalse();
        assertThat(countCheck.score()).isEqualTo(0.0);
        assertThat(countCheck.summary()).contains("✗ INCOERENTE");
        assertThat(countCheck.violations()).hasSize(1);
        assertThat(countCheck.violations().get(0).violationType()).isEqualTo("COUNT_MISMATCH");
    }

    @Test
    @DisplayName("Check 2: Mappatura CRM → Esposizioni - Match perfetto ✓")
    void testCrmMapping_AllMatch() {
        // Arrange
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001_2024", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002_2024", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_003_2024", "CORP12346", "549300GHIJKL98765432", "EUR"),
            createExposure("EXP_004_2024", "CORP12347", "549300MNOPQR11111111", "EUR")
        );
        
        List<String> crmReferences = List.of(
            "EXP_001_2024",
            "EXP_002_2024",
            "EXP_003_2024",
            "EXP_004_2024"
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, null, crmReferences);

        // Assert
        ConsistencyCheckResult crmCheck = findCheck(result, ConsistencyCheckType.CRM_EXPOSURE_MAPPING);
        assertThat(crmCheck.passed()).isTrue();
        assertThat(crmCheck.score()).isEqualTo(100.0);
        assertThat(crmCheck.summary()).contains("4/4 (100%)");
    }

    @Test
    @DisplayName("Check 2: Mappatura CRM → Esposizioni - Con riferimenti orfani ✗")
    void testCrmMapping_OrphanReferences() {
        // Arrange: Come nell'esempio fornito
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001_2024", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002_2024", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002_2024", "CORP12345", "549300ABCDEF12345678", "EUR"), // Duplicate
            createExposure("EXP_002_2024", "CORP12345", "549300ABCDEF12345678", "EUR")  // Duplicate
        );
        
        List<String> crmReferences = List.of(
            "EXP_001_2024", // ✓ Esiste
            "EXP_003_2024", // ✗ NON ESISTE
            "EXP_004_2024", // ✗ NON ESISTE
            "EXP_008_2024"  // ✗ NON ESISTE
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, null, crmReferences);

        // Assert
        ConsistencyCheckResult crmCheck = findCheck(result, ConsistencyCheckType.CRM_EXPOSURE_MAPPING);
        assertThat(crmCheck.passed()).isFalse(); // 25% < 90% threshold
        assertThat(crmCheck.score()).isEqualTo(25.0); // 1/4 = 25%
        assertThat(crmCheck.summary()).contains("1/4 (25%)");
        assertThat(crmCheck.violations()).hasSize(3); // 3 orfani
        
        // Verify violations
        assertThat(crmCheck.violations())
            .extracting(ConsistencyViolation::violationType)
            .containsOnly("ORPHAN_CRM_REFERENCE");
    }

    @Test
    @DisplayName("Check 3: Relazione LEI ↔ counterpartyId - Coerente ✓")
    void testLeiCounterpartyConsistency_Success() {
        // Arrange: Stesso LEI = Stesso counterpartyId
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_003", "CORP12346", "549300GHIJKL98765432", "EUR"),
            createExposure("EXP_004", "CORP12346", "549300GHIJKL98765432", "EUR")
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, null, null);

        // Assert
        ConsistencyCheckResult leiCheck = findCheck(result, ConsistencyCheckType.LEI_COUNTERPARTY_CONSISTENCY);
        assertThat(leiCheck.passed()).isTrue();
        assertThat(leiCheck.score()).isEqualTo(100.0);
        assertThat(leiCheck.summary()).contains("✓ 1:1 mapping");
        assertThat(leiCheck.violations()).isEmpty();
    }

    @Test
    @DisplayName("Check 3: Relazione LEI ↔ counterpartyId - Incoerente ✗")
    void testLeiCounterpartyInconsistency_Failure() {
        // Arrange: Come nell'esempio - stesso LEI con counterpartyId diversi
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_003", "CORP12344", "549300ABCDEF12345678", "EUR"), // ✗ DIVERSO!
            createExposure("EXP_004", "CORP12344", "549300ABCDEF12345678", "EUR")  // ✗ DIVERSO!
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, null, null);

        // Assert
        ConsistencyCheckResult leiCheck = findCheck(result, ConsistencyCheckType.LEI_COUNTERPARTY_CONSISTENCY);
        assertThat(leiCheck.passed()).isFalse();
        assertThat(leiCheck.score()).isEqualTo(0.0); // 0/1 LEI coerenti
        assertThat(leiCheck.summary()).contains("✗ FAIL");
        assertThat(leiCheck.violations()).hasSize(1);
        
        // Verify violation details
        ConsistencyViolation violation = leiCheck.violations().get(0);
        assertThat(violation.violationType()).isEqualTo("LEI_COUNTERPARTY_MISMATCH");
        assertThat(violation.affectedEntity()).contains("549300ABCDEF12345678");
        assertThat(violation.actualValue()).contains("CORP12345", "CORP12344");
        assertThat(violation.description())
            .contains("Stesso LEI deve sempre avere stesso counterpartyId");
    }

    @Test
    @DisplayName("Check 4: Coerenza Valutaria - Uniforme ✓")
    void testCurrencyConsistency_Uniform() {
        // Arrange: Tutte le esposizioni in EUR
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001", "CORP12345", "549300ABC", "EUR"),
            createExposure("EXP_002", "CORP12346", "549300DEF", "EUR"),
            createExposure("EXP_003", "CORP12347", "549300GHI", "EUR"),
            createExposure("EXP_004", "CORP12348", "549300JKL", "EUR")
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, null, null);

        // Assert
        ConsistencyCheckResult currencyCheck = findCheck(result, ConsistencyCheckType.CURRENCY_CONSISTENCY);
        assertThat(currencyCheck.passed()).isTrue();
        assertThat(currencyCheck.score()).isEqualTo(100.0);
        assertThat(currencyCheck.summary()).contains("EUR: 4 (100.0%)");
        assertThat(currencyCheck.summary()).contains("✓ Coerente");
    }

    @Test
    @DisplayName("Check 4: Coerenza Valutaria - Mix valute ✗")
    void testCurrencyConsistency_Mixed() {
        // Arrange: Mix di valute senza dominanza chiara
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001", "CORP12345", "549300ABC", "EUR"),
            createExposure("EXP_002", "CORP12346", "549300DEF", "EUR"),
            createExposure("EXP_003", "CORP12347", "549300GHI", "USD"),
            createExposure("EXP_004", "CORP12348", "549300JKL", "GBP")
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, null, null);

        // Assert
        ConsistencyCheckResult currencyCheck = findCheck(result, ConsistencyCheckType.CURRENCY_CONSISTENCY);
        assertThat(currencyCheck.passed()).isFalse(); // 50% dominance < 90% threshold
        assertThat(currencyCheck.score()).isLessThan(90.0);
        assertThat(currencyCheck.summary()).contains("✗ FAIL");
        assertThat(currencyCheck.violations()).isNotEmpty();
    }

    @Test
    @DisplayName("Scenario completo: Come l'esempio fornito")
    void testCompleteScenario_AsProvided() {
        // Arrange: Scenario esattamente come nell'esempio
        List<ExposureRecord> exposures = List.of(
            createExposure("EXP_001_2024", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002_2024", "CORP12345", "549300ABCDEF12345678", "EUR"),
            createExposure("EXP_002_2024", "CORP12345", "549300ABCDEF12345678", "EUR"), // Duplicate
            createExposure("EXP_002_2024", "CORP12344", "549300ABCDEF12345678", "EUR")  // ✗ counterpartyId diverso!
        );

        Integer declaredCount = 4;
        List<String> crmReferences = List.of(
            "EXP_001_2024",
            "EXP_003_2024", // ✗ Orfano
            "EXP_004_2024", // ✗ Orfano
            "EXP_008_2024"  // ✗ Orfano
        );

        // Act
        ConsistencyValidationResult result = validator.validate(exposures, declaredCount, crmReferences);

        // Assert overall result
        assertThat(result.allPassed()).isFalse();
        assertThat(result.checks()).hasSize(4);

        // Check 1: Count - PASS (4 = 4)
        ConsistencyCheckResult countCheck = findCheck(result, ConsistencyCheckType.EXPOSURE_COUNT_MATCH);
        assertThat(countCheck.passed()).isTrue();
        assertThat(countCheck.score()).isEqualTo(100.0);

        // Check 2: CRM Mapping - FAIL (25%)
        ConsistencyCheckResult crmCheck = findCheck(result, ConsistencyCheckType.CRM_EXPOSURE_MAPPING);
        assertThat(crmCheck.passed()).isFalse();
        assertThat(crmCheck.score()).isEqualTo(25.0);
        assertThat(crmCheck.violations()).hasSize(3);

        // Check 3: LEI Consistency - FAIL (stesso LEI, counterpartyId diversi)
        ConsistencyCheckResult leiCheck = findCheck(result, ConsistencyCheckType.LEI_COUNTERPARTY_CONSISTENCY);
        assertThat(leiCheck.passed()).isFalse();
        assertThat(leiCheck.score()).isEqualTo(0.0);
        assertThat(leiCheck.violations()).hasSize(1);

        // Check 4: Currency - PASS (tutte EUR)
        ConsistencyCheckResult currencyCheck = findCheck(result, ConsistencyCheckType.CURRENCY_CONSISTENCY);
        assertThat(currencyCheck.passed()).isTrue();
        assertThat(currencyCheck.score()).isEqualTo(100.0);

        // Print formatted output like in the example
        printFormattedResult(result);
    }

    // Helper methods

    private List<ExposureRecord> createExposures(int count) {
        List<ExposureRecord> exposures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            exposures.add(createExposure(
                "EXP_" + String.format("%03d", i),
                "CORP" + i,
                "549300ABC" + i,
                "EUR"
            ));
        }
        return exposures;
    }

    private ExposureRecord createExposure(
        String exposureId,
        String counterpartyId,
        String lei,
        String currency
    ) {
        return new ExposureRecord(
            exposureId,
            counterpartyId,
            BigDecimal.valueOf(1000000),
            currency,
            "IT",
            "Finance",
            "Corporate",
            "Loan",
            lei,
            "BBB",
            "Low",
            BigDecimal.valueOf(0.5),
            LocalDate.now(),
            LocalDate.now(),
            LocalDate.now().plusYears(5),
            "REF_" + exposureId
        );
    }

    private ConsistencyCheckResult findCheck(
        ConsistencyValidationResult result,
        ConsistencyCheckType type
    ) {
        return result.checks().stream()
            .filter(check -> check.checkType() == type)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Check not found: " + type));
    }

    private void printFormattedResult(ConsistencyValidationResult result) {
        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("DIMENSIONE 3: COERENZA (Consistency)");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("\nDefinizione:");
        System.out.println("I dati sono uniformi tra diverse fonti e nel tempo");
        System.out.println("\nControlli Cross-Field:");
        System.out.println("─────────────────────────────────────────────────────────────────");

        for (ConsistencyCheckResult check : result.checks()) {
            System.out.println("\n" + check.checkType().getDisplayName());
            System.out.println(check.summary());
            System.out.printf("Score: %.0f%%%s\n", 
                check.score(), 
                check.passed() ? "" : " FAIL");

            if (!check.violations().isEmpty()) {
                System.out.println("\nViolazioni:");
                for (ConsistencyViolation violation : check.violations()) {
                    System.out.printf("  ├─ %s\n", violation.affectedEntity());
                    System.out.printf("  ├─ Atteso: %s\n", violation.expectedValue());
                    System.out.printf("  ├─ Trovato: %s\n", violation.actualValue());
                    System.out.printf("  └─ %s\n", violation.description());
                }
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.printf("RISULTATO COMPLESSIVO: %s (Score: %.1f%%)\n",
            result.allPassed() ? "✓ PASS" : "✗ FAIL",
            result.overallScore());
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
    }
}
