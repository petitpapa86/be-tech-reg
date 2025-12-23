package com.bcbs239.regtech.ingestion.domain.model;

import java.util.Arrays;
import java.util.List;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class DomainMapper {

    public static BankInfoModel toBankInfoModel(BankInfoDto dto) {
        return new BankInfoModel(dto.bankName(), dto.abiCode(), dto.leiCode(), dto.reportDate(), dto.totalExposures());
    }

    public static LoanExposure toLoanExposure(ExposureDto dto) {
        return new LoanExposure(
            dto.instrumentId(),        // loanId
            dto.exposureId(),          // exposureId
            dto.counterpartyName(),    // borrowerName
            dto.counterpartyId(),      // borrowerId
            dto.counterpartyLei(),     // counterpartyLei
            dto.exposureAmount(),      // loanAmount
            dto.exposureAmount(),      // grossExposureAmount
            dto.exposureAmount(),      // netExposureAmount
            dto.currency(),            // currency
            dto.productType(),         // loanType
            dto.sector(),              // sector
            parseLocalDateSafe(dto.maturityDate()), // maturityDate
            dto.balanceSheetType(),    // exposureType
            null,                      // borrowerCountry (not in new format)
            dto.countryCode(),         // countryCode
            dto.internalRating()       // internalRating
        );
    }

    private static LocalDate parseLocalDateSafe(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static List<LoanExposure> toLoanExposureList(ExposureDto[] dtos) {
        return Arrays.stream(dtos).map(DomainMapper::toLoanExposure).collect(Collectors.toList());
    }

    public static CreditRiskMitigation toCrm(CreditRiskMitigationDto dto) {
        return new CreditRiskMitigation(dto.exposureId(), dto.collateralType(), dto.collateralValue(), dto.collateralCurrency());
    }

    public static List<CreditRiskMitigation> toCrmList(CreditRiskMitigationDto[] dtos) {
        return Arrays.stream(dtos).map(DomainMapper::toCrm).collect(Collectors.toList());
    }
}


