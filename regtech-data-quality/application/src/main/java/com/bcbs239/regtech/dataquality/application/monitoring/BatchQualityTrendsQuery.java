package com.bcbs239.regtech.dataquality.application.monitoring;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;

import java.time.Instant;

/**
 * Query to retrieve quality trends for a bank over a time period.
 */
public record BatchQualityTrendsQuery(
    BankId bankId,
    Instant startTime,
    Instant endTime,
    int limit
) {
    
    /**
     * Creates a trends query for the specified bank and time period.
     */
    public static BatchQualityTrendsQuery forBankAndPeriod(
        BankId bankId,
        Instant startTime,
        Instant endTime
    ) {
        return new BatchQualityTrendsQuery(bankId, startTime, endTime, 100);
    }
    
    /**
     * Creates a trends query with a custom limit.
     */
    public static BatchQualityTrendsQuery forBankAndPeriodWithLimit(
        BankId bankId,
        Instant startTime,
        Instant endTime,
        int limit
    ) {
        return new BatchQualityTrendsQuery(bankId, startTime, endTime, limit);
    }
    
    /**
     * Creates a trends query for the last N days.
     */
    public static BatchQualityTrendsQuery forBankLastDays(BankId bankId, int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24 * 60 * 60L);
        return new BatchQualityTrendsQuery(bankId, startTime, endTime, 100);
    }
    
    /**
     * Validates the query parameters.
     */
    public void validate() {
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        if (limit > 1000) {
            throw new IllegalArgumentException("Limit cannot exceed 1000");
        }
    }
}

