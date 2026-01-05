package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Banking group structure type
 */
@Getter
@RequiredArgsConstructor
public enum GroupType {
    INDEPENDENT("Istituto Indipendente"),
    NATIONAL_GROUP("Gruppo Nazionale"),
    INTERNATIONAL_GROUP("Gruppo Internazionale");
    
    private final String displayName;
    
    public static Result<GroupType> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "GROUP_TYPE_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Group type cannot be null or blank", 
                "validation.group_type_required"
            ));
        }
        
        try {
            return Result.success(GroupType.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "GROUP_TYPE_INVALID", 
                ErrorType.VALIDATION_ERROR, 
                "Invalid group type: " + value, 
                "validation.group_type_invalid"
            ));
        }
    }
}
