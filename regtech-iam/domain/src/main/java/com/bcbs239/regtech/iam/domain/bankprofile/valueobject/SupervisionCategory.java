package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Banking supervision category
 */
@Getter
@RequiredArgsConstructor
public enum SupervisionCategory {
    SIGNIFICANT_SSM("Banche significative (SSM)"),
    LESS_SIGNIFICANT("Banche meno significative"),
    SYSTEMICALLY_IMPORTANT("Banche di rilevanza sistemica nazionale"),
    OTHER("Altre banche");
    
    private final String displayName;
    
    public static Result<SupervisionCategory> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "SUPERVISION_CATEGORY_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Supervision category cannot be null or blank", 
                "validation.supervision_category_required"
            ));
        }
        
        try {
            return Result.success(SupervisionCategory.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "SUPERVISION_CATEGORY_INVALID", 
                ErrorType.VALIDATION_ERROR, 
                "Invalid supervision category: " + value, 
                "validation.supervision_category_invalid"
            ));
        }
    }
}
