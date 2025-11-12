package com.bcbs239.regtech.modules.dataquality.domain.specifications;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.dataquality.domain.specifications.CompletenessSpecifications;
import com.bcbs239.regtech.dataquality.domain.specifications.UniquenessSpecifications;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CompletenessSpecificationsTest {

    @Test
    void hasRequiredFields_shouldSucceed_whenAllRequiredFieldsPresent() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .amount(new BigDecimal("1000000"))
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasRequiredFields();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasRequiredFields_shouldFail_whenExposureIdMissing() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId(null)
            .amount(new BigDecimal("1000000"))
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasRequiredFields();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_EXPOSURE_ID_MISSING", result.getError().get().getCode());
    }

    @Test
    void hasRequiredFields_shouldFail_whenAmountMissing() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .amount(null)
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasRequiredFields();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_AMOUNT_MISSING", result.getError().get().getCode());
    }

    @Test
    void hasRequiredFields_shouldFail_whenMultipleFieldsMissing() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .amount(null)
            .currency(null)
            .country("US")
            .sector("CORPORATE")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasRequiredFields();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("COMPLETENESS_AMOUNT_MISSING")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("COMPLETENESS_CURRENCY_MISSING")));
    }

    @Test
    void hasLeiForCorporates_shouldSucceed_whenCorporateHasLei() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .sector("CORPORATE")
            .leiCode("549300ABCDEFGHIJKLMN01")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasLeiForCorporates();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasLeiForCorporates_shouldFail_whenCorporateMissingLei() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .sector("CORPORATE")
            .leiCode(null)
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasLeiForCorporates();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_LEI_MISSING", result.getError().get().getCode());
    }

    @Test
    void hasLeiForCorporates_shouldSucceed_whenNonCorporateMissingLei() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .sector("RETAIL")
            .leiCode(null)
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasLeiForCorporates();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasLeiForCorporates_shouldFail_whenBankingMissingLei() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .sector("BANKING")
            .leiCode(null)
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasLeiForCorporates();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_LEI_MISSING", result.getError().get().getCode());
    }

    @Test
    void hasMaturityForTermExposures_shouldSucceed_whenTermExposureHasMaturity() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .maturityDate(LocalDate.of(2025, 12, 31))
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasMaturityForTermExposures();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasMaturityForTermExposures_shouldFail_whenTermExposureMissingMaturity() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .maturityDate(null)
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasMaturityForTermExposures();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_MATURITY_MISSING", result.getError().get().getCode());
    }

    @Test
    void hasMaturityForTermExposures_shouldSucceed_whenEquityMissingMaturity() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("EQUITY")
            .maturityDate(null)
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasMaturityForTermExposures();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasInternalRating_shouldSucceed_whenRatingPresent() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .internalRating("A+")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasInternalRating();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasInternalRating_shouldFail_whenRatingMissing() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .internalRating(null)
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasInternalRating();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_INTERNAL_RATING_MISSING", result.getError().get().getCode());
    }

    @Test
    void hasInternalRating_shouldFail_whenRatingBlank() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .internalRating("   ")
            .build();

        Specification<ExposureRecord> spec = CompletenessSpecifications.hasInternalRating();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals("COMPLETENESS_INTERNAL_RATING_MISSING", result.getError().get().getCode());
    }

    @Test
    void specifications_canBeComposed_withAndOperator() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .amount(new BigDecimal("1000000"))
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .leiCode("549300ABCDEFGHIJKLMN01")
            .internalRating("A+")
            .build();

        Specification<ExposureRecord> composedSpec = 
            CompletenessSpecifications.hasRequiredFields()
                .and(CompletenessSpecifications.hasLeiForCorporates())
                .and(CompletenessSpecifications.hasInternalRating());

        // When
        Result<Void> result = composedSpec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void specifications_canBeComposed_withAndOperator_collectingAllErrors() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .amount(null) // Missing required field
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .leiCode(null) // Missing LEI for corporate
            .internalRating(null) // Missing rating
            .build();

        Specification<ExposureRecord> composedSpec = 
            CompletenessSpecifications.hasRequiredFields()
                .and(CompletenessSpecifications.hasLeiForCorporates())
                .and(CompletenessSpecifications.hasInternalRating());

        // When
        Result<Void> result = composedSpec.isSatisfiedBy(exposure);

        // Then
        assertTrue(result.isFailure());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("COMPLETENESS_AMOUNT_MISSING")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("COMPLETENESS_LEI_MISSING")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("COMPLETENESS_INTERNAL_RATING_MISSING")));
    }

    @Test
    void uniquenessSpecifications_hasUniqueExposureIds_shouldWork() {
        // Given - Test the UniquenessSpecifications to verify it's working
        java.util.List<ExposureRecord> exposures = java.util.Arrays.asList(
            ExposureRecord.builder()
                .exposureId("EXP001")
                .counterpartyId("CP001")
                .amount(new BigDecimal("1000000"))
                .currency("USD")
                .country("US")
                .sector("CORPORATE")
                .referenceNumber("REF001")
                .build(),
            ExposureRecord.builder()
                .exposureId("EXP002")
                .counterpartyId("CP002")
                .amount(new BigDecimal("2000000"))
                .currency("EUR")
                .country("DE")
                .sector("BANKING")
                .referenceNumber("REF002")
                .build()
        );

        com.bcbs239.regtech.core.shared.Specification<java.util.List<ExposureRecord>> spec = 
            UniquenessSpecifications.hasUniqueExposureIds();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposures);

        // Then
        assertTrue(result.isSuccess(), "UniquenessSpecifications.hasUniqueExposureIds() should work with unique IDs");
    }

    @Test
    void uniquenessSpecifications_hasUniqueExposureIds_shouldFailWithDuplicates() {
        // Given - Test the UniquenessSpecifications with duplicate IDs
        java.util.List<ExposureRecord> exposures = java.util.Arrays.asList(
            ExposureRecord.builder()
                .exposureId("EXP001")
                .counterpartyId("CP001")
                .amount(new BigDecimal("1000000"))
                .currency("USD")
                .country("US")
                .sector("CORPORATE")
                .referenceNumber("REF001")
                .build(),
            ExposureRecord.builder()
                .exposureId("EXP001") // Duplicate ID
                .counterpartyId("CP002")
                .amount(new BigDecimal("2000000"))
                .currency("EUR")
                .country("DE")
                .sector("BANKING")
                .referenceNumber("REF002")
                .build()
        );

        com.bcbs239.regtech.core.shared.Specification<java.util.List<ExposureRecord>> spec = 
            UniquenessSpecifications.hasUniqueExposureIds();

        // When
        Result<Void> result = spec.isSatisfiedBy(exposures);

        // Then
        assertFalse(result.isSuccess(), "UniquenessSpecifications.hasUniqueExposureIds() should fail with duplicate IDs");
        assertEquals("UNIQUENESS_DUPLICATE_EXPOSURE_IDS", result.getError().get().getCode());
        assertTrue(result.getError().get().getMessage().contains("EXP001"));
    }}

