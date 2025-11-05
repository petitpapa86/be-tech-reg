package com.bcbs239.regtech.billing.domain.subscriptions;

import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Subscription tier enumeration with pricing and limits
 */
@Getter
public enum SubscriptionTier {
    STARTER(Money.of(new BigDecimal("500.00"), Currency.getInstance("EUR")), 10000);

    /**
     * -- GETTER --
     *  Get the monthly price for this tier
     */
    private final Money monthlyPrice;
    /**
     * -- GETTER --
     *  Get the exposure limit for this tier
     */
    private final int exposureLimit;
    
    SubscriptionTier(Money monthlyPrice, int exposureLimit) {
        this.monthlyPrice = monthlyPrice;
        this.exposureLimit = exposureLimit;
    }

    /**
     * Check if usage exceeds the tier limit
     */
    public boolean isUsageOverLimit(int actualUsage) {
        return actualUsage > exposureLimit;
    }
    
    /**
     * Calculate overage amount based on usage
     */
    public int calculateOverage(int actualUsage) {
        return Math.max(0, actualUsage - exposureLimit);
    }
    
    /**
     * Get overage rate per exposure (€0.05 per exposure over limit)
     */
    public Money getOverageRate() {
        return Money.of(new BigDecimal("0.05"), Currency.getInstance("EUR"));
    }
    
    /**
     * Calculate total overage charges
     */
    public Money calculateOverageCharges(int actualUsage) {
        int overageCount = calculateOverage(actualUsage);
        if (overageCount <= 0) {
            return Money.zero(Currency.getInstance("EUR"));
        }
        return getOverageRate().multiply(overageCount);
    }
}

