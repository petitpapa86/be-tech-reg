package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.dataquality.domain.config.BankId;

/**
 * Query to retrieve data quality configuration for a specific bank.
 * 
 * <p>CQRS Pattern: Read operation query.
 */
public record GetConfigurationQuery(BankId bankId) {
}
