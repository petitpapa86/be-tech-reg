# Specification Classes Deprecation Notice

## Overview

As of **version 2.0**, all Specification-based validation classes in the Data Quality module have been **deprecated** and will be removed in **version 3.0**. All validation logic has been successfully migrated to the database-driven Rules Engine.

## Deprecated Classes

The following classes are now deprecated:

| Class | Package | Replacement |
|-------|---------|-------------|
| `CompletenessSpecifications` | `com.bcbs239.regtech.dataquality.domain.specifications` | Rules Engine with `COMPLETENESS` rule type |
| `AccuracySpecifications` | `com.bcbs239.regtech.dataquality.domain.specifications` | Rules Engine with `ACCURACY` rule type |
| `ConsistencySpecifications` | `com.bcbs239.regtech.dataquality.domain.specifications` | Rules Engine with `CONSISTENCY` rule type |
| `TimelinessSpecifications` | `com.bcbs239.regtech.dataquality.domain.specifications` | Rules Engine with `TIMELINESS` rule type |
| `UniquenessSpecifications` | `com.bcbs239.regtech.dataquality.domain.specifications` | Rules Engine with `UNIQUENESS` rule type |

## Why Are We Deprecating Specifications?

The Rules Engine provides significant advantages over hardcoded Specifications:

### 1. **Configurability**
- Rules can be modified through database updates without code deployment
- No need to rebuild and redeploy the application for rule changes
- Business users can manage rules without developer involvement

### 2. **Dynamic Control**
- Enable or disable rules at runtime based on business needs
- Adjust rule parameters (thresholds, lists) without code changes
- Respond quickly to regulatory changes

### 3. **Auditability**
- All rule executions are persisted to the database
- Complete audit trail of violations and resolutions
- Track rule changes over time

### 4. **Flexibility**
- Support for exemptions with validity periods
- Configurable severity levels per rule
- Custom error messages and field mappings

### 5. **Maintainability**
- Centralized rule management in the database
- Consistent rule format using SpEL expressions
- Easier to test and validate rule changes

## Migration Timeline

| Version | Status | Description |
|---------|--------|-------------|
| **2.0** | ✅ Current | Specifications marked as `@Deprecated`, Rules Engine fully operational |
| **2.1** | 🔜 Planned | Deprecation warnings logged when Specifications are used |
| **3.0** | 📅 Future | Specifications removed entirely, Rules Engine only |

## How to Migrate

### Step 1: Enable Rules Engine

Ensure the Rules Engine is enabled in your configuration:

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 300
```

### Step 2: Verify Rules Are Loaded

Check that all rules have been migrated to the database. Run the application and verify the startup logs:

```
INFO  [RulesEngine] Loaded 25 active rules from database
INFO  [RulesEngine] Rules Engine initialized successfully
```

### Step 3: Update Your Code

**Before (Deprecated):**
```java
import com.bcbs239.regtech.dataquality.domain.specifications.*;

// Using hardcoded Specifications
ValidationResult result = ValidationResult.validate(
    exposure,
    CompletenessSpecifications.hasRequiredFields(),
    AccuracySpecifications.hasPositiveAmount(),
    TimelinessSpecifications.isWithinReportingPeriod()
);
```

**After (Recommended):**
```java
import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;

// Using Rules Engine
ValidationResult result = ValidationResult.validate(
    exposure,
    dataQualityRulesService
);
```

### Step 4: Test Thoroughly

Run all existing tests to ensure the Rules Engine produces identical validation results:

```bash
mvn test -pl regtech-data-quality
```

All 309 tests should pass without modification.

## Rule Equivalence Mapping

Each Specification method has been migrated to one or more database rules:

### Completeness Rules

| Specification Method | Database Rule Code |
|---------------------|-------------------|
| `hasRequiredFields()` | `COMPLETENESS_EXPOSURE_ID_REQUIRED`<br>`COMPLETENESS_AMOUNT_REQUIRED`<br>`COMPLETENESS_CURRENCY_REQUIRED`<br>`COMPLETENESS_COUNTRY_REQUIRED`<br>`COMPLETENESS_SECTOR_REQUIRED` |
| `hasLeiForCorporates()` | `COMPLETENESS_LEI_FOR_CORPORATES` |
| `hasMaturityForTermExposures()` | `COMPLETENESS_MATURITY_FOR_TERM` |
| `hasInternalRating()` | `COMPLETENESS_INTERNAL_RATING` |

### Accuracy Rules

| Specification Method | Database Rule Code |
|---------------------|-------------------|
| `hasPositiveAmount()` | `ACCURACY_POSITIVE_AMOUNT` |
| `hasValidCurrency()` | `ACCURACY_VALID_CURRENCY` |
| `hasValidCountry()` | `ACCURACY_VALID_COUNTRY` |
| `hasValidLeiFormat()` | `ACCURACY_VALID_LEI_FORMAT` |
| `hasReasonableAmount()` | `ACCURACY_REASONABLE_AMOUNT` |

### Consistency Rules

| Specification Method | Database Rule Code |
|---------------------|-------------------|
| `currencyMatchesCountry()` | `CONSISTENCY_CURRENCY_COUNTRY` |
| `sectorMatchesCounterpartyType()` | `CONSISTENCY_SECTOR_COUNTERPARTY` |
| `ratingMatchesRiskCategory()` | `CONSISTENCY_RATING_RISK` |

### Timeliness Rules

| Specification Method | Database Rule Code |
|---------------------|-------------------|
| `isWithinReportingPeriod()` | `TIMELINESS_REPORTING_PERIOD` |
| `isNotFutureDate()` | `TIMELINESS_NO_FUTURE_DATE` |
| `hasRecentValuation()` | `TIMELINESS_RECENT_VALUATION` |

### Uniqueness Rules

| Specification Method | Database Rule Code |
|---------------------|-------------------|
| `hasUniqueExposureIds()` | `UNIQUENESS_EXPOSURE_IDS` |
| `hasUniqueCounterpartyExposurePairs()` | `UNIQUENESS_COUNTERPARTY_EXPOSURE` |

## Frequently Asked Questions

### Q: Will my existing code break?

**A:** No. The deprecated Specifications will continue to work in version 2.x. However, you should migrate to the Rules Engine before version 3.0.

### Q: Do I need to change my tests?

**A:** No. All existing tests should pass without modification. The Rules Engine produces identical validation results.

### Q: Can I use both Specifications and Rules Engine?

**A:** Yes, during the migration period (version 2.x), both approaches work. However, we recommend migrating fully to the Rules Engine.

### Q: How do I manage rules in the database?

**A:** Rules are stored in the `business_rules` table. You can:
- View rules: `SELECT * FROM business_rules WHERE enabled = true;`
- Disable a rule: `UPDATE business_rules SET enabled = false WHERE rule_code = 'RULE_CODE';`
- Update parameters: `UPDATE rule_parameters SET parameter_value = 'new_value' WHERE rule_id = X;`

### Q: What if I find a bug in a migrated rule?

**A:** You can fix it by updating the SpEL expression in the database:
```sql
UPDATE business_rules 
SET rule_expression = 'new_expression' 
WHERE rule_code = 'RULE_CODE';
```

No code deployment required!

### Q: How do I add a new validation rule?

**A:** Insert a new rule into the database:
```sql
INSERT INTO business_rules (rule_code, rule_name, rule_type, rule_expression, enabled)
VALUES ('MY_NEW_RULE', 'My New Rule', 'ACCURACY', '#amount > 0', true);
```

### Q: Will performance be affected?

**A:** No. The Rules Engine uses in-memory caching and has been tested to perform within 10% of Specification-based validation.

## Support and Resources

### Documentation

- [Rules Engine Configuration Guide](RULES_ENGINE_CONFIGURATION_GUIDE.md) - Complete configuration reference
- [Rules Engine Implementation Summary](RULES_ENGINE_IMPLEMENTATION_SUMMARY.md) - Technical implementation details
- [Design Document](../.kiro/specs/data-quality-rules-integration/design.md) - Architecture and design decisions
- [Requirements Document](../.kiro/specs/data-quality-rules-integration/requirements.md) - Functional requirements

### Getting Help

If you encounter issues during migration:

1. Check the startup logs for configuration errors
2. Review the Rules Engine Configuration Guide
3. Verify all rules are loaded: `SELECT COUNT(*) FROM business_rules WHERE enabled = true;`
4. Run tests to ensure validation results match: `mvn test`
5. Contact the development team for assistance

## Summary

The migration from Specifications to the Rules Engine represents a significant improvement in the Data Quality module's flexibility and maintainability. While Specifications served us well, the Rules Engine provides the configurability and auditability required for modern regulatory compliance systems.

**Action Required:**
- ✅ Ensure Rules Engine is enabled in your configuration
- ✅ Verify all rules are loaded from the database
- ✅ Plan migration of custom code using Specifications
- ✅ Test thoroughly before version 3.0 release

**Timeline:**
- **Now (v2.0):** Start planning migration
- **v2.1:** Complete migration (deprecation warnings)
- **v3.0:** Specifications removed (Rules Engine only)

---

*Last Updated: November 26, 2025*  
*Version: 2.0*
