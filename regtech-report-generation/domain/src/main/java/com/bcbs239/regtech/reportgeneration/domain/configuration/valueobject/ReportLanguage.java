package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportLanguage {
    ITALIAN("Italiano", "it"),
    ENGLISH("English", "en"),
    BILINGUAL("Bilingue (IT/EN)", "it-en");
    
    private final String displayName;
    private final String code;
    
    public static Result<ReportLanguage> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_LANGUAGE",
                ErrorType.VALIDATION_ERROR,
                "Report language cannot be null or blank",
                "report.language.required"
            ));
        }
        
        try {
            return Result.success(ReportLanguage.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_LANGUAGE",
                ErrorType.VALIDATION_ERROR,
                "Invalid report language: " + value + ". Valid values: " +
                String.join(", ", java.util.Arrays.stream(values())
                    .map(Enum::name)
                    .toList()),
                "report.language.invalid"
            ));
        }
    }
}
