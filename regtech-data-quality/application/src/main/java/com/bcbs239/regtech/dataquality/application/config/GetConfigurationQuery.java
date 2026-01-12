package com.bcbs239.regtech.dataquality.application.config;

/**
 * Query to retrieve data quality configuration for a specific bank.
 * 
 * <p>CQRS Pattern: Read operation query.
 */
public record GetConfigurationQuery(String bankId) {
}
