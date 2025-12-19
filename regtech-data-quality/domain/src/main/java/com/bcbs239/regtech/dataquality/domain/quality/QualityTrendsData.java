package com.bcbs239.regtech.dataquality.domain.quality;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;

import java.time.Instant;
import java.util.List;

/**
 * Value object containing quality trends data.
 * Immutable representation of calculated trends.
 */
public record QualityTrendsData(
    BankId bankId,
    Instant startTime,
    Instant endTime,
    int totalReports,
    double averageOverallScore,
    double averageCompletenessScore,
    double averageAccuracyScore,
    double averageConsistencyScore,
    double averageTimelinessScore,
    double averageUniquenessScore,
    double averageValidityScore,
    double complianceRate,
    String trendDirection
) {

    /**
     * Creates an empty trends data for when no reports are found.
     */
    public static QualityTrendsData empty(BankId bankId, Instant startTime, Instant endTime) {
        return new QualityTrendsData(
            bankId,
            startTime,
            endTime,
            0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0,
            "STABLE"
        );
    }
}