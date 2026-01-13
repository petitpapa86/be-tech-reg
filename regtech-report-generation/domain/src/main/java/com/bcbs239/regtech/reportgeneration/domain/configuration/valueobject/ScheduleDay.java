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
    VENERDI("Venerdì", 5),
    SABATO("Sabato", 6),
    DOMENICA("Domenica", 7);
    
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
        if (trimmed.equals("MONDAY")) return Result.success(LUNEDI);
        if (trimmed.equals("TUESDAY")) return Result.success(MARTEDI);
        if (trimmed.equals("WEDNESDAY")) return Result.success(MERCOLEDI);
        if (trimmed.equals("THURSDAY")) return Result.success(GIOVEDI);
        if (trimmed.equals("FRIDAY")) return Result.success(VENERDI);
        if (trimmed.equals("SATURDAY")) return Result.success(SABATO);
        if (trimmed.equals("SUNDAY")) return Result.success(DOMENICA);
        
        return Result.failure(ErrorDetail.of(
            "INVALID_SCHEDULE_DAY",
            ErrorType.VALIDATION_ERROR,
            "Invalid schedule day: " + value + ". Valid values: LUNEDI, MARTEDI, MERCOLEDI, GIOVEDI, VENERDI, SABATO, DOMENICA",
            "report.scheduleDay.invalid"
        ));
    }
}
