package com.bcbs239.regtech.modules.ingestion.domain.model;

import java.util.Objects;

public class BankInfoModel {
    private final String bankName;
    private final String abiCode;
    private final String leiCode;
    private final String reportDate;
    private final int totalLoans;

    public BankInfoModel(String bankName, String abiCode, String leiCode, String reportDate, int totalLoans) {
        this.bankName = Objects.requireNonNull(bankName);
        this.abiCode = abiCode;
        this.leiCode = leiCode;
        this.reportDate = reportDate;
        this.totalLoans = totalLoans;
    }

    public String getBankName() { return bankName; }
    public String getAbiCode() { return abiCode; }
    public String getLeiCode() { return leiCode; }
    public String getReportDate() { return reportDate; }
    public int getTotalLoans() { return totalLoans; }
}

