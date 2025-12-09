package com.bcbs239.regtech.modules.ingestion.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test demonstrating BCBS 239 validation with separation of concerns:
 * - Uploaded file contains only transaction/exposure data
 * - Bank configuration (capital data, templates) comes from internal systems
 * - Template validation rules are applied during processing
 * 
 * NOTE: This test is disabled because it requires a full Spring Boot application context
 * that is not available in the infrastructure module tests. This is a demonstration/documentation
 * test that should be moved to an integration test module or properly configured with
 * @ContextConfiguration to specify the required beans.
 * 
 * TODO: Move to proper integration test module with full application context
 */
@org.junit.jupiter.api.Disabled("Requires full Spring Boot application context - needs proper test configuration")
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("BCBS 239 Validation with Bank Configuration Lookup")
class BCBS239ValidationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should validate original problematic loan file and identify all issues")
    void shouldValidateOriginalProblematicLoanFile() throws IOException {
        // Given: Original problematic loan file
        ClassPathResource inputFile = new ClassPathResource("test-data/daily_loans_2024_09_12.json");
        JsonNode loanData = objectMapper.readTree(inputFile.getInputStream());
        
        // And: Bank configuration from internal systems
        BankConfiguration bankConfig = getBankConfiguration("08081"); // ABI code lookup
        
        // And: BCBS 239 template configuration
        BCBS239Template template = getTemplate("IT_LARGE_EXPOSURES_CIRCULARE_285");
        
        // When: Processing with full validation
        ValidationResult result = processWithBCBS239Validation(loanData, bankConfig, template);
        
        // Then: Should identify all data quality and regulatory issues
        assertThat(result.isValid()).isFalse();
        assertThat(result.getComplianceScore()).isBetween(0.2, 0.5); // Low due to missing data
        
        // Verify specific issues are caught
        List<ValidationIssue> issues = result.getIssues();
        
        // Data quality issues from original file
        assertThat(issues).anySatisfy(issue -> {
            assertThat(issue.getRecordId()).isEqualTo("LOAN003");
            assertThat(issue.getFieldName()).isEqualTo("borrower_id");
            assertThat(issue.getIssueType()).isEqualTo("MISSING_REQUIRED_FIELD");
        });
        
        assertThat(issues).anySatisfy(issue -> {
            assertThat(issue.getRecordId()).isEqualTo("LOAN003");
            assertThat(issue.getFieldName()).isEqualTo("loan_amount");
            assertThat(issue.getIssueType()).isEqualTo("INVALID_VALUE_RANGE");
        });
        
        assertThat(issues).anySatisfy(issue -> {
            assertThat(issue.getRecordId()).isEqualTo("LOAN004");
            assertThat(issue.getFieldName()).isEqualTo("currency");
            assertThat(issue.getIssueType()).isEqualTo("INVALID_FORMAT");
        });
        
        // Missing regulatory fields
        assertThat(issues).anySatisfy(issue -> {
            assertThat(issue.getFieldName()).isEqualTo("sector");
            assertThat(issue.getIssueType()).isEqualTo("MISSING_REGULATORY_FIELD");
        });
    }

    @Test
    @DisplayName("Should validate enhanced loan file with bank config lookup and achieve high compliance")
    void shouldValidateEnhancedLoanFileWithBankConfig() throws IOException {
        // Given: Enhanced loan file with proper data
        ClassPathResource inputFile = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        JsonNode loanData = objectMapper.readTree(inputFile.getInputStream());
        
        // And: Bank configuration from internal systems (simulated)
        BankConfiguration bankConfig = getBankConfiguration("08081");
        
        // And: BCBS 239 template
        BCBS239Template template = getTemplate("IT_LARGE_EXPOSURES_CIRCULARE_285");
        
        // When: Processing with full validation
        ValidationResult result = processWithBCBS239Validation(loanData, bankConfig, template);
        
        // Then: Should achieve high compliance score
        assertThat(result.isValid()).isTrue();
        assertThat(result.getComplianceScore()).isGreaterThan(0.9); // High compliance
        
        // Verify BCBS 239 calculations work
        List<ExposureCalculation> calculations = result.getExposureCalculations();
        assertThat(calculations).hasSize(5);
        
        // Verify large exposure calculations
        ExposureCalculation loan004 = calculations.stream()
            .filter(calc -> "LOAN004".equals(calc.getLoanId()))
            .findFirst()
            .orElseThrow();
            
        // LOAN004: 700,000 EUR net exposure / 125,000,000 EUR capital = 0.56%
        assertThat(loan004.getExposurePercentage()).isBetween(0.5, 0.6);
        assertThat(loan004.isLargeExposure()).isFalse(); // < 10% threshold
        assertThat(loan004.exceedsLimit()).isFalse(); // < 25% limit
    }

    @Test
    @DisplayName("Should demonstrate bank configuration lookup and template application")
    void shouldDemonstrateBankConfigurationLookup() {
        // Given: Bank ABI code from uploaded file
        String abiCode = "08081";
        
        // When: Looking up bank configuration
        BankConfiguration config = getBankConfiguration(abiCode);
        
        // Then: Should retrieve complete regulatory data
        assertThat(config.getAbiCode()).isEqualTo("08081");
        assertThat(config.getLeiCode()).isEqualTo("815600D7623147C25D86");
        assertThat(config.getBankName()).isEqualTo("Community First Bank");
        assertThat(config.getTotalCapital()).isEqualTo(new BigDecimal("125000000.00"));
        assertThat(config.getTier1Capital()).isEqualTo(new BigDecimal("115000000.00"));
        assertThat(config.getEligibleCapitalLargeExposures()).isEqualTo(new BigDecimal("125000000.00"));
        
        // And: Template configuration should be available
        BCBS239Template template = getTemplate("IT_LARGE_EXPOSURES_CIRCULARE_285");
        assertThat(template.getId()).isEqualTo("IT_LARGE_EXPOSURES_CIRCULARE_285");
        assertThat(template.getLargeExposureThreshold()).isEqualTo(10.0);
        assertThat(template.getLegalLimitGeneral()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Should calculate exposure percentages and identify large exposures")
    void shouldCalculateExposurePercentagesAndIdentifyLargeExposures() throws IOException {
        // Given: Enhanced loan data
        ClassPathResource inputFile = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        JsonNode loanData = objectMapper.readTree(inputFile.getInputStream());
        
        BankConfiguration bankConfig = getBankConfiguration("08081");
        BCBS239Template template = getTemplate("IT_LARGE_EXPOSURES_CIRCULARE_285");
        
        // When: Processing exposures
        List<ExposureCalculation> calculations = calculateExposures(loanData, bankConfig, template);
        
        // Then: Should calculate correct percentages
        assertThat(calculations).hasSize(5);
        
        // Verify calculations for each loan
        Map<String, ExposureCalculation> calcMap = new HashMap<>();
        calculations.forEach(calc -> calcMap.put(calc.getLoanId(), calc));
        
        // LOAN001: 240,000 / 125,000,000 = 0.192%
        ExposureCalculation loan001 = calcMap.get("LOAN001");
        assertThat(loan001.getExposurePercentage()).isBetween(0.19, 0.20);
        assertThat(loan001.isLargeExposure()).isFalse();
        
        // LOAN002: 450,000 / 125,000,000 = 0.36%
        ExposureCalculation loan002 = calcMap.get("LOAN002");
        assertThat(loan002.getExposurePercentage()).isBetween(0.35, 0.37);
        assertThat(loan002.isLargeExposure()).isFalse();
        
        // LOAN004: 700,000 / 125,000,000 = 0.56%
        ExposureCalculation loan004 = calcMap.get("LOAN004");
        assertThat(loan004.getExposurePercentage()).isBetween(0.55, 0.57);
        assertThat(loan004.isLargeExposure()).isFalse();
        
        // All exposures should be well below 10% threshold
        assertThat(calculations).allSatisfy(calc -> 
            assertThat(calc.isLargeExposure()).isFalse()
        );
    }

    // Helper methods to simulate system components

    private BankConfiguration getBankConfiguration(String abiCode) {
        // Simulate lookup from bank configuration tables
        return new BankConfiguration(
            abiCode,
            "815600D7623147C25D86", // LEI code
            "Community First Bank",
            new BigDecimal("125000000.00"), // Total capital
            new BigDecimal("115000000.00"), // Tier 1 capital
            new BigDecimal("125000000.00")  // Eligible capital for large exposures
        );
    }

    private BCBS239Template getTemplate(String templateId) {
        // Simulate template configuration lookup
        return new BCBS239Template(
            templateId,
            "Italian Large Exposures - Circolare 285",
            10.0, // Large exposure threshold
            25.0, // Legal limit general
            25.0  // Shadow banking limit
        );
    }

    private ValidationResult processWithBCBS239Validation(JsonNode loanData, BankConfiguration bankConfig, BCBS239Template template) {
        ValidationResult result = new ValidationResult();
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Validate bank info
        JsonNode bankInfo = loanData.get("bank_info");
        if (!bankInfo.has("abi_code")) {
            issues.add(new ValidationIssue("BANK_INFO", "abi_code", "MISSING_REGULATORY_FIELD", "ABI code required"));
        }
        if (!bankInfo.has("lei_code")) {
            issues.add(new ValidationIssue("BANK_INFO", "lei_code", "MISSING_REGULATORY_FIELD", "LEI code required"));
        }
        
        // Validate loan portfolio
        JsonNode loanPortfolio = loanData.get("loan_portfolio");
        if (loanPortfolio != null && loanPortfolio.isArray()) {
            for (JsonNode loan : loanPortfolio) {
                validateLoanRecord(loan, issues);
            }
        }
        
        // Calculate compliance score
        double complianceScore = calculateComplianceScore(loanData, issues);
        
        result.setValid(issues.stream().noneMatch(issue -> "ERROR".equals(issue.getSeverity())));
        result.setComplianceScore(complianceScore);
        result.setIssues(issues);
        
        // Calculate exposures if data is valid
        if (result.isValid()) {
            List<ExposureCalculation> calculations = calculateExposures(loanData, bankConfig, template);
            result.setExposureCalculations(calculations);
        }
        
        return result;
    }

    private void validateLoanRecord(JsonNode loan, List<ValidationIssue> issues) {
        String loanId = loan.has("loan_id") ? loan.get("loan_id").asText() : "";
        
        // Check required fields
        if (loanId.isEmpty()) {
            issues.add(new ValidationIssue(loanId, "loan_id", "MISSING_REQUIRED_FIELD", "Loan ID is required"));
        }
        
        String borrowerId = loan.has("borrower_id") ? loan.get("borrower_id").asText() : "";
        if (borrowerId.isEmpty()) {
            issues.add(new ValidationIssue(loanId, "borrower_id", "MISSING_REQUIRED_FIELD", "Borrower ID is required"));
        }
        
        // Check loan amount
        if (loan.has("loan_amount")) {
            double amount = loan.get("loan_amount").asDouble();
            if (amount < 0) {
                issues.add(new ValidationIssue(loanId, "loan_amount", "INVALID_VALUE_RANGE", "Loan amount cannot be negative"));
            }
        }
        
        // Check currency
        if (loan.has("currency")) {
            String currency = loan.get("currency").asText();
            if ("XXX".equals(currency)) {
                issues.add(new ValidationIssue(loanId, "currency", "INVALID_FORMAT", "Invalid currency code"));
            }
        }
        
        // Check regulatory fields
        if (!loan.has("sector")) {
            issues.add(new ValidationIssue(loanId, "sector", "MISSING_REGULATORY_FIELD", "Sector classification required"));
        }
        
        if (!loan.has("exposure_type")) {
            issues.add(new ValidationIssue(loanId, "exposure_type", "MISSING_REGULATORY_FIELD", "Exposure type required"));
        }
    }

    private double calculateComplianceScore(JsonNode loanData, List<ValidationIssue> issues) {
        double score = 1.0;
        
        // Deduct for each issue
        long errorCount = issues.stream().filter(issue -> "ERROR".equals(issue.getSeverity())).count();
        long warningCount = issues.stream().filter(issue -> "WARNING".equals(issue.getSeverity())).count();
        
        score -= (errorCount * 0.1); // 10% per error
        score -= (warningCount * 0.05); // 5% per warning
        
        return Math.max(0.0, score);
    }

    private List<ExposureCalculation> calculateExposures(JsonNode loanData, BankConfiguration bankConfig, BCBS239Template template) {
        List<ExposureCalculation> calculations = new ArrayList<>();
        
        JsonNode loanPortfolio = loanData.get("loan_portfolio");
        if (loanPortfolio != null && loanPortfolio.isArray()) {
            for (JsonNode loan : loanPortfolio) {
                String loanId = loan.get("loan_id").asText();
                double netExposure = loan.has("net_exposure_amount") ? 
                    loan.get("net_exposure_amount").asDouble() : 
                    loan.get("loan_amount").asDouble();
                
                double exposurePercentage = (netExposure / bankConfig.getEligibleCapitalLargeExposures().doubleValue()) * 100;
                boolean isLargeExposure = exposurePercentage >= template.getLargeExposureThreshold();
                boolean exceedsLimit = exposurePercentage > template.getLegalLimitGeneral();
                
                calculations.add(new ExposureCalculation(
                    loanId,
                    netExposure,
                    exposurePercentage,
                    isLargeExposure,
                    exceedsLimit
                ));
            }
        }
        
        return calculations;
    }

    // Data classes for testing
    public static class BankConfiguration {
        private final String abiCode;
        private final String leiCode;
        private final String bankName;
        private final BigDecimal totalCapital;
        private final BigDecimal tier1Capital;
        private final BigDecimal eligibleCapitalLargeExposures;

        public BankConfiguration(String abiCode, String leiCode, String bankName, 
                               BigDecimal totalCapital, BigDecimal tier1Capital, 
                               BigDecimal eligibleCapitalLargeExposures) {
            this.abiCode = abiCode;
            this.leiCode = leiCode;
            this.bankName = bankName;
            this.totalCapital = totalCapital;
            this.tier1Capital = tier1Capital;
            this.eligibleCapitalLargeExposures = eligibleCapitalLargeExposures;
        }

        // Getters
        public String getAbiCode() { return abiCode; }
        public String getLeiCode() { return leiCode; }
        public String getBankName() { return bankName; }
        public BigDecimal getTotalCapital() { return totalCapital; }
        public BigDecimal getTier1Capital() { return tier1Capital; }
        public BigDecimal getEligibleCapitalLargeExposures() { return eligibleCapitalLargeExposures; }
    }

    public static class BCBS239Template {
        private final String id;
        private final String name;
        private final double largeExposureThreshold;
        private final double legalLimitGeneral;
        private final double shadowBankingLimit;

        public BCBS239Template(String id, String name, double largeExposureThreshold, 
                             double legalLimitGeneral, double shadowBankingLimit) {
            this.id = id;
            this.name = name;
            this.largeExposureThreshold = largeExposureThreshold;
            this.legalLimitGeneral = legalLimitGeneral;
            this.shadowBankingLimit = shadowBankingLimit;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public double getLargeExposureThreshold() { return largeExposureThreshold; }
        public double getLegalLimitGeneral() { return legalLimitGeneral; }
        public double getShadowBankingLimit() { return shadowBankingLimit; }
    }

    public static class ValidationResult {
        private boolean valid;
        private double complianceScore;
        private List<ValidationIssue> issues;
        private List<ExposureCalculation> exposureCalculations;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public double getComplianceScore() { return complianceScore; }
        public void setComplianceScore(double score) { this.complianceScore = score; }
        
        public List<ValidationIssue> getIssues() { return issues; }
        public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }
        
        public List<ExposureCalculation> getExposureCalculations() { return exposureCalculations; }
        public void setExposureCalculations(List<ExposureCalculation> calculations) { this.exposureCalculations = calculations; }
    }

    public static class ValidationIssue {
        private final String recordId;
        private final String fieldName;
        private final String issueType;
        private final String message;
        private final String severity;

        public ValidationIssue(String recordId, String fieldName, String issueType, String message) {
            this.recordId = recordId;
            this.fieldName = fieldName;
            this.issueType = issueType;
            this.message = message;
            this.severity = issueType.contains("MISSING_REGULATORY") ? "ERROR" : "WARNING";
        }

        // Getters
        public String getRecordId() { return recordId; }
        public String getFieldName() { return fieldName; }
        public String getIssueType() { return issueType; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
    }

    public static class ExposureCalculation {
        private final String loanId;
        private final double netExposureAmount;
        private final double exposurePercentage;
        private final boolean largeExposure;
        private final boolean exceedsLimit;

        public ExposureCalculation(String loanId, double netExposureAmount, double exposurePercentage, 
                                 boolean largeExposure, boolean exceedsLimit) {
            this.loanId = loanId;
            this.netExposureAmount = netExposureAmount;
            this.exposurePercentage = exposurePercentage;
            this.largeExposure = largeExposure;
            this.exceedsLimit = exceedsLimit;
        }

        // Getters
        public String getLoanId() { return loanId; }
        public double getNetExposureAmount() { return netExposureAmount; }
        public double getExposurePercentage() { return exposurePercentage; }
        public boolean isLargeExposure() { return largeExposure; }
        public boolean exceedsLimit() { return exceedsLimit; }
    }
}

