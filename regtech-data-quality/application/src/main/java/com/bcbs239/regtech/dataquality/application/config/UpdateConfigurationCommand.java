package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.dataquality.domain.config.BankId;

/**
 * Command to update data quality configuration for a specific bank.
 * 
 * <p>CQRS Pattern: Write operation command.
 */
public record UpdateConfigurationCommand(
    BankId bankId,
    ConfigurationDto configuration
) {
}
