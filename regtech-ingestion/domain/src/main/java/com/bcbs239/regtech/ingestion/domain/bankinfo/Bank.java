package com.bcbs239.regtech.ingestion.domain.bankinfo;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;

/**
 * Bank entity representing a financial institution.
 */
@Getter
public class Bank extends Entity {

    private final String name;
    private final String country;

    private Bank(BankId id, String name, String country) {
        this.name = name;
        this.country = country;
    }

    public static Result<Bank> create(BankId id, String name, String country) {
        if (id == null) {
            return Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                "BANK_ID_NULL", com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                "Bank ID cannot be null", "bank.id_null"));
        }
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                "BANK_NAME_NULL", com.bcbs239.regtech.core.domain.shared.ErrorType.VALIDATION_ERROR,
                "Bank name cannot be null or empty", "bank.name_null"));
        }
        if (country == null || country.trim().isEmpty()) {
            return Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                "BANK_COUNTRY_NULL", com.bcbs239.regtech.core.domain.shared.ErrorType.VALIDATION_ERROR,
                "Bank country cannot be null or empty", "bank.country_null"));
        }

        return Result.success(new Bank(id, name.trim(), country.trim()));
    }

}