package com.bcbs239.regtech.dataquality.domain.quality;

/**
 * Domain model for Quality Thresholds.
 * 
 * completenessMinPercent: minimum percentage for completeness
 * accuracyMaxErrorPercent: maximum error percentage for accuracy
 * timelinessDays: maximum days for timeliness
 * consistencyPercent: minimum percentage for consistency
 */
public record QualityThreshold(
    String bankId,
    double completenessMinPercent,
    double accuracyMaxErrorPercent,
    int timelinessDays,
    double consistencyPercent
) {
}
