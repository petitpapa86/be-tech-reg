package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Report template type for BCBS 239 compliance
 */
@Getter
@RequiredArgsConstructor
public enum ReportTemplate {
    BANCA_ITALIA_STANDARD("Banca d'Italia - Standard"),
    ECB_EUROPEAN_STANDARD("ECB - European Standard"),
    CUSTOM("Custom Template");
    
    private final String displayName;
    
    public static Result<ReportTemplate> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_TEMPLATE",
                ErrorType.VALIDATION_ERROR,
                "Report template cannot be null or blank",
                "report.template.required"
            ));
        }
        
        try {
            return Result.success(ReportTemplate.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_TEMPLATE",
                ErrorType.VALIDATION_ERROR,
                "Invalid report template: " + value + ". Valid values: " +
                String.join(", ", java.util.Arrays.stream(values())
                    .map(Enum::name)
                    .toList()),
                "report.template.invalid"
            ));
        }
    }
}
