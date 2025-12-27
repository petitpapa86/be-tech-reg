package com.bcbs239.regtech.dataquality.domain.model.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a calculated large exposure for presentation/reporting.
 *
 * <p>This is intentionally small and immutable: it provides only the data and
 * behaviors needed by {@code QualityReport.toFrontendPresentation(...)}.</p>
 */
public record LargeExposure(
    String exposureId,
    String counterparty,
    BigDecimal amount,
    double percentOfCapital,
    BigDecimal tierOneCapital
) {

    private static final double LIMIT_PERCENT = 25.0;

    public LargeExposure {
        Objects.requireNonNull(counterparty, "counterparty cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(tierOneCapital, "tierOneCapital cannot be null");
        if (counterparty.isBlank()) {
            throw new IllegalArgumentException("counterparty cannot be blank");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (tierOneCapital.signum() <= 0) {
            throw new IllegalArgumentException("tierOneCapital must be positive");
        }
        if (Double.isNaN(percentOfCapital) || Double.isInfinite(percentOfCapital) || percentOfCapital < 0.0) {
            throw new IllegalArgumentException("percentOfCapital must be a non-negative finite number");
        }
    }

    public boolean exceedsLimit() {
        return percentOfCapital > LIMIT_PERCENT;
    }

    /**
     * Estimates the amount that would need to be reduced to get back within limit.
     *
     * <p>Uses a proportional approximation: excess fraction = (p - limit) / p.</p>
     */
    public BigDecimal calculateExcess() {
        if (!exceedsLimit()) {
            return BigDecimal.ZERO;
        }

        BigDecimal limitAmount = tierOneCapital.multiply(BigDecimal.valueOf(LIMIT_PERCENT / 100.0));
        BigDecimal excess = amount.subtract(limitAmount);
        return excess.signum() > 0 ? excess : BigDecimal.ZERO;
    }
}
