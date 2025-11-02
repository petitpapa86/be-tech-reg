package com.bcbs239.regtech.modules.ingestion.domain.model;

import java.util.Objects;

public class LoanExposure {
    private final String loanId;
    private final String exposureId;
    private final String borrowerName;
    private final String borrowerId;
    private final String counterpartyLei;
    private final double loanAmount;
    private final double grossExposureAmount;
    private final double netExposureAmount;
    private final String currency;
    private final String loanType;
    private final String sector;
    private final String exposureType;
    private final String borrowerCountry;
    private final String countryCode;

    public LoanExposure(String loanId, String exposureId, String borrowerName, String borrowerId,
                        String counterpartyLei, double loanAmount, double grossExposureAmount,
                        double netExposureAmount, String currency, String loanType, String sector,
                        String exposureType, String borrowerCountry, String countryCode) {
        this.loanId = Objects.requireNonNull(loanId);
        this.exposureId = exposureId;
        this.borrowerName = borrowerName;
        this.borrowerId = borrowerId;
        this.counterpartyLei = counterpartyLei;
        this.loanAmount = loanAmount;
        this.grossExposureAmount = grossExposureAmount;
        this.netExposureAmount = netExposureAmount;
        this.currency = currency;
        this.loanType = loanType;
        this.sector = sector;
        this.exposureType = exposureType;
        this.borrowerCountry = borrowerCountry;
        this.countryCode = countryCode;
    }

    // Getters
    public String getLoanId() { return loanId; }
    public String getExposureId() { return exposureId; }
    public String getBorrowerName() { return borrowerName; }
    public String getBorrowerId() { return borrowerId; }
    public String getCounterpartyLei() { return counterpartyLei; }
    public double getLoanAmount() { return loanAmount; }
    public double getGrossExposureAmount() { return grossExposureAmount; }
    public double getNetExposureAmount() { return netExposureAmount; }
    public String getCurrency() { return currency; }
    public String getLoanType() { return loanType; }
    public String getSector() { return sector; }
    public String getExposureType() { return exposureType; }
    public String getBorrowerCountry() { return borrowerCountry; }
    public String getCountryCode() { return countryCode; }

    @Override
    public String toString() {
        return "LoanExposure{" + "loanId='" + loanId + '\'' + ", exposureId='" + exposureId + '\'' + '}';
    }
}

