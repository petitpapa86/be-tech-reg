package com.bcbs239.regtech.modules.dataquality.domain.specifications;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ExposureRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TimelinessSpecificationsTest {

    @Test
    void isWithinReportingPeriod_shouldSucceedForValidReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(LocalDate.now().minusDays(30));
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinReportingPeriod();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void isWithinReportingPeriod_shouldFailForFutureReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(LocalDate.now().plusDays(1));
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinReportingPeriod();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_FUTURE_REPORTING_DATE", result.getError().get().getCode());
        assertEquals("reporting_date", result.getError().get().getField());
    }

    @Test
    void isWithinReportingPeriod_shouldFailForStaleReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(LocalDate.now().minusDays(100));
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinReportingPeriod();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_STALE_REPORTING_DATE", result.getError().get().getCode());
        assertEquals("reporting_date", result.getError().get().getField());
        assertTrue(result.getError().get().getMessage().contains("100 days"));
    }

    @Test
    void isWithinReportingPeriod_shouldSucceedForNullReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(null);
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinReportingPeriod();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasRecentValuation_shouldSucceedForRecentValuationDate() {
        // Given
        ExposureRecord exposure = createExposureWithValuationDate(LocalDate.now().minusDays(15));
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasRecentValuation();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasRecentValuation_shouldFailForFutureValuationDate() {
        // Given
        ExposureRecord exposure = createExposureWithValuationDate(LocalDate.now().plusDays(1));
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasRecentValuation();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_FUTURE_VALUATION_DATE", result.getError().get().getCode());
        assertEquals("valuation_date", result.getError().get().getField());
    }

    @Test
    void hasRecentValuation_shouldFailForStaleValuationDate() {
        // Given
        ExposureRecord exposure = createExposureWithValuationDate(LocalDate.now().minusDays(40));
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasRecentValuation();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_STALE_VALUATION", result.getError().get().getCode());
        assertEquals("valuation_date", result.getError().get().getField());
        assertTrue(result.getError().get().getMessage().contains("40 days"));
    }

    @Test
    void hasRecentValuation_shouldSucceedForNullValuationDate() {
        // Given
        ExposureRecord exposure = createExposureWithValuationDate(null);
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasRecentValuation();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void isNotFutureDate_shouldSucceedForPastDates() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .reportingDate(LocalDate.now().minusDays(1))
            .valuationDate(LocalDate.now().minusDays(2))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.isNotFutureDate();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void isNotFutureDate_shouldFailForFutureReportingDate() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .reportingDate(LocalDate.now().plusDays(1))
            .valuationDate(LocalDate.now().minusDays(1))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.isNotFutureDate();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_FUTURE_REPORTING_DATE", result.getError().getCode());
    }

    @Test
    void isNotFutureDate_shouldFailForFutureValuationDate() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .reportingDate(LocalDate.now().minusDays(1))
            .valuationDate(LocalDate.now().plusDays(1))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.isNotFutureDate();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_FUTURE_VALUATION_DATE", result.getError().getCode());
    }

    @Test
    void isWithinProcessingWindow_shouldSucceedForRecentReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(LocalDate.now().minusDays(2));
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinProcessingWindow();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void isWithinProcessingWindow_shouldFailForOldReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(LocalDate.now().minusDays(10));
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinProcessingWindow();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_PROCESSING_WINDOW_EXCEEDED", result.getError().getCode());
        assertEquals("reporting_date", result.getError().getField());
    }

    @Test
    void isWithinProcessingWindow_shouldSucceedForNullReportingDate() {
        // Given
        ExposureRecord exposure = createExposureWithReportingDate(null);
        Specification<ExposureRecord> spec = TimelinessSpecifications.isWithinProcessingWindow();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasReasonableMaturityDate_shouldSucceedForValidMaturityDate() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .reportingDate(LocalDate.now().minusDays(1))
            .maturityDate(LocalDate.now().plusYears(5))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasReasonableMaturityDate();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasReasonableMaturityDate_shouldFailForMaturityBeforeReporting() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .reportingDate(LocalDate.now())
            .maturityDate(LocalDate.now().minusDays(1))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasReasonableMaturityDate();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_MATURITY_BEFORE_REPORTING", result.getError().getCode());
    }

    @Test
    void hasReasonableMaturityDate_shouldFailForUnreasonablyLongMaturity() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .reportingDate(LocalDate.now())
            .maturityDate(LocalDate.now().plusYears(60))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasReasonableMaturityDate();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_UNREASONABLE_MATURITY", result.getError().getCode());
        assertTrue(result.getError().getMessage().contains("60 years"));
    }

    @Test
    void hasConsistentDates_shouldSucceedForConsistentDates() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .reportingDate(LocalDate.now().minusDays(1))
            .valuationDate(LocalDate.now().minusDays(2))
            .maturityDate(LocalDate.now().plusYears(1))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasConsistentDates();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void hasConsistentDates_shouldFailForValuationAfterReporting() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .reportingDate(LocalDate.now().minusDays(10))
            .valuationDate(LocalDate.now().minusDays(1))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasConsistentDates();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_VALUATION_AFTER_REPORTING", result.getError().getCode());
    }

    @Test
    void hasConsistentDates_shouldFailForMaturityBeforeReportingForTermExposure() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .productType("LOAN")
            .reportingDate(LocalDate.now())
            .maturityDate(LocalDate.now().minusDays(1))
            .build();
        Specification<ExposureRecord> spec = TimelinessSpecifications.hasConsistentDates();
        
        // When
        Result<Void> result = spec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_MATURITY_BEFORE_REPORTING", result.getError().getCode());
    }

    @Test
    void specificationComposition_shouldWorkWithAndOperator() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .reportingDate(LocalDate.now().minusDays(1))
            .valuationDate(LocalDate.now().minusDays(2))
            .build();
        
        Specification<ExposureRecord> composedSpec = 
            TimelinessSpecifications.isWithinReportingPeriod()
                .and(TimelinessSpecifications.hasRecentValuation())
                .and(TimelinessSpecifications.isNotFutureDate());
        
        // When
        Result<Void> result = composedSpec.isSatisfiedBy(exposure);
        
        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void specificationComposition_shouldFailWhenOneSpecificationFails() {
        // Given
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP001")
            .reportingDate(LocalDate.now().plusDays(1)) // Future date - should fail
            .valuationDate(LocalDate.now().minusDays(2))
            .build();
        
        Specification<ExposureRecord> composedSpec = 
            TimelinessSpecifications.isWithinReportingPeriod()
                .and(TimelinessSpecifications.hasRecentValuation())
                .and(TimelinessSpecifications.isNotFutureDate());
        
        // When
        Result<Void> result = composedSpec.isSatisfiedBy(exposure);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("TIMELINESS_FUTURE_REPORTING_DATE", result.getError().getCode());
    }

    // Helper methods
    private ExposureRecord createExposureWithReportingDate(LocalDate reportingDate) {
        return ExposureRecord.builder()
            .exposureId("EXP001")
            .counterpartyId("CP001")
            .amount(BigDecimal.valueOf(100000))
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .reportingDate(reportingDate)
            .build();
    }

    private ExposureRecord createExposureWithValuationDate(LocalDate valuationDate) {
        return ExposureRecord.builder()
            .exposureId("EXP001")
            .counterpartyId("CP001")
            .amount(BigDecimal.valueOf(100000))
            .currency("USD")
            .country("US")
            .sector("CORPORATE")
            .valuationDate(valuationDate)
            .build();
    }
}