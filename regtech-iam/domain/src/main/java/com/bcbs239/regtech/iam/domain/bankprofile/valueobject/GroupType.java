package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

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
            return Result.failure("Group type cannot be null or blank");
        }
        
        try {
            return Result.success(GroupType.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid group type: " + value);
        }
    }
}
