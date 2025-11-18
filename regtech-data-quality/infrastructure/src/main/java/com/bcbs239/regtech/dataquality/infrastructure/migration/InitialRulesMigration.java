package com.bcbs239.regtech.dataquality.infrastructure.migration;

import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Migrates hardcoded validation rules from Specifications to database-driven Rules Engine.
 * 
 * <p>This migration runs only once on startup if enabled via configuration.
 * It creates configurable rules for:</p>
 * <ul>
 *   <li>Accuracy thresholds (max amounts, valid currencies/countries)</li>
 *   <li>Timeliness constraints (max reporting age, future dates)</li>
 *   <li>Other data quality rules previously hardcoded in Specifications</li>
 * </ul>
 * 
 * <p>To enable: <code>regtech.dataquality.rules-migration.enabled=true</code></p>
 * <p>After first run, disable to prevent duplicate rules.</p>
 */
@Slf4j
@Component
@Order(100)  // Run after database initialization
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "regtech.dataquality.rules-migration",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false  // Disabled by default to prevent duplicate runs
)
public class InitialRulesMigration implements CommandLineRunner {
    
    private final BusinessRuleRepository ruleRepository;
    
    private static final String REGULATION_ID = "BCBS_239_DATA_QUALITY";
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2024, 1, 1);
    
    @Override
    public void run(String... args) {
        if (ruleRepository.count() > 0) {
            log.info("Rules already exist (count={}), skipping migration", ruleRepository.count());
            return;
        }
        
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Starting Initial Rules Migration");
        log.info("  Migrating hardcoded rules to Rules Engine...");
        log.info("═══════════════════════════════════════════════════════════");
        
        try {
            migrateAccuracyRules();
            migrateTimelinessRules();
            migrateConsistencyRules();
            
            long totalRules = ruleRepository.count();
            log.info("═══════════════════════════════════════════════════════════");
            log.info("  ✓ Rules Migration Completed Successfully");
            log.info("  Total rules created: {}", totalRules);
            log.info("═══════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("✗ Rules migration failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to migrate rules", e);
        }
    }
    
    /**
     * Migrates accuracy validation rules.
     */
    private void migrateAccuracyRules() {
        log.info("→ Migrating Accuracy rules...");
        
        // Rule 1: Maximum Reasonable Amount
        BusinessRule maxAmountRule = BusinessRule.builder()
            .ruleId("DQ_ACCURACY_MAX_AMOUNT")
            .regulationId(REGULATION_ID)
            .ruleName("Maximum Reasonable Amount")
            .ruleCode("ACCURACY_MAX_AMOUNT")
            .description("Validates that exposure amount is within reasonable bounds (10B EUR)")
            .ruleType(RuleType.THRESHOLD)
            .ruleCategory("ACCURACY")
            .severity(Severity.MEDIUM)
            .businessLogic("#amount < #max_reasonable_amount")
            .executionOrder(10)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameter maxAmountParam = RuleParameter.builder()
            .rule(maxAmountRule)
            .parameterName("max_reasonable_amount")
            .parameterValue("10000000000")
            .parameterType(ParameterType.NUMERIC)
            .dataType("DECIMAL")
            .unit("EUR")
            .description("Maximum reasonable exposure amount (10 billion EUR)")
            .isConfigurable(true)
            .build();
        
        maxAmountRule.addParameter(maxAmountParam);
        ruleRepository.save(maxAmountRule);
        log.info("  ✓ Created rule: {}", maxAmountRule.getRuleCode());
        
        // Rule 2: Valid Currency Codes
        BusinessRule validCurrenciesRule = BusinessRule.builder()
            .ruleId("DQ_ACCURACY_VALID_CURRENCIES")
            .regulationId(REGULATION_ID)
            .ruleName("Valid Currency Codes")
            .ruleCode("ACCURACY_VALID_CURRENCIES")
            .description("Validates currency against list of valid ISO 4217 codes")
            .ruleType(RuleType.VALIDATION)
            .ruleCategory("ACCURACY")
            .severity(Severity.HIGH)
            .businessLogic("#valid_currency_codes.contains(#currency)")
            .executionOrder(5)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameter currencyListParam = RuleParameter.builder()
            .rule(validCurrenciesRule)
            .parameterName("valid_currency_codes")
            .parameterValue("USD,EUR,GBP,JPY,CHF,CAD,AUD,SEK,NOK,DKK,PLN,CZK,HUF,RON,BGN,HRK")
            .parameterType(ParameterType.LIST)
            .dataType("STRING")
            .description("Comma-separated list of valid ISO 4217 currency codes")
            .isConfigurable(true)
            .build();
        
        validCurrenciesRule.addParameter(currencyListParam);
        ruleRepository.save(validCurrenciesRule);
        log.info("  ✓ Created rule: {}", validCurrenciesRule.getRuleCode());
        
        // Rule 3: Valid Country Codes
        BusinessRule validCountriesRule = BusinessRule.builder()
            .ruleId("DQ_ACCURACY_VALID_COUNTRIES")
            .regulationId(REGULATION_ID)
            .ruleName("Valid Country Codes")
            .ruleCode("ACCURACY_VALID_COUNTRIES")
            .description("Validates country against list of valid ISO 3166 codes")
            .ruleType(RuleType.VALIDATION)
            .ruleCategory("ACCURACY")
            .severity(Severity.HIGH)
            .businessLogic("#valid_country_codes.contains(#country)")
            .executionOrder(6)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameter countryListParam = RuleParameter.builder()
            .rule(validCountriesRule)
            .parameterName("valid_country_codes")
            .parameterValue("US,GB,FR,DE,IT,ES,NL,BE,AT,CH,SE,NO,DK,FI,PL,CZ,HU,RO,BG,HR,IE,PT,GR,SK,SI,EE,LV,LT,LU,MT,CY")
            .parameterType(ParameterType.LIST)
            .dataType("STRING")
            .description("Comma-separated list of valid ISO 3166 country codes")
            .isConfigurable(true)
            .build();
        
        validCountriesRule.addParameter(countryListParam);
        ruleRepository.save(validCountriesRule);
        log.info("  ✓ Created rule: {}", validCountriesRule.getRuleCode());
        
        log.info("  ✓ Accuracy rules migration completed (3 rules)");
    }
    
    /**
     * Migrates timeliness validation rules.
     */
    private void migrateTimelinessRules() {
        log.info("→ Migrating Timeliness rules...");
        
        // Rule 1: Maximum Reporting Age
        BusinessRule maxReportingAgeRule = BusinessRule.builder()
            .ruleId("DQ_TIMELINESS_MAX_REPORTING_AGE")
            .regulationId(REGULATION_ID)
            .ruleName("Maximum Reporting Age")
            .ruleCode("TIMELINESS_MAX_AGE")
            .description("Validates that reporting date is not older than 90 days")
            .ruleType(RuleType.THRESHOLD)
            .ruleCategory("TIMELINESS")
            .severity(Severity.HIGH)
            .businessLogic("T(java.time.temporal.ChronoUnit).DAYS.between(#reporting_date, T(java.time.LocalDate).now()) <= #max_reporting_age_days")
            .executionOrder(20)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameter maxAgeParam = RuleParameter.builder()
            .rule(maxReportingAgeRule)
            .parameterName("max_reporting_age_days")
            .parameterValue("90")
            .parameterType(ParameterType.NUMERIC)
            .dataType("INTEGER")
            .unit("days")
            .minValue(new BigDecimal("1"))
            .maxValue(new BigDecimal("365"))
            .description("Maximum age for reporting data (90 days)")
            .isConfigurable(true)
            .build();
        
        maxReportingAgeRule.addParameter(maxAgeParam);
        ruleRepository.save(maxReportingAgeRule);
        log.info("  ✓ Created rule: {}", maxReportingAgeRule.getRuleCode());
        
        // Rule 2: No Future Dates
        BusinessRule noFutureDatesRule = BusinessRule.builder()
            .ruleId("DQ_TIMELINESS_NO_FUTURE_DATES")
            .regulationId(REGULATION_ID)
            .ruleName("No Future Dates")
            .ruleCode("TIMELINESS_NO_FUTURE")
            .description("Validates that reporting date is not in the future")
            .ruleType(RuleType.VALIDATION)
            .ruleCategory("TIMELINESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#reporting_date <= T(java.time.LocalDate).now()")
            .executionOrder(15)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        ruleRepository.save(noFutureDatesRule);
        log.info("  ✓ Created rule: {}", noFutureDatesRule.getRuleCode());
        
        log.info("  ✓ Timeliness rules migration completed (2 rules)");
    }
    
    /**
     * Migrates consistency validation rules.
     */
    private void migrateConsistencyRules() {
        log.info("→ Migrating Consistency rules...");
        
        // Rule 1: Currency-Country Consistency (Example - can be expanded)
        BusinessRule currencyCountryRule = BusinessRule.builder()
            .ruleId("DQ_CONSISTENCY_CURRENCY_COUNTRY")
            .regulationId(REGULATION_ID)
            .ruleName("Currency-Country Consistency")
            .ruleCode("CONSISTENCY_CURRENCY_COUNTRY")
            .description("Validates that currency matches country (e.g., EUR for Eurozone countries)")
            .ruleType(RuleType.BUSINESS_LOGIC)
            .ruleCategory("CONSISTENCY")
            .severity(Severity.MEDIUM)
            .businessLogic("!(#currency == 'EUR' && #eurozone_countries.contains(#country)) || (#currency == 'EUR' && #eurozone_countries.contains(#country))")
            .executionOrder(30)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(false)  // Disabled by default - needs business logic refinement
            .build();
        
        RuleParameter eurozoneParam = RuleParameter.builder()
            .rule(currencyCountryRule)
            .parameterName("eurozone_countries")
            .parameterValue("DE,FR,IT,ES,NL,BE,AT,PT,IE,GR,FI,LU,SK,SI,EE,LV,LT,CY,MT,HR")
            .parameterType(ParameterType.LIST)
            .dataType("STRING")
            .description("List of Eurozone country codes")
            .isConfigurable(true)
            .build();
        
        currencyCountryRule.addParameter(eurozoneParam);
        ruleRepository.save(currencyCountryRule);
        log.info("  ✓ Created rule: {} (disabled, needs refinement)", currencyCountryRule.getRuleCode());
        
        log.info("  ✓ Consistency rules migration completed (1 rule)");
    }
}
