package com.bcbs239.regtech.dataquality.domain.rules;

public enum RuleType {
    VALIDATION,
    CALCULATION,
    TRANSFORMATION,
    DATA_QUALITY,
    THRESHOLD,
    BUSINESS_LOGIC,
    
    // Data Quality Dimensions (BCBS 239)
    COMPLETENESS,
    ACCURACY,
    CONSISTENCY,
    TIMELINESS,
    UNIQUENESS,
    VALIDITY
}
