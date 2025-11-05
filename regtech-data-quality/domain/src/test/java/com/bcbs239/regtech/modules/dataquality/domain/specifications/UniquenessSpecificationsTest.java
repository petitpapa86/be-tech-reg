package com.bcbs239.regtech.modules.dataquality.domain.specifications;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.dataquality.domain.specifications.UniquenessSpecifications;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

class UniquenessSpecificationsTest {

    @Test
    void hasUniqueExposureIds_withUniqueIds_shouldSucceed() {
        // Arrange
        List<ExposureRecord> exposures = Arrays.asList(
            createExposureRecord("EXP001", "CP001", "REF001"),
            createExposureRecord("EXP002", "CP002", "REF002")
        );
        
        Specification<List<ExposureRecord>> spec = UniquenessSpecifications.hasUniqueExposureIds();
        
        // Act
        Result<Void> result = spec.isSatisfiedBy(exposures);
        
        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    void hasUniqueExposureIds_withDuplicateIds_shouldFail() {
        // Arrange
        List<ExposureRecord> exposures = Arrays.asList(
            createExposureRecord("EXP001", "CP001", "REF001"),
            createExposureRecord("EXP001", "CP002", "REF002") // Duplicate exposure ID
        );
        
        Specification<List<ExposureRecord>> spec = UniquenessSpecifications.hasUniqueExposureIds();
        
        // Act
        Result<Void> result = spec.isSatisfiedBy(exposures);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("UNIQUENESS_DUPLICATE_EXPOSURE_IDS", result.getError().get().getCode());
        assertTrue(result.getError().get().getMessage().contains("EXP001"));
        assertEquals("exposure_id", result.getError().get().getField());
    }

    @Test
    void hasUniqueCounterpartyExposurePairs_withUniquePairs_shouldSucceed() {
        // Arrange
        List<ExposureRecord> exposures = Arrays.asList(
            createExposureRecord("EXP001", "CP001", "REF001"),
            createExposureRecord("EXP002", "CP001", "REF002") // Same counterparty, different exposure
        );
        
        Specification<List<ExposureRecord>> spec = UniquenessSpecifications.hasUniqueCounterpartyExposurePairs();
        
        // Act
        Result<Void> result = spec.isSatisfiedBy(exposures);
        
        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    void hasUniqueCounterpartyExposurePairs_withDuplicatePairs_shouldFail() {
        // Arrange
        List<ExposureRecord> exposures = Arrays.asList(
            createExposureRecord("EXP001", "CP001", "REF001"),
            createExposureRecord("EXP001", "CP001", "REF002") // Duplicate counterparty-exposure pair
        );
        
        Specification<List<ExposureRecord>> spec = UniquenessSpecifications.hasUniqueCounterpartyExposurePairs();
        
        // Act
        Result<Void> result = spec.isSatisfiedBy(exposures);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("UNIQUENESS_DUPLICATE_COUNTERPARTY_EXPOSURE", result.getError().get().getCode());
        assertTrue(result.getError().get().getMessage().contains("CP001:EXP001"));
        assertEquals("counterparty_id", result.getError().get().getField());
    }

    @Test
    void hasUniqueReferenceNumbers_withUniqueReferences_shouldSucceed() {
        // Arrange
        List<ExposureRecord> exposures = Arrays.asList(
            createExposureRecord("EXP001", "CP001", "REF001"),
            createExposureRecord("EXP002", "CP002", "REF002")
        );
        
        Specification<List<ExposureRecord>> spec = UniquenessSpecifications.hasUniqueReferenceNumbers();
        
        // Act
        Result<Void> result = spec.isSatisfiedBy(exposures);
        
        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    void hasUniqueReferenceNumbers_withDuplicateReferences_shouldFail() {
        // Arrange
        List<ExposureRecord> exposures = Arrays.asList(
            createExposureRecord("EXP001", "CP001", "REF001"),
            createExposureRecord("EXP002", "CP002", "REF001") // Duplicate reference number
        );
        
        Specification<List<ExposureRecord>> spec = UniquenessSpecifications.hasUniqueReferenceNumbers();
        
        // Act
        Result<Void> result = spec.isSatisfiedBy(exposures);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("UNIQUENESS_DUPLICATE_REFERENCE_NUMBERS", result.getError().get().getCode());
        assertTrue(result.getError().get().getMessage().contains("REF001"));
        assertEquals("reference_number", result.getError().get().getField());
    }

    private ExposureRecord createExposureRecord(String exposureId, String counterpartyId, String referenceNumber) {
        return ExposureRecord.builder()
            .exposureId(exposureId)
            .counterpartyId(counterpartyId)
            .amount(BigDecimal.valueOf(1000000))
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .counterpartyType("CORPORATE")
            .productType("LOAN")
            .leiCode("1234567890ABCDEFGHIJ")
            .internalRating("A")
            .riskCategory("LOW")
            .riskWeight(BigDecimal.valueOf(0.5))
            .reportingDate(LocalDate.now())
            .valuationDate(LocalDate.now())
            .maturityDate(LocalDate.now().plusYears(1))
            .referenceNumber(referenceNumber)
            .build();
    }
}