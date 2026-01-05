package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Time for scheduled report generation
 * Format: HH:mm (24-hour)
 */
@Value
public class ScheduleTime {
    LocalTime time;
    
    private ScheduleTime(LocalTime time) {
        this.time = time;
    }
    
    public static Result<ScheduleTime> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_SCHEDULE_TIME",
                ErrorType.VALIDATION_ERROR,
                "Schedule time cannot be null or blank",
                "report.scheduleTime.required"
            ));
        }
        
        try {
            LocalTime parsedTime = LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            
            // Business rule: Must be within working hours (08:00-18:00)
            if (parsedTime.isBefore(LocalTime.of(8, 0)) || parsedTime.isAfter(LocalTime.of(18, 0))) {
                return Result.failure(ErrorDetail.of(
                    "INVALID_SCHEDULE_TIME",
                    ErrorType.VALIDATION_ERROR,
                    "Schedule time must be within working hours (08:00-18:00)",
                    "report.scheduleTime.outsideWorkingHours"
                ));
            }
            
            return Result.success(new ScheduleTime(parsedTime));
        } catch (DateTimeParseException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_SCHEDULE_TIME",
                ErrorType.VALIDATION_ERROR,
                "Invalid time format: " + value + ". Expected format: HH:mm",
                "report.scheduleTime.invalidFormat"
            ));
        }
    }
    
    public String toFormattedString() {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    @Override
    public String toString() {
        return toFormattedString();
    }
}
