package com.bcbs239.regtech.billing.domain.billing;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Billing period value object with pro-ration calculation methods
 */
public record BillingPeriod(LocalDate startDate, LocalDate endDate) {
    
    public BillingPeriod {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
    }
    
    /**
     * Create billing period with validation
     */
    public static Result<BillingPeriod> create(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_PERIOD", "Start date cannot be null"));
        }
        if (endDate == null) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_PERIOD", "End date cannot be null"));
        }
        if (startDate.isAfter(endDate)) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_PERIOD", "Start date cannot be after end date"));
        }
        return Result.success(new BillingPeriod(startDate, endDate));
    }
    
    /**
     * Create billing period for a specific month
     */
    public static BillingPeriod forMonth(YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return new BillingPeriod(start, end);
    }
    
    /**
     * Create billing period for current month
     */
    public static BillingPeriod forCurrentMonth() {
        return forMonth(YearMonth.now());
    }
    
    /**
     * Create billing period for next month
     */
    public static BillingPeriod forNextMonth() {
        return forMonth(YearMonth.now().plusMonths(1));
    }
    
    /**
     * Get total number of days in the billing period
     */
    public int getDaysInPeriod() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * Calculate pro-rated amount based on partial usage of the billing period
     */
    public Result<Money> calculateProRatedAmount(Money monthlyAmount, LocalDate serviceStartDate) {
        if (monthlyAmount == null) {
            return Result.failure(new ErrorDetail("INVALID_PRORATION", "Monthly amount cannot be null"));
        }
        if (serviceStartDate == null) {
            return Result.failure(new ErrorDetail("INVALID_PRORATION", "Service start date cannot be null"));
        }
        
        // If service starts before or on the billing period start, charge full amount
        if (serviceStartDate.isBefore(startDate) || serviceStartDate.isEqual(startDate)) {
            return Result.success(monthlyAmount);
        }
        
        // If service starts after billing period ends, no charge
        if (serviceStartDate.isAfter(endDate)) {
            return Result.success(Money.zero(monthlyAmount.currency()));
        }
        
        int totalDays = getDaysInPeriod();
        int remainingDays = (int) ChronoUnit.DAYS.between(serviceStartDate, endDate) + 1;
        
        BigDecimal proRationFactor = BigDecimal.valueOf(remainingDays)
            .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
            
        return Result.success(monthlyAmount.multiply(proRationFactor));
    }
    
    /**
     * Check if a date falls within this billing period
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * Get the year-month representation of this billing period
     */
    public YearMonth getYearMonth() {
        return YearMonth.from(startDate);
    }
    
    /**
     * Get billing period identifier (e.g., "2024-01")
     */
    public String getPeriodId() {
        return startDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    /**
     * Check if this is the current billing period
     */
    public boolean isCurrentPeriod() {
        LocalDate today = LocalDate.now();
        return contains(today);
    }
    
    /**
     * Check if this billing period has ended
     */
    public boolean hasEnded() {
        return LocalDate.now().isAfter(endDate);
    }
    
    @Override
    public String toString() {
        return startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
    }
}