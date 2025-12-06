package com.bcbs239.regtech.dataquality.infrastructure.migration;

import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.BusinessRuleEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleParameterEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
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
            migrateCompletenessRules();
            migrateAccuracyRules();
            migrateConsistencyRules();
            migrateTimelinessRules();
            migrateUniquenessRules();
            migrateValidityRules();
            
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
     * Migrates completeness validation rules.
     */
    private void migrateCompletenessRules() {
        log.info("→ Migrating Completeness rules...");
        
        // Rule 1: Exposure ID Required
        BusinessRuleEntity exposureIdRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_EXPOSURE_ID")
            .regulationId(REGULATION_ID)
            .ruleName("Exposure ID Required")
            .ruleCode("COMPLETENESS_EXPOSURE_ID_REQUIRED")
            .description("Validates that exposure ID is present and non-empty")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#exposureId != null && !#exposureId.trim().isEmpty()")
            .executionOrder(1)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(exposureIdRule);
        log.info("  ✓ Created rule: {}", exposureIdRule.getRuleCode());
        
        // Rule 2: Amount Required
        BusinessRuleEntity amountRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_AMOUNT")
            .regulationId(REGULATION_ID)
            .ruleName("Amount Required")
            .ruleCode("COMPLETENESS_AMOUNT_REQUIRED")
            .description("Validates that amount is present")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#amount != null")
            .executionOrder(2)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(amountRule);
        log.info("  ✓ Created rule: {}", amountRule.getRuleCode());
        
        // Rule 3: Currency Required
        BusinessRuleEntity currencyRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_CURRENCY")
            .regulationId(REGULATION_ID)
            .ruleName("Currency Required")
            .ruleCode("COMPLETENESS_CURRENCY_REQUIRED")
            .description("Validates that currency is present and non-empty")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#currency != null && !#currency.trim().isEmpty()")
            .executionOrder(3)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(currencyRule);
        log.info("  ✓ Created rule: {}", currencyRule.getRuleCode());
        
        // Rule 4: Country Required
        BusinessRuleEntity countryRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_COUNTRY")
            .regulationId(REGULATION_ID)
            .ruleName("Country Required")
            .ruleCode("COMPLETENESS_COUNTRY_REQUIRED")
            .description("Validates that country is present and non-empty")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#country != null && !#country.trim().isEmpty()")
            .executionOrder(4)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(countryRule);
        log.info("  ✓ Created rule: {}", countryRule.getRuleCode());
        
        // Rule 5: Sector Required
        BusinessRuleEntity sectorRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_SECTOR")
            .regulationId(REGULATION_ID)
            .ruleName("Sector Required")
            .ruleCode("COMPLETENESS_SECTOR_REQUIRED")
            .description("Validates that sector is present and non-empty")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#sector != null && !#sector.trim().isEmpty()")
            .executionOrder(5)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(sectorRule);
        log.info("  ✓ Created rule: {}", sectorRule.getRuleCode());
        
        // Rule 6: LEI for Corporates
        BusinessRuleEntity leiForCorporatesRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_LEI_CORPORATES")
            .regulationId(REGULATION_ID)
            .ruleName("LEI Required for Corporate Exposures")
            .ruleCode("COMPLETENESS_LEI_FOR_CORPORATES")
            .description("Validates that corporate exposures have LEI codes")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.HIGH)
            .businessLogic("#sector != 'CORPORATE' || (#leiCode != null && !#leiCode.trim().isEmpty())")
            .executionOrder(6)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(leiForCorporatesRule);
        log.info("  ✓ Created rule: {}", leiForCorporatesRule.getRuleCode());
        
        // Rule 7: Maturity for Term Exposures
        BusinessRuleEntity maturityForTermRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_MATURITY_TERM")
            .regulationId(REGULATION_ID)
            .ruleName("Maturity Date Required for Term Exposures")
            .ruleCode("COMPLETENESS_MATURITY_FOR_TERM")
            .description("Validates that term exposures (non-equity) have maturity dates")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.HIGH)
            .businessLogic("#productType == 'EQUITY' || #maturityDate != null")
            .executionOrder(7)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(maturityForTermRule);
        log.info("  ✓ Created rule: {}", maturityForTermRule.getRuleCode());
        
        // Rule 8: Internal Rating Required
        BusinessRuleEntity internalRatingRule = BusinessRuleEntity.builder()
            .ruleId("DQ_COMPLETENESS_INTERNAL_RATING")
            .regulationId(REGULATION_ID)
            .ruleName("Internal Rating Required")
            .ruleCode("COMPLETENESS_INTERNAL_RATING")
            .description("Validates that internal rating is present for risk assessment")
            .ruleType(RuleType.COMPLETENESS)
            .ruleCategory("COMPLETENESS")
            .severity(Severity.HIGH)
            .businessLogic("#internalRating != null && !#internalRating.trim().isEmpty()")
            .executionOrder(8)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(internalRatingRule);
        log.info("  ✓ Created rule: {}", internalRatingRule.getRuleCode());
        
        log.info("  ✓ Completeness rules migration completed (8 rules)");
    }
    
    /**
     * Migrates accuracy validation rules.
     */
    private void migrateAccuracyRules() {
        log.info("→ Migrating Accuracy rules...");
        
        // Rule 1: Positive Amount
        BusinessRuleEntity positiveAmountRule = BusinessRuleEntity.builder()
            .ruleId("DQ_ACCURACY_POSITIVE_AMOUNT")
            .regulationId(REGULATION_ID)
            .ruleName("Positive Amount")
            .ruleCode("ACCURACY_POSITIVE_AMOUNT")
            .description("Validates that exposure amount is positive")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("ACCURACY")
            .severity(Severity.CRITICAL)
            .businessLogic("#amount != null && #amount > 0")
            .executionOrder(10)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(positiveAmountRule);
        log.info("  ✓ Created rule: {}", positiveAmountRule.getRuleCode());
        
        // Rule 2: Valid Currency Codes
        BusinessRuleEntity validCurrenciesRule = BusinessRuleEntity.builder()
            .ruleId("DQ_ACCURACY_VALID_CURRENCY")
            .regulationId(REGULATION_ID)
            .ruleName("Valid Currency Codes")
            .ruleCode("ACCURACY_VALID_CURRENCY")
            .description("Validates currency against list of valid ISO 4217 codes")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("ACCURACY")
            .severity(Severity.HIGH)
            .businessLogic("#currency == null || #validCurrencies.contains(#currency.toUpperCase())")
            .executionOrder(11)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameterEntity currencyListParam = RuleParameterEntity.builder()
            .rule(validCurrenciesRule)
            .parameterName("validCurrencies")
            .parameterValue("USD,EUR,GBP,JPY,CHF,CAD,AUD,SEK,NOK,DKK,PLN,CZK,HUF,BGN,RON,HRK,RSD,BAM,MKD,ALL,CNY,HKD,SGD,KRW,INR,THB,MYR,IDR,PHP,VND,BRL,MXN,ARS,CLP,COP,PEN,UYU,ZAR,EGP,MAD,TND,NGN,GHS,KES,UGX,TZS,ZMW,BWP,MUR,SCR")
            .parameterType(ParameterType.LIST)
            .dataType("STRING")
            .description("Comma-separated list of valid ISO 4217 currency codes")
            .isConfigurable(true)
            .build();
        
        validCurrenciesRule.addParameter(currencyListParam);
        ruleRepository.save(validCurrenciesRule);
        log.info("  ✓ Created rule: {}", validCurrenciesRule.getRuleCode());
        
        // Rule 3: Valid Country Codes
        BusinessRuleEntity validCountriesRule = BusinessRuleEntity.builder()
            .ruleId("DQ_ACCURACY_VALID_COUNTRY")
            .regulationId(REGULATION_ID)
            .ruleName("Valid Country Codes")
            .ruleCode("ACCURACY_VALID_COUNTRY")
            .description("Validates country against list of valid ISO 3166 codes")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("ACCURACY")
            .severity(Severity.HIGH)
            .businessLogic("#country == null || #validCountries.contains(#country.toUpperCase())")
            .executionOrder(12)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameterEntity countryListParam = RuleParameterEntity.builder()
            .rule(validCountriesRule)
            .parameterName("validCountries")
            .parameterValue("US,GB,DE,FR,IT,ES,NL,BE,AT,CH,SE,NO,DK,FI,IE,PT,GR,PL,CZ,HU,SK,SI,EE,LV,LT,BG,RO,HR,CY,MT,LU,JP,CN,HK,SG,KR,IN,TH,MY,ID,PH,VN,AU,NZ,CA,MX,BR,AR,CL,CO,PE,UY,ZA,EG,MA,TN,NG,GH,KE,UG,TZ,ZM,BW,MU,SC,RU,UA,BY,MD,GE,AM,AZ,KZ,UZ,KG,TJ,TM,MN,TR,IL,SA,AE,QA,KW,BH,OM,JO,LB,SY,IQ,IR,AF,PK,BD,LK,NP,BT,MM,LA,KH,BN,TL,FJ,PG,SB,VU,NC,PF")
            .parameterType(ParameterType.LIST)
            .dataType("STRING")
            .description("Comma-separated list of valid ISO 3166 country codes")
            .isConfigurable(true)
            .build();
        
        validCountriesRule.addParameter(countryListParam);
        ruleRepository.save(validCountriesRule);
        log.info("  ✓ Created rule: {}", validCountriesRule.getRuleCode());
        
        // Rule 4: Valid LEI Format
        BusinessRuleEntity validLeiFormatRule = BusinessRuleEntity.builder()
            .ruleId("DQ_ACCURACY_VALID_LEI_FORMAT")
            .regulationId(REGULATION_ID)
            .ruleName("Valid LEI Format")
            .ruleCode("ACCURACY_VALID_LEI_FORMAT")
            .description("Validates that LEI code has correct format (20 alphanumeric characters)")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("ACCURACY")
            .severity(Severity.HIGH)
            .businessLogic("#leiCode == null || #leiCode.matches('[A-Z0-9]{20}')")
            .executionOrder(13)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(validLeiFormatRule);
        log.info("  ✓ Created rule: {}", validLeiFormatRule.getRuleCode());
        
        // Rule 5: Reasonable Amount
        BusinessRuleEntity reasonableAmountRule = BusinessRuleEntity.builder()
            .ruleId("DQ_ACCURACY_REASONABLE_AMOUNT")
            .regulationId(REGULATION_ID)
            .ruleName("Reasonable Amount")
            .ruleCode("ACCURACY_REASONABLE_AMOUNT")
            .description("Validates that exposure amount is within reasonable bounds (10B EUR)")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("ACCURACY")
            .severity(Severity.MEDIUM)
            .businessLogic("#amount == null || #amount < #maxReasonableAmount")
            .executionOrder(14)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameterEntity maxAmountParam = RuleParameterEntity.builder()
            .rule(reasonableAmountRule)
            .parameterName("maxReasonableAmount")
            .parameterValue("10000000000")
            .parameterType(ParameterType.NUMERIC)
            .dataType("DECIMAL")
            .unit("EUR")
            .description("Maximum reasonable exposure amount (10 billion EUR)")
            .isConfigurable(true)
            .build();
        
        reasonableAmountRule.addParameter(maxAmountParam);
        ruleRepository.save(reasonableAmountRule);
        log.info("  ✓ Created rule: {}", reasonableAmountRule.getRuleCode());
        
        log.info("  ✓ Accuracy rules migration completed (5 rules)");
    }
    
    /**
     * Migrates timeliness validation rules.
     */
    private void migrateTimelinessRules() {
        log.info("→ Migrating Timeliness rules...");
        
        // Rule 1: Reporting Period
        BusinessRuleEntity reportingPeriodRule = BusinessRuleEntity.builder()
            .ruleId("DQ_TIMELINESS_REPORTING_PERIOD")
            .regulationId(REGULATION_ID)
            .ruleName("Reporting Period")
            .ruleCode("TIMELINESS_REPORTING_PERIOD")
            .description("Validates that reporting date is within acceptable reporting period (not older than max age)")
            .ruleType(RuleType.TIMELINESS)
            .ruleCategory("TIMELINESS")
            .severity(Severity.HIGH)
            .businessLogic("#reportingDate == null || T(java.time.temporal.ChronoUnit).DAYS.between(#reportingDate, T(java.time.LocalDate).now()) <= #maxReportingAgeDays")
            .executionOrder(30)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameterEntity maxAgeParam = RuleParameterEntity.builder()
            .rule(reportingPeriodRule)
            .parameterName("maxReportingAgeDays")
            .parameterValue("90")
            .parameterType(ParameterType.NUMERIC)
            .dataType("INTEGER")
            .unit("days")
            .minValue(new BigDecimal("1"))
            .maxValue(new BigDecimal("365"))
            .description("Maximum age for reporting data (90 days)")
            .isConfigurable(true)
            .build();
        
        reportingPeriodRule.addParameter(maxAgeParam);
        ruleRepository.save(reportingPeriodRule);
        log.info("  ✓ Created rule: {}", reportingPeriodRule.getRuleCode());
        
        // Rule 2: No Future Date
        BusinessRuleEntity noFutureDateRule = BusinessRuleEntity.builder()
            .ruleId("DQ_TIMELINESS_NO_FUTURE_DATE")
            .regulationId(REGULATION_ID)
            .ruleName("No Future Date")
            .ruleCode("TIMELINESS_NO_FUTURE_DATE")
            .description("Validates that reporting date is not in the future")
            .ruleType(RuleType.TIMELINESS)
            .ruleCategory("TIMELINESS")
            .severity(Severity.CRITICAL)
            .businessLogic("#reportingDate == null || !#reportingDate.isAfter(T(java.time.LocalDate).now())")
            .executionOrder(31)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(noFutureDateRule);
        log.info("  ✓ Created rule: {}", noFutureDateRule.getRuleCode());
        
        // Rule 3: Recent Valuation
        BusinessRuleEntity recentValuationRule = BusinessRuleEntity.builder()
            .ruleId("DQ_TIMELINESS_RECENT_VALUATION")
            .regulationId(REGULATION_ID)
            .ruleName("Recent Valuation")
            .ruleCode("TIMELINESS_RECENT_VALUATION")
            .description("Validates that valuation date is recent enough (not older than 30 days)")
            .ruleType(RuleType.TIMELINESS)
            .ruleCategory("TIMELINESS")
            .severity(Severity.MEDIUM)
            .businessLogic("#valuationDate == null || T(java.time.temporal.ChronoUnit).DAYS.between(#valuationDate, T(java.time.LocalDate).now()) <= 30")
            .executionOrder(32)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(recentValuationRule);
        log.info("  ✓ Created rule: {}", recentValuationRule.getRuleCode());
        
        log.info("  ✓ Timeliness rules migration completed (3 rules)");
    }
    
    /**
     * Migrates consistency validation rules.
     */
    private void migrateConsistencyRules() {
        log.info("→ Migrating Consistency rules...");
        
        // Rule 1: Currency-Country Consistency
        // Note: This is a simplified version. Full implementation would require complex mapping logic
        BusinessRuleEntity currencyCountryRule = BusinessRuleEntity.builder()
            .ruleId("DQ_CONSISTENCY_CURRENCY_COUNTRY")
            .regulationId(REGULATION_ID)
            .ruleName("Currency-Country Consistency")
            .ruleCode("CONSISTENCY_CURRENCY_COUNTRY")
            .description("Validates that currency matches country (e.g., EUR for Eurozone countries)")
            .ruleType(RuleType.CONSISTENCY)
            .ruleCategory("CONSISTENCY")
            .severity(Severity.MEDIUM)
            .businessLogic("true")  // Placeholder - complex logic requires custom function
            .executionOrder(20)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(false)  // Disabled - requires custom implementation
            .build();
        ruleRepository.save(currencyCountryRule);
        log.info("  ✓ Created rule: {} (disabled, requires custom implementation)", currencyCountryRule.getRuleCode());
        
        // Rule 2: Sector-Counterparty Type Consistency
        BusinessRuleEntity sectorCounterpartyRule = BusinessRuleEntity.builder()
            .ruleId("DQ_CONSISTENCY_SECTOR_COUNTERPARTY")
            .regulationId(REGULATION_ID)
            .ruleName("Sector-Counterparty Type Consistency")
            .ruleCode("CONSISTENCY_SECTOR_COUNTERPARTY")
            .description("Validates that sector classification is consistent with counterparty type")
            .ruleType(RuleType.CONSISTENCY)
            .ruleCategory("CONSISTENCY")
            .severity(Severity.MEDIUM)
            .businessLogic("true")  // Placeholder - complex logic requires custom function
            .executionOrder(21)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(false)  // Disabled - requires custom implementation
            .build();
        ruleRepository.save(sectorCounterpartyRule);
        log.info("  ✓ Created rule: {} (disabled, requires custom implementation)", sectorCounterpartyRule.getRuleCode());
        
        // Rule 3: Rating-Risk Category Consistency
        BusinessRuleEntity ratingRiskRule = BusinessRuleEntity.builder()
            .ruleId("DQ_CONSISTENCY_RATING_RISK")
            .regulationId(REGULATION_ID)
            .ruleName("Rating-Risk Category Consistency")
            .ruleCode("CONSISTENCY_RATING_RISK")
            .description("Validates that internal rating is consistent with risk category")
            .ruleType(RuleType.CONSISTENCY)
            .ruleCategory("CONSISTENCY")
            .severity(Severity.MEDIUM)
            .businessLogic("true")  // Placeholder - complex logic requires custom function
            .executionOrder(22)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(false)  // Disabled - requires custom implementation
            .build();
        ruleRepository.save(ratingRiskRule);
        log.info("  ✓ Created rule: {} (disabled, requires custom implementation)", ratingRiskRule.getRuleCode());
        
        log.info("  ✓ Consistency rules migration completed (3 rules)");
    }
    
    /**
     * Migrates uniqueness validation rules.
     * Note: Uniqueness rules are batch-level and require special handling.
     */
    private void migrateUniquenessRules() {
        log.info("→ Migrating Uniqueness rules...");
        
        // Rule 1: Unique Exposure IDs
        BusinessRuleEntity uniqueExposureIdsRule = BusinessRuleEntity.builder()
            .ruleId("DQ_UNIQUENESS_EXPOSURE_IDS")
            .regulationId(REGULATION_ID)
            .ruleName("Unique Exposure IDs")
            .ruleCode("UNIQUENESS_EXPOSURE_IDS")
            .description("Validates that all exposure IDs within a batch are unique (batch-level check)")
            .ruleType(RuleType.UNIQUENESS)
            .ruleCategory("UNIQUENESS")
            .severity(Severity.CRITICAL)
            .businessLogic("true")  // Batch-level validation requires special handling
            .executionOrder(40)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(false)  // Disabled - requires batch-level implementation
            .build();
        ruleRepository.save(uniqueExposureIdsRule);
        log.info("  ✓ Created rule: {} (disabled, requires batch-level implementation)", uniqueExposureIdsRule.getRuleCode());
        
        // Rule 2: Unique Counterparty-Exposure Pairs
        BusinessRuleEntity uniqueCounterpartyExposureRule = BusinessRuleEntity.builder()
            .ruleId("DQ_UNIQUENESS_COUNTERPARTY_EXPOSURE")
            .regulationId(REGULATION_ID)
            .ruleName("Unique Counterparty-Exposure Pairs")
            .ruleCode("UNIQUENESS_COUNTERPARTY_EXPOSURE")
            .description("Validates that counterparty-exposure pairs are unique within a batch (batch-level check)")
            .ruleType(RuleType.UNIQUENESS)
            .ruleCategory("UNIQUENESS")
            .severity(Severity.HIGH)
            .businessLogic("true")  // Batch-level validation requires special handling
            .executionOrder(41)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(false)  // Disabled - requires batch-level implementation
            .build();
        ruleRepository.save(uniqueCounterpartyExposureRule);
        log.info("  ✓ Created rule: {} (disabled, requires batch-level implementation)", uniqueCounterpartyExposureRule.getRuleCode());
        
        log.info("  ✓ Uniqueness rules migration completed (2 rules)");
    }
    
    /**
     * Migrates validity validation rules.
     */
    private void migrateValidityRules() {
        log.info("→ Migrating Validity rules...");
        
        // Rule 1: Valid Sector
        BusinessRuleEntity validSectorRule = BusinessRuleEntity.builder()
            .ruleId("DQ_VALIDITY_VALID_SECTOR")
            .regulationId(REGULATION_ID)
            .ruleName("Valid Sector")
            .ruleCode("VALIDITY_VALID_SECTOR")
            .description("Validates that sector is from the list of valid sectors")
            .ruleType(RuleType.VALIDITY)
            .ruleCategory("VALIDITY")
            .severity(Severity.HIGH)
            .businessLogic("#sector == null || #validSectors.contains(#sector.toUpperCase())")
            .executionOrder(50)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        
        RuleParameterEntity validSectorsParam = RuleParameterEntity.builder()
            .rule(validSectorRule)
            .parameterName("validSectors")
            .parameterValue("BANKING,CORPORATE_MANUFACTURING,CORPORATE_SERVICES,CORPORATE_RETAIL,CORPORATE_TECHNOLOGY,SOVEREIGN,RETAIL,SME,REAL_ESTATE,INSURANCE,CORPORATE")
            .parameterType(ParameterType.LIST)
            .dataType("STRING")
            .description("Comma-separated list of valid sector codes")
            .isConfigurable(true)
            .build();
        
        validSectorRule.addParameter(validSectorsParam);
        ruleRepository.save(validSectorRule);
        log.info("  ✓ Created rule: {}", validSectorRule.getRuleCode());
        
        // Rule 2: Risk Weight Range
        BusinessRuleEntity riskWeightRangeRule = BusinessRuleEntity.builder()
            .ruleId("DQ_VALIDITY_RISK_WEIGHT_RANGE")
            .regulationId(REGULATION_ID)
            .ruleName("Risk Weight Range")
            .ruleCode("VALIDITY_RISK_WEIGHT_RANGE")
            .description("Validates that risk weight is within valid range (0 to 1.5)")
            .ruleType(RuleType.VALIDITY)
            .ruleCategory("VALIDITY")
            .severity(Severity.HIGH)
            .businessLogic("#riskWeight == null || (#riskWeight >= 0 && #riskWeight <= 1.5)")
            .executionOrder(51)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(riskWeightRangeRule);
        log.info("  ✓ Created rule: {}", riskWeightRangeRule.getRuleCode());
        
        // Rule 3: Maturity After Reporting
        BusinessRuleEntity maturityAfterReportingRule = BusinessRuleEntity.builder()
            .ruleId("DQ_VALIDITY_MATURITY_AFTER_REPORTING")
            .regulationId(REGULATION_ID)
            .ruleName("Maturity After Reporting")
            .ruleCode("VALIDITY_MATURITY_AFTER_REPORTING")
            .description("Validates that maturity date is after or equal to reporting date")
            .ruleType(RuleType.VALIDITY)
            .ruleCategory("VALIDITY")
            .severity(Severity.MEDIUM)
            .businessLogic("#maturityDate == null || #reportingDate == null || !#maturityDate.isBefore(#reportingDate)")
            .executionOrder(52)
            .effectiveDate(EFFECTIVE_DATE)
            .enabled(true)
            .build();
        ruleRepository.save(maturityAfterReportingRule);
        log.info("  ✓ Created rule: {}", maturityAfterReportingRule.getRuleCode());
        
        log.info("  ✓ Validity rules migration completed (3 rules)");
    }
}

