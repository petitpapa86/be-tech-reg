package com.bcbs239.regtech.billing.application.shared;

import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;

/**
 * Data transfer object for usage metrics from the ingestion context.
 * Contains usage statistics for a specific user and billing period.
 */
public record UsageMetrics(
    UserId userId,
    BillingPeriod billingPeriod,
    int totalExposures,
    int documentsProcessed,
    long dataVolumeBytes
) {
    
    /**
     * Factory method to create UsageMetrics
     */
    public static UsageMetrics of(
            UserId userId,
            BillingPeriod billingPeriod,
            int totalExposures,
            int documentsProcessed,
            long dataVolumeBytes) {
        return new UsageMetrics(
            userId,
            billingPeriod,
            totalExposures,
            documentsProcessed,
            dataVolumeBytes
        );
    }
    
    /**
     * Create empty usage metrics (no usage)
     */
    public static UsageMetrics empty(UserId userId, BillingPeriod billingPeriod) {
        return new UsageMetrics(userId, billingPeriod, 0, 0, 0L);
    }
    
    /**
     * Check if there is any usage in this period
     */
    public boolean hasUsage() {
        return totalExposures > 0 || documentsProcessed > 0 || dataVolumeBytes > 0;
    }
}

