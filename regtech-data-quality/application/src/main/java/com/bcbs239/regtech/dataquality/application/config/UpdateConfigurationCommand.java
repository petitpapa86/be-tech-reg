package com.bcbs239.regtech.dataquality.application.config;

/**
 * Command to update data quality configuration for a specific bank.
 * 
 * <p>CQRS Pattern: Write operation command.
 */
public record UpdateConfigurationCommand(
    String bankId,
    ConfigurationDto configuration
) {
}
