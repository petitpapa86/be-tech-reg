package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleDay {
    LUNEDI("Lunedì", 1),
    MARTEDI("Martedì", 2),
    MERCOLEDI("Mercoledì", 3),
    GIOVEDI("Giovedì", 4),
    VENERDI("Venerdì", 5);
    
    private final String displayName;
    private final int dayOfWeek;
    
    public static Result<ScheduleDay> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_SCHEDULE_DAY",
                ErrorType.VALIDATION_ERROR,
                "Schedule day cannot be null or blank",
                "report.scheduleDay.required"
            ));
        }
        
        String trimmed = value.trim().toUpperCase();
        
        for (ScheduleDay day : values()) {
            if (day.name().equals(trimmed) || day.displayName.toUpperCase().equals(trimmed)) {
                return Result.success(day);
            }
        }
        
        // Backward compatibility
        return switch (trimmed) {
            case "MONDAY" -> Result.success(LUNEDI);
            case "TUESDAY" -> Result.success(MARTEDI);
            case "WEDNESDAY" -> Result.success(MERCOLEDI);
            case "THURSDAY" -> Result.success(GIOVEDI);
            case "FRIDAY" -> Result.success(VENERDI);
            default -> Result.failure(ErrorDetail.of(
                    "INVALID_SCHEDULE_DAY",
                    ErrorType.VALIDATION_ERROR,
                    "Invalid schedule day: " + value + ". Valid values: LUNEDI, MARTEDI, MERCOLEDI, GIOVEDI, VENERDI",
                    "report.scheduleDay.invalid"
            ));
        };

    }
}
