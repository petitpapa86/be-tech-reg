package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleDay {
    MONDAY("Lunedì", 1),
    TUESDAY("Martedì", 2),
    WEDNESDAY("Mercoledì", 3),
    THURSDAY("Giovedì", 4),
    FRIDAY("Venerdì", 5);
    
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
        
        try {
            return Result.success(ScheduleDay.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_SCHEDULE_DAY",
                ErrorType.VALIDATION_ERROR,
                "Invalid schedule day: " + value + ". Valid values: " +
                String.join(", ", java.util.Arrays.stream(values())
                    .map(Enum::name)
                    .toList()),
                "report.scheduleDay.invalid"
            ));
        }
    }
}
