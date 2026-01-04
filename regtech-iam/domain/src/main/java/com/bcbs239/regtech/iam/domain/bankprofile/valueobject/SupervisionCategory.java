package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

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
            return Result.failure("Supervision category cannot be null or blank");
        }
        
        try {
            return Result.success(SupervisionCategory.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid supervision category: " + value);
        }
    }
}
