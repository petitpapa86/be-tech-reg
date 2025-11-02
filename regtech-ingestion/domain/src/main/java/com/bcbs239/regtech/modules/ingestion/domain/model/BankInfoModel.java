package com.bcbs239.regtech.modules.ingestion.domain.model;

import java.util.Objects;

public record BankInfoModel(
    String bankName,
    String abiCode,
    String leiCode,
    String reportDate,
    int totalLoans
) {
    public BankInfoModel {
        Objects.requireNonNull(bankName);
    }
}
