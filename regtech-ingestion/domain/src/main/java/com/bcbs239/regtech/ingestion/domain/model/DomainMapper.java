package com.bcbs239.regtech.ingestion.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DomainMapper {

    public static BankInfoModel toBankInfoModel(BankInfoDto dto) {
        return new BankInfoModel(dto.bankName(), dto.abiCode(), dto.leiCode(), dto.reportDate(), dto.totalLoans());
    }

    public static LoanExposure toLoanExposure(LoanExposureDto dto) {
        return new LoanExposure(
            dto.loanId(), dto.exposureId(), dto.borrowerName(), dto.borrowerId(), dto.counterpartyLei(),
            dto.loanAmount(), dto.grossExposureAmount(), dto.netExposureAmount(), dto.currency(), dto.loanType(),
            dto.sector(), dto.exposureType(), dto.borrowerCountry(), dto.countryCode()
        );
    }

    public static List<LoanExposure> toLoanExposureList(LoanExposureDto[] dtos) {
        return Arrays.stream(dtos).map(DomainMapper::toLoanExposure).collect(Collectors.toList());
    }

    public static CreditRiskMitigation toCrm(CreditRiskMitigationDto dto) {
        return new CreditRiskMitigation(dto.exposureId(), dto.collateralType(), dto.collateralValue(), dto.collateralCurrency());
    }

    public static List<CreditRiskMitigation> toCrmList(CreditRiskMitigationDto[] dtos) {
        return Arrays.stream(dtos).map(DomainMapper::toCrm).collect(Collectors.toList());
    }
}

