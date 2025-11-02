package com.bcbs239.regtech.modules.ingestion.domain.model;

import java.util.Objects;

public record CreditRiskMitigation(
    String exposureId,
    String collateralType,
    double collateralValue,
    String collateralCurrency
) {
    public CreditRiskMitigation {
        Objects.requireNonNull(exposureId);
    }
}
