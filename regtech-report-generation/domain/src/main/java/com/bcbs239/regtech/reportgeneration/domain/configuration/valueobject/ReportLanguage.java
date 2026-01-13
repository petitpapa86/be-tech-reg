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
        
        String trimmed = value.trim().toUpperCase();
        
        // Support code (IT, EN) or enum name (ITALIAN, ENGLISH)
        for (ReportLanguage language : values()) {
            if (language.name().equals(trimmed) || language.code.equalsIgnoreCase(trimmed)) {
                return Result.success(language);
            }
        }
        
        return Result.failure(ErrorDetail.of(
            "INVALID_LANGUAGE",
            ErrorType.VALIDATION_ERROR,
            "Invalid report language: " + value + ". Valid values: IT, EN, BILINGUAL",
            "report.language.invalid"
        ));
    }
}
