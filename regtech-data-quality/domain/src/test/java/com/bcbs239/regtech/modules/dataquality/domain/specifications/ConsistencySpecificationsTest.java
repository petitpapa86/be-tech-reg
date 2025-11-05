package com.bcbs239.regtech.modules.dataquality.domain.specifications;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.dataquality.domain.specifications.ConsistencySpecifications;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

@DisplayName("ConsistencySpecifications Tests")
class ConsistencySpecificationsTest {

    @Nested
    @DisplayName("Currency-Country Consistency Tests")
    class CurrencyCountryConsistencyTests {

        @Test
        @DisplayName("Should pass when currency matches country")
        void shouldPassWhenCurrencyMatchesCountry() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("USD")
                .country("US")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.currencyMatchesCountry();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should pass when EUR currency matches eurozone country")
        void shouldPassWhenEurCurrencyMatchesEurozoneCountry() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("EUR")
                .country("DE")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.currencyMatchesCountry();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail when currency does not match country")
        void shouldFailWhenCurrencyDoesNotMatchCountry() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("USD")
                .country("DE")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.currencyMatchesCountry();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertEquals("CONSISTENCY_CURRENCY_COUNTRY_MISMATCH", result.getError().get().getCode());
            assertTrue(result.getError().get().getMessage().contains("USD"));
            assertTrue(result.getError().get().getMessage().contains("DE"));
        }

        @Test
        @DisplayName("Should pass when currency or country is null")
        void shouldPassWhenCurrencyOrCountryIsNull() {
            // Given
            ExposureRecord exposureNullCurrency = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency(null)
                .country("US")
                .build();

            ExposureRecord exposureNullCountry = ExposureRecord.builder()
                .exposureId("EXP002")
                .currency("USD")
                .country(null)
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.currencyMatchesCountry();

            // When & Then
            assertTrue(spec.isSatisfiedBy(exposureNullCurrency).isSuccess());
            assertTrue(spec.isSatisfiedBy(exposureNullCountry).isSuccess());
        }

        @Test
        @DisplayName("Should pass when currency is not in mapping (unknown currency)")
        void shouldPassWhenCurrencyNotInMapping() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("XYZ")
                .country("US")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.currencyMatchesCountry();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Sector-CounterpartyType Consistency Tests")
    class SectorCounterpartyTypeConsistencyTests {

        @Test
        @DisplayName("Should pass when banking sector matches bank counterparty type")
        void shouldPassWhenBankingSectorMatchesBankCounterpartyType() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .sector("BANKING")
                .counterpartyType("BANK")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.sectorMatchesCounterpartyType();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should pass when corporate sector matches corporate counterparty type")
        void shouldPassWhenCorporateSectorMatchesCorporateCounterpartyType() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .sector("CORPORATE_MANUFACTURING")
                .counterpartyType("CORPORATE")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.sectorMatchesCounterpartyType();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail when sector does not match counterparty type")
        void shouldFailWhenSectorDoesNotMatchCounterpartyType() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .sector("BANKING")
                .counterpartyType("INDIVIDUAL")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.sectorMatchesCounterpartyType();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertEquals("CONSISTENCY_SECTOR_COUNTERPARTY_MISMATCH", result.getError().get().getCode());
            assertTrue(result.getError().get().getMessage().contains("BANKING"));
            assertTrue(result.getError().get().getMessage().contains("INDIVIDUAL"));
        }

        @Test
        @DisplayName("Should pass when sector or counterparty type is null")
        void shouldPassWhenSectorOrCounterpartyTypeIsNull() {
            // Given
            ExposureRecord exposureNullSector = ExposureRecord.builder()
                .exposureId("EXP001")
                .sector(null)
                .counterpartyType("BANK")
                .build();

            ExposureRecord exposureNullCounterpartyType = ExposureRecord.builder()
                .exposureId("EXP002")
                .sector("BANKING")
                .counterpartyType(null)
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.sectorMatchesCounterpartyType();

            // When & Then
            assertTrue(spec.isSatisfiedBy(exposureNullSector).isSuccess());
            assertTrue(spec.isSatisfiedBy(exposureNullCounterpartyType).isSuccess());
        }
    }

    @Nested
    @DisplayName("Rating-RiskCategory Consistency Tests")
    class RatingRiskCategoryConsistencyTests {

        @Test
        @DisplayName("Should pass when AAA rating matches low risk category")
        void shouldPassWhenAaaRatingMatchesLowRiskCategory() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .internalRating("AAA")
                .riskCategory("LOW_RISK")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.ratingMatchesRiskCategory();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should pass when rating with modifier matches risk category")
        void shouldPassWhenRatingWithModifierMatchesRiskCategory() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .internalRating("AA+")
                .riskCategory("INVESTMENT_GRADE")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.ratingMatchesRiskCategory();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail when rating does not match risk category")
        void shouldFailWhenRatingDoesNotMatchRiskCategory() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .internalRating("AAA")
                .riskCategory("HIGH_RISK")
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.ratingMatchesRiskCategory();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertEquals("CONSISTENCY_RATING_RISK_MISMATCH", result.getError().get().getCode());
            assertTrue(result.getError().get().getMessage().contains("AAA"));
            assertTrue(result.getError().get().getMessage().contains("HIGH_RISK"));
        }

        @Test
        @DisplayName("Should pass when rating or risk category is null")
        void shouldPassWhenRatingOrRiskCategoryIsNull() {
            // Given
            ExposureRecord exposureNullRating = ExposureRecord.builder()
                .exposureId("EXP001")
                .internalRating(null)
                .riskCategory("LOW_RISK")
                .build();

            ExposureRecord exposureNullRiskCategory = ExposureRecord.builder()
                .exposureId("EXP002")
                .internalRating("AAA")
                .riskCategory(null)
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.ratingMatchesRiskCategory();

            // When & Then
            assertTrue(spec.isSatisfiedBy(exposureNullRating).isSuccess());
            assertTrue(spec.isSatisfiedBy(exposureNullRiskCategory).isSuccess());
        }
    }

    @Nested
    @DisplayName("ProductType-Maturity Consistency Tests")
    class ProductTypeMaturityConsistencyTests {

        @Test
        @DisplayName("Should pass when loan product has maturity date")
        void shouldPassWhenLoanProductHasMaturityDate() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .productType("LOAN")
                .maturityDate(LocalDate.now().plusYears(5))
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.productTypeMatchesMaturity();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should pass when equity product has no maturity date")
        void shouldPassWhenEquityProductHasNoMaturityDate() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .productType("EQUITY")
                .maturityDate(null)
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.productTypeMatchesMaturity();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail when loan product has no maturity date")
        void shouldFailWhenLoanProductHasNoMaturityDate() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .productType("LOAN")
                .maturityDate(null)
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.productTypeMatchesMaturity();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertEquals("CONSISTENCY_PRODUCT_MATURITY_MISSING", result.getError().get().getCode());
            assertTrue(result.getError().get().getMessage().contains("LOAN"));
        }

        @Test
        @DisplayName("Should fail when equity product has maturity date")
        void shouldFailWhenEquityProductHasMaturityDate() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .productType("EQUITY")
                .maturityDate(LocalDate.now().plusYears(1))
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.productTypeMatchesMaturity();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertEquals("CONSISTENCY_PRODUCT_MATURITY_UNEXPECTED", result.getError().get().getCode());
            assertTrue(result.getError().get().getMessage().contains("EQUITY"));
        }

        @Test
        @DisplayName("Should pass when product type is null")
        void shouldPassWhenProductTypeIsNull() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .productType(null)
                .maturityDate(LocalDate.now().plusYears(1))
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.productTypeMatchesMaturity();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should pass when product type is not in known categories")
        void shouldPassWhenProductTypeIsNotInKnownCategories() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .productType("UNKNOWN_PRODUCT")
                .maturityDate(null)
                .build();

            Specification<ExposureRecord> spec = ConsistencySpecifications.productTypeMatchesMaturity();

            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);

            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Specification Composition Tests")
    class SpecificationCompositionTests {

        @Test
        @DisplayName("Should support AND composition of consistency specifications")
        void shouldSupportAndCompositionOfConsistencySpecifications() {
            // Given
            ExposureRecord validExposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("USD")
                .country("US")
                .sector("BANKING")
                .counterpartyType("BANK")
                .internalRating("AAA")
                .riskCategory("LOW_RISK")
                .productType("LOAN")
                .maturityDate(LocalDate.now().plusYears(5))
                .build();

            ExposureRecord invalidExposure = ExposureRecord.builder()
                .exposureId("EXP002")
                .currency("USD")
                .country("DE") // Invalid currency-country combination
                .sector("BANKING")
                .counterpartyType("BANK")
                .internalRating("AAA")
                .riskCategory("LOW_RISK")
                .productType("LOAN")
                .maturityDate(LocalDate.now().plusYears(5))
                .build();

            Specification<ExposureRecord> compositeSpec = 
                ConsistencySpecifications.currencyMatchesCountry()
                    .and(ConsistencySpecifications.sectorMatchesCounterpartyType())
                    .and(ConsistencySpecifications.ratingMatchesRiskCategory())
                    .and(ConsistencySpecifications.productTypeMatchesMaturity());

            // When & Then
            assertTrue(compositeSpec.isSatisfiedBy(validExposure).isSuccess());
            assertFalse(compositeSpec.isSatisfiedBy(invalidExposure).isSuccess());
        }

        @Test
        @DisplayName("Should support OR composition of consistency specifications")
        void shouldSupportOrCompositionOfConsistencySpecifications() {
            // Given
            ExposureRecord partiallyValidExposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("USD")
                .country("US") // Valid currency-country
                .sector("BANKING")
                .counterpartyType("INDIVIDUAL") // Invalid sector-counterparty
                .build();

            Specification<ExposureRecord> compositeSpec = 
                ConsistencySpecifications.currencyMatchesCountry()
                    .or(ConsistencySpecifications.sectorMatchesCounterpartyType());

            // When
            Result<Void> result = compositeSpec.isSatisfiedBy(partiallyValidExposure);

            // Then
            assertTrue(result.isSuccess()); // Should pass because currency-country is valid
        }
    }
}