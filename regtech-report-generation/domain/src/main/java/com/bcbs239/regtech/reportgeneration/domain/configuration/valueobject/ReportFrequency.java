package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportFrequency {
    MENSILE("Mensile", 1),
    TRIMESTRALE("Trimestrale", 3),
    SEMESTRALE("Semestrale", 6),
    ANNUALE("Annuale", 12);
    
    private final String displayName;
    private final int monthsInterval;
    
    public static Result<ReportFrequency> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_FREQUENCY",
                ErrorType.VALIDATION_ERROR,
                "Report frequency cannot be null or blank",
                "report.frequency.required"
            ));
        }
        
        String trimmed = value.trim().toUpperCase();
        
        for (ReportFrequency frequency : values()) {
            if (frequency.name().equals(trimmed) || frequency.displayName.toUpperCase().equals(trimmed)) {
                return Result.success(frequency);
            }
        }
        
        // Backward compatibility for MONTHLY, etc if needed? 
        // Migration will handle DB, but if API receives old values:
        if (trimmed.equals("MONTHLY")) return Result.success(MENSILE);
        if (trimmed.equals("QUARTERLY")) return Result.success(TRIMESTRALE);
        if (trimmed.equals("SEMI_ANNUAL")) return Result.success(SEMESTRALE);
        if (trimmed.equals("ANNUAL")) return Result.success(ANNUALE);
        
        return Result.failure(ErrorDetail.of(
            "INVALID_FREQUENCY",
            ErrorType.VALIDATION_ERROR,
            "Invalid report frequency: " + value + ". Valid values: MENSILE, TRIMESTRALE, SEMESTRALE, ANNUALE",
            "report.frequency.invalid"
        ));
    }
}
