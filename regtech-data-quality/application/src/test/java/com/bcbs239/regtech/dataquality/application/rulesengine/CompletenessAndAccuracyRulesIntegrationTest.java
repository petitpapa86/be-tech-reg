package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Completeness and Accuracy rules.
 * Tests the actual rules defined in V42__insert_initial_business_rules.sql
 * 
 * Tests both COMPLETENESS (Dimension 1) and ACCURACY (Dimension 2) validations.
 * 
 * NOTE: This test should be run from the regtech-app module to access the full Spring Boot configuration.
 * Run: mvnw test -pl regtech-app -Dtest=CompletenessAndAccuracyRulesIntegrationTest
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:db/migration/V3__init_schemas.sql",
    "classpath:db/migration/dataquality/V40__create_rules_engine_tables.sql",
    "classpath:db/migration/dataquality/V41__insert_regulations.sql",
    "classpath:db/migration/dataquality/V42__insert_initial_business_rules.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Completeness and Accuracy Rules Integration Tests")
class CompletenessAndAccuracyRulesIntegrationTest {

    @Autowired
    private DataQualityRulesService dataQualityRulesService;

    @Nested
    @DisplayName("DIMENSIONE 1: COMPLETEZZA (Completeness)")
    class CompletenessRulesTests {

        @Test
        @DisplayName("✅ Should PASS when all required fields are present")
        void shouldPassWhenAllRequiredFieldsPresent() {
            // Arrange - Esposizione completa
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP_001_2024")
                .counterpartyId("CP_12345")
                .exposureAmount(new BigDecimal("2000000"))
                .currency("EUR")
                .countryCode("IT")
                .sector("RETAIL")
                .counterpartyType("CORPORATE")
                .productType("LOAN")
                .counterpartyLei("815600D7623147C25D86")
                .internalRating("BBB")
                .riskCategory("STANDARD")
                .riskWeight(new BigDecimal("0.75"))
                .reportingDate(LocalDate.now().minusDays(10))
                .valuationDate(LocalDate.now().minusDays(5))
                .maturityDate(LocalDate.now().plusYears(1))
                .referenceNumber("REF_001")
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert - check no COMPLETENESS violations
            List<RuleViolation> completenessViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("COMPLETENESS"))
                .toList();
                
            assertThat(completenessViolations)
                .describedAs("Non dovrebbero esserci violazioni per completezza")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL when exposureId is missing")
        void shouldFailWhenExposureIdMissing() {
            // Arrange - Manca exposureId
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId(null) // ❌ Mancante
                .counterpartyId("CP_12345")
                .exposureAmount(new BigDecimal("2000000"))
                .currency("EUR")
                .countryCode("IT")
                .sector("RETAIL")
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> violations = results.ruleViolations();
            assertThat(violations)
                .describedAs("Dovrebbe esserci violazione per exposureId mancante")
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("COMPLETENESS");
                    assertThat(violation.violationDescription()).contains("exposureId");
                });
        }

        @Test
        @DisplayName("❌ Should FAIL when exposureId is empty")
        void shouldFailWhenExposureIdEmpty() {
            // Arrange - exposureId vuoto
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("   ") // ❌ Vuoto (solo spazi)
                .counterpartyId("CP_12345")
                .exposureAmount(new BigDecimal("2000000"))
                .currency("EUR")
                .countryCode("IT")
                .sector("RETAIL")
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .describedAs("Dovrebbe esserci violazione per exposureId vuoto")
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("COMPLETENESS");
                    assertThat(violation.violationDescription().toLowerCase()).contains("exposureid");
                });
        }

        @Test
        @DisplayName("❌ Should FAIL when counterpartyId is missing (NEW RULE)")
        void shouldFailWhenCounterpartyIdMissing() {
            // Arrange - Manca counterpartyId
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP_001")
                .counterpartyId(null) // ❌ Mancante
                .exposureAmount(new BigDecimal("2000000"))
                .currency("EUR")
                .countryCode("IT")
                .sector("RETAIL")
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .describedAs("Dovrebbe esserci violazione per counterpartyId mancante")
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("COMPLETENESS");
                    assertThat(violation.violationDescription().toLowerCase()).contains("counterpartyid");
                });
        }

        @Test
        @DisplayName("❌ Should FAIL when amount is missing")
        void shouldFailWhenAmountMissing() {
            // Arrange
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP_001")
                .counterpartyId("CP_12345")
                .exposureAmount(null) // ❌ Mancante
                .currency("EUR")
                .countryCode("IT")
                .sector("RETAIL")
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("COMPLETENESS");
                    assertThat(violation.violationDescription().toLowerCase()).contains("amount");
                });
        }

        @Test
        @DisplayName("❌ Should FAIL when currency is missing")
        void shouldFailWhenCurrencyMissing() {
            // Arrange
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP_001")
                .counterpartyId("CP_12345")
                .exposureAmount(new BigDecimal("2000000"))
                .currency(null) // ❌ Mancante
                .countryCode("IT")
                .sector("RETAIL")
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("COMPLETENESS");
                    assertThat(violation.violationDescription().toLowerCase()).contains("currency");
                });
        }
    }

    @Nested
    @DisplayName("DIMENSIONE 2: ACCURATEZZA (Accuracy)")
    class AccuracyRulesTests {

        @Test
        @DisplayName("✅ Should PASS when amount is positive")
        void shouldPassWhenAmountPositive() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .exposureAmount(new BigDecimal("2000000")) // ✅ Positivo
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> amountViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .filter(v -> v.violationDescription().toLowerCase().contains("amount") && 
                             v.violationDescription().toLowerCase().contains("positive"))
                .toList();
                
            assertThat(amountViolations)
                .describedAs("Non dovrebbe esserci violazione per amount positivo")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL when amount is negative")
        void shouldFailWhenAmountNegative() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .exposureAmount(new BigDecimal("-1000")) // ❌ Negativo
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("ACCURACY");
                    assertThat(violation.violationDescription().toLowerCase()).contains("amount");
                });
        }

        @Test
        @DisplayName("✅ Should PASS with valid EUR currency")
        void shouldPassWithEurCurrency() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .currency("EUR") // ✅ Valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> currencyViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .filter(v -> v.violationDescription().toLowerCase().contains("currency"))
                .toList();
                
            assertThat(currencyViolations)
                .describedAs("EUR è una valuta valida, non dovrebbe esserci violazione")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL with invalid currency code")
        void shouldFailWithInvalidCurrency() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .currency("XYZ") // ❌ Non valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("ACCURACY");
                    assertThat(violation.violationDescription().toLowerCase()).contains("currency");
                });
        }

        @Test
        @DisplayName("✅ Should PASS with valid IT country code")
        void shouldPassWithItCountryCode() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .countryCode("IT") // ✅ Italia valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> countryViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .filter(v -> v.violationDescription().toLowerCase().contains("country"))
                .toList();
                
            assertThat(countryViolations)
                .describedAs("IT è un codice paese valido")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL with invalid country code")
        void shouldFailWithInvalidCountryCode() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .countryCode("XX") // ❌ Non valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("ACCURACY");
                    assertThat(violation.violationDescription().toLowerCase()).contains("country");
                });
        }

        @Test
        @DisplayName("✅ Should PASS with valid 20-character LEI")
        void shouldPassWithValidLei() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .counterpartyLei("815600D7623147C25D86") // ✅ 20 caratteri alfanumerici
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> leiViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .filter(v -> v.violationDescription().toLowerCase().contains("lei"))
                .toList();
                
            assertThat(leiViolations)
                .describedAs("LEI valido non dovrebbe generare violazioni")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL with invalid LEI format (too short)")
        void shouldFailWithShortLei() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .counterpartyLei("815600D7623147") // ❌ Solo 14 caratteri
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("ACCURACY");
                    assertThat(violation.violationDescription().toLowerCase()).contains("lei");
                });
        }

        @Test
        @DisplayName("✅ Should PASS with valid BBB rating")
        void shouldPassWithBbbRating() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .internalRating("BBB") // ✅ Valido (scala S&P)
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> ratingViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .filter(v -> v.violationDescription().toLowerCase().contains("rating"))
                .toList();
                
            assertThat(ratingViolations)
                .describedAs("BBB è un rating valido")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL with invalid rating format")
        void shouldFailWithInvalidRating() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .internalRating("XYZ") // ❌ Non valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("ACCURACY");
                    assertThat(violation.violationDescription().toLowerCase()).contains("rating");
                });
        }

        @Test
        @DisplayName("✅ Should PASS with valid LOAN product type")
        void shouldPassWithLoanProductType() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .productType("LOAN") // ✅ Valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> productViolations = results.ruleViolations().stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .filter(v -> v.violationDescription().toLowerCase().contains("product"))
                .toList();
                
            assertThat(productViolations)
                .describedAs("LOAN è un tipo di prodotto valido")
                .isEmpty();
        }

        @Test
        @DisplayName("❌ Should FAIL with invalid product type")
        void shouldFailWithInvalidProductType() {
            // Arrange
            ExposureRecord exposure = createValidExposure()
                .productType("INVALID_PRODUCT") // ❌ Non valido
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .anySatisfy(violation -> {
                    assertThat(violation.violationType()).isEqualTo("ACCURACY");
                    assertThat(violation.violationDescription().toLowerCase()).contains("product");
                });
        }
    }

    @Nested
    @DisplayName("Scenario Reali (Real-world scenarios)")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("📋 Scenario completo: Esposizione italiana valida")
        void scenarioValidItalianExposure() {
            // Arrange - Esposizione italiana completa e corretta
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP_001_2024")
                .counterpartyId("CP_12345")
                .exposureAmount(new BigDecimal("2000000"))
                .currency("EUR")
                .countryCode("IT")
                .sector("RETAIL")
                .counterpartyType("CORPORATE")
                .productType("LOAN")
                .counterpartyLei("815600D7623147C25D86")
                .internalRating("BBB")
                .riskWeight(new BigDecimal("0.75"))
                .maturityDate(LocalDate.of(2026, 1, 31))
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            assertThat(results.ruleViolations())
                .describedAs("Esposizione valida non dovrebbe avere violazioni")
                .isEmpty();
        }

        @Test
        @DisplayName("📊 Riepilogo accuratezza: Multiple errori")
        void scenarioMultipleErrors() {
            // Arrange - Esposizione con multipli errori
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP_002_2024")
                .counterpartyId(null) // ❌ Errore completezza
                .exposureAmount(new BigDecimal("-1000")) // ❌ Errore accuratezza (negativo)
                .currency("XYZ") // ❌ Errore accuratezza (valuta invalida)
                .countryCode("XX") // ❌ Errore accuratezza (paese invalido)
                .sector("RETAIL")
                .counterpartyLei("SHORT") // ❌ Errore accuratezza (LEI troppo corto)
                .internalRating("INVALID") // ❌ Errore accuratezza (rating invalido)
                .maturityDate(LocalDate.now().plusYears(1))
                .build();

            // Act
            ValidationResults results = dataQualityRulesService.validateNoPersist(exposure);

            // Assert
            List<RuleViolation> violations = results.ruleViolations();
            
            // Verifica che ci siano multiple violazioni
            assertThat(violations).hasSizeGreaterThan(3);

            // Verifica violazioni completezza
            List<RuleViolation> completenessViolations = violations.stream()
                .filter(v -> v.violationType().equals("COMPLETENESS"))
                .toList();
            assertThat(completenessViolations).isNotEmpty();

            // Verifica violazioni accuratezza
            List<RuleViolation> accuracyViolations = violations.stream()
                .filter(v -> v.violationType().equals("ACCURACY"))
                .toList();
            assertThat(accuracyViolations).hasSizeGreaterThan(2);

            // Stampa riepilogo per debug
            System.out.println("\n═══════════════════════════════════════════");
            System.out.println("RIEPILOGO ACCURATEZZA");
            System.out.println("═══════════════════════════════════════════");
            System.out.println("Controlli Falliti: " + violations.size());
            System.out.println("\nBreakdown Errori:");
            
            violations.forEach(v -> {
                System.out.println("├─ Tipo: " + v.violationType());
                System.out.println("└─ Messaggio: " + v.violationDescription());
                System.out.println();
            });
        }
    }

    // Helper method per creare esposizioni valide
    private ExposureRecord.Builder createValidExposure() {
        return ExposureRecord.builder()
            .exposureId("EXP_001_2024")
            .counterpartyId("CP_12345")
            .exposureAmount(new BigDecimal("2000000"))
            .currency("EUR")
            .countryCode("IT")
            .sector("RETAIL")
            .counterpartyType("CORPORATE")
            .productType("LOAN")
            .counterpartyLei("815600D7623147C25D86")
            .internalRating("BBB")
            .riskWeight(new BigDecimal("0.75"))
            .reportingDate(LocalDate.now().minusDays(10))
            .valuationDate(LocalDate.now().minusDays(5))
            .maturityDate(LocalDate.now().plusYears(1))
            .referenceNumber("REF_001");
    }
}
