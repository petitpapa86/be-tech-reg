package com.bcbs239.regtech.modules.ingestion.domain.model;

import java.util.Objects;

public class CreditRiskMitigation {
    private final String exposureId;
    private final String collateralType;
    private final double collateralValue;
    private final String collateralCurrency;

    public CreditRiskMitigation(String exposureId, String collateralType, double collateralValue, String collateralCurrency) {
        this.exposureId = Objects.requireNonNull(exposureId);
        this.collateralType = collateralType;
        this.collateralValue = collateralValue;
        this.collateralCurrency = collateralCurrency;
    }

    public String getExposureId() { return exposureId; }
    public String getCollateralType() { return collateralType; }
    public double getCollateralValue() { return collateralValue; }
    public String getCollateralCurrency() { return collateralCurrency; }
}

