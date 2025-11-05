package com.bcbs239.regtech.ingestion.domain.model;

import java.util.Objects;

public record LoanExposure(
    String loanId,
    String exposureId,
    String borrowerName,
    String borrowerId,
    String counterpartyLei,
    double loanAmount,
    double grossExposureAmount,
    double netExposureAmount,
    String currency,
    String loanType,
    String sector,
    String exposureType,
    String borrowerCountry,
    String countryCode
) {
    public LoanExposure {
        Objects.requireNonNull(loanId);
    }

    // toString kept for readability
    @Override
    public String toString() {
        return "LoanExposure{" + "loanId='" + loanId + '\'' + ", exposureId='" + exposureId + '\'' + '}';
    }
}

