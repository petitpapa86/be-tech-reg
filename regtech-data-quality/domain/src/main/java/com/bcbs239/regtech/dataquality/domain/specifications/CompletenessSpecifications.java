package com.bcbs239.regtech.dataquality.domain.specifications;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for Completeness quality dimension validation.
 * 
 * Completeness ensures that all required fields are present and non-empty
 * according to BCBS 239 regulatory requirements and business rules.
 */
public class CompletenessSpecifications {

    /**
     * Validates that all required fields are present and non-empty.
     * Required fields: exposureId, amount, currency, country, sector
     * 
     * @return Specification that validates required field presence
     */
    public static Specification<ExposureRecord> hasRequiredFields() {
        return exposure -> {
            List<ErrorDetail> errors = new ArrayList<>();
            
            if (isBlank(exposure.exposureId())) {
                errors.add(ErrorDetail.of("COMPLETENESS_EXPOSURE_ID_MISSING", 
                    "Exposure ID is required", "exposure_id"));
            }
            
            if (exposure.amount() == null) {
                errors.add(ErrorDetail.of("COMPLETENESS_AMOUNT_MISSING", 
                    "Amount is required", "amount"));
            }
            
            if (isBlank(exposure.currency())) {
                errors.add(ErrorDetail.of("COMPLETENESS_CURRENCY_MISSING", 
                    "Currency is required", "currency"));
            }
            
            if (isBlank(exposure.country())) {
                errors.add(ErrorDetail.of("COMPLETENESS_COUNTRY_MISSING", 
                    "Country is required", "country"));
            }
            
            if (isBlank(exposure.sector())) {
                errors.add(ErrorDetail.of("COMPLETENESS_SECTOR_MISSING", 
                    "Sector is required", "sector"));
            }
            
            return errors.isEmpty() ? Result.success() : Result.failure(errors);
        };
    }

    /**
     * Validates that corporate exposures have LEI codes.
     * Corporate exposures are identified by sector starting with "CORPORATE" or being "BANKING".
     * 
     * @return Specification that validates LEI presence for corporate exposures
     */
    public static Specification<ExposureRecord> hasLeiForCorporates() {
        return exposure -> {
            if (exposure.isCorporateExposure() && isBlank(exposure.leiCode())) {
                return Result.failure(ErrorDetail.of("COMPLETENESS_LEI_MISSING", 
                    "LEI code required for corporate exposures", "lei_code"));
            }
            return Result.success();
        };
    }

    /**
     * Validates that term exposures have maturity dates.
     * Term exposures are all exposures except equity products.
     * 
     * @return Specification that validates maturity date presence for term exposures
     */
    public static Specification<ExposureRecord> hasMaturityForTermExposures() {
        return exposure -> {
            if (exposure.isTermExposure() && exposure.maturityDate() == null) {
                return Result.failure(ErrorDetail.of("COMPLETENESS_MATURITY_MISSING", 
                    "Maturity date required for term exposures", "maturity_date"));
            }
            return Result.success();
        };
    }

    /**
     * Validates that exposures requiring risk assessment have internal ratings.
     * All exposures should have internal ratings for proper risk assessment.
     * 
     * @return Specification that validates internal rating presence
     */
    public static Specification<ExposureRecord> hasInternalRating() {
        return exposure -> {
            if (isBlank(exposure.internalRating())) {
                return Result.failure(ErrorDetail.of("COMPLETENESS_INTERNAL_RATING_MISSING", 
                    "Internal rating is required for risk assessment", "internal_rating"));
            }
            return Result.success();
        };
    }

    /**
     * Utility method to check if a string is blank (null, empty, or whitespace only)
     * 
     * @param value The string to check
     * @return true if the string is blank, false otherwise
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

