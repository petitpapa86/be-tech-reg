package com.bcbs239.regtech.dataquality.domain.specifications;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Specifications for Timeliness quality dimension validation.
 * 
 * Timeliness ensures that data freshness and processing deadlines
 * are validated according to BCBS 239 regulatory requirements.
 * 
 * @deprecated This class is deprecated and will be removed in a future release.
 *             All validation logic has been migrated to the database-driven Rules Engine.
 *             Use {@link com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService}
 *             for configurable, database-driven validation instead.
 *             See the Rules Engine Configuration Guide for migration details.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class TimelinessSpecifications {

    // Maximum age for reporting data: 90 days
    private static final long MAX_REPORTING_AGE_DAYS = 90;
    
    // Maximum age for valuation data: 30 days
    private static final long MAX_VALUATION_AGE_DAYS = 30;
    
    // Maximum processing window: 5 business days from reporting date
    private static final long MAX_PROCESSING_WINDOW_DAYS = 5;

    /**
     * Validates that the reporting date is within the acceptable reporting period.
     * Reporting dates should not be in the future and not older than 90 days.
     * 
     * @return Specification that validates reporting period compliance
     */
    public static Specification<ExposureRecord> isWithinReportingPeriod() {
        return exposure -> {
            if (exposure.reportingDate() == null) {
                return Result.success(); // Completeness validation handles missing dates
            }
            
            LocalDate reportingDate = exposure.reportingDate();
            LocalDate now = LocalDate.now();
            
            // Check if reporting date is in the future
            if (reportingDate.isAfter(now)) {
                return Result.failure(ErrorDetail.of("TIMELINESS_FUTURE_REPORTING_DATE",
                    ErrorType.VALIDATION_ERROR,
                    "Reporting date cannot be in the future", "timeliness.future.reporting.date"));
            }
            
            // Check if reporting date is too old (stale data)
            long daysBetween = ChronoUnit.DAYS.between(reportingDate, now);
            if (daysBetween > MAX_REPORTING_AGE_DAYS) {
                return Result.failure(ErrorDetail.of("TIMELINESS_STALE_REPORTING_DATE", 
                    ErrorType.VALIDATION_ERROR,
                    String.format("Reporting date is too old (%d days, max %d days)", 
                        daysBetween, MAX_REPORTING_AGE_DAYS), "timeliness.stale.reporting.date"));
            }
            
            return Result.success();
        };
    }

    /**
     * Validates that the valuation date is recent enough for accurate risk assessment.
     * Valuation dates should not be older than 30 days from current date.
     * 
     * @return Specification that validates valuation date freshness
     */
    public static Specification<ExposureRecord> hasRecentValuation() {
        return exposure -> {
            if (exposure.valuationDate() == null) {
                return Result.success(); // Completeness validation handles missing dates
            }
            
            LocalDate valuationDate = exposure.valuationDate();
            LocalDate now = LocalDate.now();
            
            // Check if valuation date is in the future
            if (valuationDate.isAfter(now)) {
                return Result.failure(ErrorDetail.of("TIMELINESS_FUTURE_VALUATION_DATE", 
                    ErrorType.VALIDATION_ERROR,
                    "Valuation date cannot be in the future", "timeliness.future.valuation.date"));
            }
            
            // Check if valuation is too old (stale valuation)
            long daysBetween = ChronoUnit.DAYS.between(valuationDate, now);
            if (daysBetween > MAX_VALUATION_AGE_DAYS) {
                return Result.failure(ErrorDetail.of("TIMELINESS_STALE_VALUATION", 
                    ErrorType.VALIDATION_ERROR,
                    String.format("Valuation date is too old (%d days, max %d days)", 
                        daysBetween, MAX_VALUATION_AGE_DAYS), "timeliness.stale.valuation"));
            }
            
            return Result.success();
        };
    }

    /**
     * Validates that dates are not in the future.
     * This is a general validation for any date field that should not be future-dated.
     * 
     * @return Specification that validates dates are not in the future
     */
    public static Specification<ExposureRecord> isNotFutureDate() {
        return exposure -> {
            LocalDate now = LocalDate.now();
            
            // Check reporting date
            if (exposure.reportingDate() != null && exposure.reportingDate().isAfter(now)) {
                return Result.failure(ErrorDetail.of("TIMELINESS_FUTURE_REPORTING_DATE", 
                    ErrorType.VALIDATION_ERROR,
                    "Reporting date cannot be in the future", "timeliness.future.reporting.date"));
            }
            
            // Check valuation date
            if (exposure.valuationDate() != null && exposure.valuationDate().isAfter(now)) {
                return Result.failure(ErrorDetail.of("TIMELINESS_FUTURE_VALUATION_DATE", 
                    ErrorType.VALIDATION_ERROR,
                    "Valuation date cannot be in the future", "timeliness.future.valuation.date"));
            }
            
            // Note: Maturity date can be in the future, so we don't validate it here
            
            return Result.success();
        };
    }

    /**
     * Validates that the exposure data is within the acceptable processing window.
     * Data should be processed within 5 business days of the reporting date.
     * 
     * @return Specification that validates processing window compliance
     */
    public static Specification<ExposureRecord> isWithinProcessingWindow() {
        return exposure -> {
            if (exposure.reportingDate() == null) {
                return Result.success(); // Completeness validation handles missing dates
            }
            
            LocalDate reportingDate = exposure.reportingDate();
            LocalDate now = LocalDate.now();
            
            // Only validate if reporting date is in the past
            if (!reportingDate.isAfter(now)) {
                long daysBetween = ChronoUnit.DAYS.between(reportingDate, now);
                
                // Convert to business days (rough approximation: 5/7 of calendar days)
                long businessDaysApprox = (daysBetween * 5) / 7;
                
                if (businessDaysApprox > MAX_PROCESSING_WINDOW_DAYS) {
                    return Result.failure(ErrorDetail.of("TIMELINESS_PROCESSING_WINDOW_EXCEEDED", 
                        ErrorType.VALIDATION_ERROR,
                        String.format("Processing window exceeded (%d business days, max %d days)", 
                            businessDaysApprox, MAX_PROCESSING_WINDOW_DAYS), "timeliness.processing.window.exceeded"));
                }
            }
            
            return Result.success();
        };
    }

    /**
     * Validates that the maturity date is reasonable relative to the reporting date.
     * For term exposures, maturity should be after reporting date but within reasonable bounds.
     * 
     * @return Specification that validates maturity date reasonableness
     */
    public static Specification<ExposureRecord> hasReasonableMaturityDate() {
        return exposure -> {
            if (exposure.maturityDate() == null || exposure.reportingDate() == null) {
                return Result.success(); // Other validations handle missing dates
            }
            
            LocalDate maturityDate = exposure.maturityDate();
            LocalDate reportingDate = exposure.reportingDate();
            
            // Maturity date should be after reporting date for term exposures
            if (exposure.isTermExposure() && maturityDate.isBefore(reportingDate)) {
                return Result.failure(ErrorDetail.of("TIMELINESS_MATURITY_BEFORE_REPORTING", 
                    ErrorType.VALIDATION_ERROR,
                    "Maturity date cannot be before reporting date for term exposures", "timeliness.maturity.before.reporting"));
            }
            
            // Check for unreasonably long maturities (more than 50 years)
            long yearsBetween = ChronoUnit.YEARS.between(reportingDate, maturityDate);
            if (yearsBetween > 50) {
                return Result.failure(ErrorDetail.of("TIMELINESS_UNREASONABLE_MATURITY", 
                    ErrorType.VALIDATION_ERROR,
                    String.format("Maturity date is unreasonably far in the future (%d years)", yearsBetween), "timeliness.unreasonable.maturity"));
            }
            
            return Result.success();
        };
    }

    /**
     * Validates that all date fields are logically consistent with each other.
     * Ensures proper chronological order of dates where applicable.
     * 
     * @return Specification that validates date consistency
     */
    public static Specification<ExposureRecord> hasConsistentDates() {
        return exposure -> {
            LocalDate reportingDate = exposure.reportingDate();
            LocalDate valuationDate = exposure.valuationDate();
            LocalDate maturityDate = exposure.maturityDate();
            
            // Valuation date should not be significantly after reporting date
            if (reportingDate != null && valuationDate != null) {
                if (valuationDate.isAfter(reportingDate.plusDays(7))) {
                    return Result.failure(ErrorDetail.of("TIMELINESS_VALUATION_AFTER_REPORTING", 
                        ErrorType.VALIDATION_ERROR,
                        "Valuation date should not be more than 7 days after reporting date", "timeliness.valuation.after.reporting"));
                }
            }
            
            // For term exposures, maturity should be after reporting
            if (reportingDate != null && maturityDate != null && exposure.isTermExposure()) {
                if (maturityDate.isBefore(reportingDate)) {
                    return Result.failure(ErrorDetail.of("TIMELINESS_MATURITY_BEFORE_REPORTING", 
                        ErrorType.VALIDATION_ERROR,
                        "Maturity date cannot be before reporting date for term exposures", "timeliness.maturity.before.reporting"));
                }
            }
            
            return Result.success();
        };
    }
}

