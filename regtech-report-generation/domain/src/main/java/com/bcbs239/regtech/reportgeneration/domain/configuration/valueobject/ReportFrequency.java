package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportFrequency {
    MONTHLY("Mensile", 1),
    QUARTERLY("Trimestrale", 3),
    SEMI_ANNUAL("Semestrale", 6),
    ANNUAL("Annuale", 12);
    
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
        
        try {
            return Result.success(ReportFrequency.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_FREQUENCY",
                ErrorType.VALIDATION_ERROR,
                "Invalid report frequency: " + value + ". Valid values: " +
                String.join(", ", java.util.Arrays.stream(values())
                    .map(Enum::name)
                    .toList()),
                "report.frequency.invalid"
            ));
        }
    }
}
