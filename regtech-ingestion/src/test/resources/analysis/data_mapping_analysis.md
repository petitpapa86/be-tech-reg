# Data Mapping Analysis: Community First Bank → Italian Large Exposures Template

## Input Data Structure
```json
{
  "bank_info": {
    "bank_name": "Community First Bank",
    "report_date": "2024-09-12", 
    "total_loans": 5
  },
  "loan_portfolio": [
    {
      "loan_id": "LOAN001",
      "borrower_name": "Mike's Pizza Inc",
      "borrower_id": "CORP12345", 
      "loan_amount": 250000,
      "currency": "USD",
      "loan_type": "Business Loan",
      "borrower_country": "US"
    }
    // ... more loans
  ]
}
```

## Template Requirements vs Available Data

### ✅ AVAILABLE MAPPINGS
| Template Field | Input Field | Mapping Notes |
|---|---|---|
| `bank_information.bank_name` | `bank_info.bank_name` | Direct mapping |
| `bank_information.reporting_date` | `bank_info.report_date` | Direct mapping |
| `exposures[].counterparty_name` | `loan_portfolio[].borrower_name` | Direct mapping |
| `exposures[].counterparty_id` | `loan_portfolio[].borrower_id` | Direct mapping |
| `exposures[].gross_exposure_amount` | `loan_portfolio[].loan_amount` | Direct mapping |
| `exposures[].currency` | `loan_portfolio[].currency` | Direct mapping |
| `exposures[].country_code` | `loan_portfolio[].borrower_country` | Direct mapping |

### ❌ MISSING CRITICAL FIELDS
| Template Field | Status | Impact |
|---|---|---|
| `bank_information.abi_code` | **MISSING** | 🔴 MANDATORY - Italian bank identifier |
| `bank_information.lei_code` | **MISSING** | 🔴 MANDATORY - Legal Entity Identifier |
| `capital_data.total_capital` | **MISSING** | 🔴 MANDATORY - Required for risk calculations |
| `capital_data.tier1_capital` | **MISSING** | 🔴 MANDATORY - Required for exposure limits |
| `capital_data.eligible_capital_large_exposures` | **MISSING** | 🔴 MANDATORY - Base for percentage calculations |
| `exposures[].exposure_id` | **MISSING** | 🔴 MANDATORY - Unique exposure identifier |
| `exposures[].net_exposure_amount` | **MISSING** | 🔴 MANDATORY - Post credit risk mitigation |
| `exposures[].sector` | **MISSING** | 🔴 MANDATORY - Economic sector classification |
| `exposures[].exposure_type` | **MISSING** | 🔴 MANDATORY - On/off balance sheet classification |

### ⚠️ DATA QUALITY ISSUES IN INPUT
| Issue | Records Affected | BCBS 239 Principle Violated |
|---|---|---|
| Empty `loan_id` | LOAN005 (Mary's Bakery) | Principle 3: Accuracy & Integrity |
| Empty `borrower_id` | LOAN003 (Tech Startup LLC) | Principle 3: Accuracy & Integrity |
| Negative amount | LOAN003 (-50000) | Principle 3: Accuracy & Integrity |
| Invalid currency | LOAN004 ("XXX") | Principle 3: Accuracy & Integrity |
| Currency mismatch | USD loans vs EUR template | Principle 4: Completeness |

### 🔄 REQUIRED TRANSFORMATIONS
| Transformation | Logic | BCBS 239 Compliance |
|---|---|---|
| **Currency Conversion** | USD/EUR → EUR (template base) | Principle 4: Completeness |
| **Sector Classification** | `loan_type` → `sector` enum | Principle 4: Completeness |
| **Exposure ID Generation** | `loan_id` → `exposure_id` format | Principle 3: Accuracy |
| **Country Code Mapping** | "US" → "US", "DE" → "DE", "CA" → "CA" | Principle 3: Accuracy |
| **Net Amount Calculation** | `gross_amount` - `credit_risk_mitigation` | Principle 3: Accuracy |

## Ingestion System Requirements

### 1. Data Enrichment Needed
```yaml
enrichment_rules:
  - field: "abi_code"
    source: "bank_registry_lookup"
    lookup_key: "bank_name"
    
  - field: "lei_code" 
    source: "gleif_registry_lookup"
    lookup_key: "bank_name"
    
  - field: "capital_data"
    source: "regulatory_reporting_system"
    lookup_key: "abi_code"
    
  - field: "sector"
    source: "loan_type_mapping"
    mapping:
      "Business Loan": "CORPORATE"
      "Mortgage": "RETAIL" 
      "Credit Line": "CORPORATE"
      "Equipment Loan": "CORPORATE"
      "Small Business": "CORPORATE"
```

### 2. Validation Rules Implementation
```yaml
validation_rules:
  bcbs_239_principle_3:
    - rule: "loan_id_not_empty"
      message: "Loan ID is mandatory for traceability"
      
    - rule: "borrower_id_not_empty" 
      message: "Borrower ID required for counterparty identification"
      
    - rule: "positive_amounts"
      message: "Loan amounts must be positive"
      
    - rule: "valid_currency_codes"
      message: "Currency must be valid ISO 4217 code"
      
  bcbs_239_principle_4:
    - rule: "all_exposures_covered"
      message: "All material exposures must be included"
      
    - rule: "capital_data_available"
      message: "Capital data required for risk calculations"
```

### 3. Business Logic for BCBS 239 Compliance
```yaml
business_calculations:
  large_exposure_percentage:
    formula: "(net_exposure_amount / eligible_capital_large_exposures) * 100"
    threshold: 10.0
    
  legal_limit_check:
    formula: "exposure_percentage <= 25.0"
    exception_rules:
      - "eu_sovereigns: no_limit"
      - "covered_bonds: 150.0"
      
  data_quality_score:
    completeness_weight: 0.4
    accuracy_weight: 0.4  
    consistency_weight: 0.2
```

## Expected Ingestion Results

### Processing Summary
- **Total Records**: 5 loans
- **Valid Records**: 2 (LOAN001, LOAN002) 
- **Invalid Records**: 3 (LOAN003, LOAN004, LOAN005)
- **Data Quality Score**: ~60% (due to missing fields and validation errors)
- **BCBS 239 Compliance**: PARTIAL (missing critical regulatory data)

### Validation Issues Expected
1. **LOAN003**: Missing borrower_id, negative amount
2. **LOAN004**: Invalid currency code "XXX"  
3. **LOAN005**: Missing loan_id
4. **ALL RECORDS**: Missing sector classification, exposure_type, capital_data

### Recommendations for Bank
1. **Immediate**: Fix data quality issues in source system
2. **Short-term**: Implement data enrichment from regulatory systems
3. **Long-term**: Align source data structure with BCBS 239 requirements