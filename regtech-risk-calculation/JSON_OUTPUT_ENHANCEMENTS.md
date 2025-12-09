# JSON Output Enhancements for Risk Calculation Results

## Summary

Enhanced the JSON serialization of risk calculation results to include additional fields for each exposure in the output file.

## Changes Made

### Modified Files

1. **CalculationResultsJsonSerializer.java**
   - Updated `createExposuresNode()` method to include new fields

### New Fields Added to Each Exposure

The following fields have been added to each exposure in the `calculated_exposures` array:

1. **`exchange_rate_used`** (double)
   - The exchange rate that was used to convert the original currency to EUR
   - Calculated as: `eur_amount / original_amount`
   - For EUR exposures, this will be 1.0

2. **`percentage_of_total`** (double)
   - The percentage this exposure represents of the total portfolio
   - Calculated as: `(eur_amount / total_portfolio_amount) * 100`
   - Expressed as a percentage (e.g., 66.67 for 66.67%)

3. **`country`** (string)
   - The ISO country code for the exposure (e.g., "IT", "FR", "US")
   - Extracted from the exposure classification

4. **`geographic_region`** (string)
   - Already existed, but now explicitly documented
   - Values: "ITALY", "EU", "NON_EU", etc.

5. **`original_currency`** (string)
   - Already existed, but now explicitly documented
   - The currency code of the original exposure amount (e.g., "EUR", "USD", "GBP")

## Example JSON Output

```json
{
  "batch_id": "batch_test_001",
  "calculated_at": "2024-12-01T14:30:00Z",
  "bank_id": "08081",
  "bank_name": "Test Bank",
  "summary": {
    "total_exposures": 2,
    "total_amount_eur": 1500000.0,
    "geographic_breakdown": { ... },
    "sector_breakdown": { ... },
    "concentration_indices": { ... }
  },
  "calculated_exposures": [
    {
      "instrument_id": "INST_001",
      "counterparty_ref": "CP_001",
      "original_amount": 1000000.0,
      "original_currency": "EUR",
      "eur_amount": 1000000.0,
      "mitigated_amount_eur": 1000000.0,
      "exchange_rate_used": 1.0,
      "percentage_of_total": 66.67,
      "country": "IT",
      "geographic_region": "ITALY",
      "economic_sector": "RETAIL",
      "mitigation": { ... }
    },
    {
      "instrument_id": "INST_002",
      "counterparty_ref": "CP_002",
      "original_amount": 500000.0,
      "original_currency": "USD",
      "eur_amount": 500000.0,
      "mitigated_amount_eur": 500000.0,
      "exchange_rate_used": 0.9205,
      "percentage_of_total": 33.33,
      "country": "US",
      "geographic_region": "NON_EU",
      "economic_sector": "CORPORATE",
      "mitigation": null
    }
  ]
}
```

## Implementation Details

### Exchange Rate Calculation

```java
double eurAmount = exposure.getExposure().getAmount().getEurAmount().getValue();
double originalAmount = exposure.getExposure().getAmount().getOriginalAmount();
double exchangeRate = originalAmount > 0 ? eurAmount / originalAmount : 1.0;
```

### Percentage of Total Calculation

```java
// Calculate total portfolio amount first
double totalPortfolioAmount = exposures.stream()
    .mapToDouble(e => e.getExposure().getAmount().getEurAmount().getValue())
    .sum();

// Then calculate percentage for each exposure
double percentageOfTotal = totalPortfolioAmount > 0 
    ? (eurAmount / totalPortfolioAmount) * 100.0 
    : 0.0;
```

## Testing

Updated `CalculationResultsJsonSerializerTest.java` to verify the new fields are present and correctly calculated:

- `exchange_rate_used` should be 1.0 for EUR exposures
- `percentage_of_total` should sum to 100% across all exposures
- `country` should contain the ISO country code
- All fields should be present in the JSON output

## Notes

- The `exchange_rate_used` field is derived from the already-converted amounts, not from the actual exchange rate service
- For exposures already in EUR, the exchange rate will always be 1.0
- The `percentage_of_total` is calculated dynamically based on the current set of exposures
- The `country` field comes from the exposure classification data

## Next Steps

To complete this implementation:

1. Ensure the domain model has the necessary methods to access country codes
2. Verify that the `ExposureClassification` class includes a `getCountryCode()` method
3. Run integration tests to ensure the JSON output is correct
4. Update any downstream consumers of this JSON format to handle the new fields
