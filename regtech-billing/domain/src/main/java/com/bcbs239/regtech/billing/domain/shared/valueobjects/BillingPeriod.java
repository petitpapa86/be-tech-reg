package com.bcbs239.regtech.billing.domain.shared.valueobjects;

import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * BillingPeriod value object representing a billing cycle period.
 * Typically represents a month (e.g., "2024-10") but can be extended for other periods.
 */
public record BillingPeriod(YearMonth yearMonth) {

    public BillingPeriod {
        Objects.requireNonNull(yearMonth, "YearMonth cannot be null");
    }

    /**
     * Factory method to create BillingPeriod from YearMonth
     */
    public static BillingPeriod of(YearMonth yearMonth) {
        return new BillingPeriod(yearMonth);
    }

    /**
     * Factory method to create BillingPeriod for a specific month
     */
    public static BillingPeriod forMonth(YearMonth yearMonth) {
        return new BillingPeriod(yearMonth);
    }

    /**
     * Factory method to create BillingPeriod for current month
     */
    public static BillingPeriod current() {
        return new BillingPeriod(YearMonth.now());
    }

    /**
     * Factory method to create BillingPeriod from string (format: "yyyy-MM")
     */
    public static Result<BillingPeriod> fromString(String period) {
        try {
            YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
            return Result.success(new BillingPeriod(ym));
        } catch (DateTimeParseException e) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_PERIOD",
                "Billing period must be in format yyyy-MM"));
        }
    }

    /**
     * Get the start date of this billing period
     */
    public LocalDate getStartDate() {
        return yearMonth.atDay(1);
    }

    /**
     * Get the end date of this billing period
     */
    public LocalDate getEndDate() {
        return yearMonth.atEndOfMonth();
    }

    /**
     * Get the period ID (same as toString format)
     */
    public String getPeriodId() {
        return toString();
    }

    /**
     * Get the next billing period
     */
    public BillingPeriod next() {
        return new BillingPeriod(yearMonth.plusMonths(1));
    }

    /**
     * Get the previous billing period
     */
    public BillingPeriod previous() {
        return new BillingPeriod(yearMonth.minusMonths(1));
    }

    /**
     * Check if a date falls within this billing period
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(getStartDate()) && !date.isAfter(getEndDate());
    }

    /**
     * Calculate pro-rated amount for a service that starts mid-period
     */
    public Result<Money> calculateProRatedAmount(Money monthlyAmount, LocalDate serviceStartDate) {
        if (monthlyAmount == null) {
            return Result.failure(new ErrorDetail("INVALID_AMOUNT", "Monthly amount cannot be null"));
        }
        if (serviceStartDate == null) {
            return Result.failure(new ErrorDetail("INVALID_DATE", "Service start date cannot be null"));
        }

        LocalDate periodStart = getStartDate();
        LocalDate periodEnd = getEndDate();

        // If service starts on or before period start, no prorating needed
        if (!serviceStartDate.isAfter(periodStart)) {
            return Result.success(monthlyAmount);
        }

        // If service starts after period end, no charge for this period
        if (serviceStartDate.isAfter(periodEnd)) {
            return Result.success(Money.zero(monthlyAmount.currency()));
        }

        // Calculate days in period and days of service
        long totalDaysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        long serviceDays = java.time.temporal.ChronoUnit.DAYS.between(serviceStartDate, periodEnd) + 1;

        // Calculate pro-rated amount
        BigDecimal ratio = BigDecimal.valueOf(serviceDays).divide(BigDecimal.valueOf(totalDaysInPeriod), 4, RoundingMode.HALF_UP);
        Money proratedAmount = monthlyAmount.multiply(ratio);

        return Result.success(proratedAmount);
    }

    @Override
    public String toString() {
        return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}

