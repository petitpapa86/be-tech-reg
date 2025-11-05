package com.bcbs239.regtech.modules.ingestion.infrastructure;

import com.bcbs239.regtech.ingestion.domain.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enhanced loan JSON parsing unit tests")
class EnhancedLoanParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should parse enhanced loan JSON and verify key mappings and collateral entries")
    void shouldParseEnhancedLoanJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        JsonNode root = objectMapper.readTree(resource.getInputStream());

        // bank_info
        JsonNode bankInfo = root.get("bank_info");
        assertThat(bankInfo).as("bank_info node").isNotNull();
        assertThat(bankInfo.get("bank_name").asText()).isEqualTo("Community First Bank");
        assertThat(bankInfo.get("abi_code").asText()).isEqualTo("08081");
        assertThat(bankInfo.get("lei_code").asText()).isEqualTo("815600D7623147C25D86");
        assertThat(bankInfo.get("total_loans").asInt()).isEqualTo(5);

        // loan_portfolio
        JsonNode loanPortfolio = root.get("loan_portfolio");
        assertThat(loanPortfolio).as("loan_portfolio").isNotNull();
        assertThat(loanPortfolio.isArray()).isTrue();
        assertThat(loanPortfolio.size()).isEqualTo(5);

        // Map loan_portfolio to domain DTOs
        LoanExposureDto[] exposures =
            objectMapper.treeToValue(loanPortfolio, LoanExposureDto[].class);

        assertThat(exposures).hasSize(5);

        // Map to domain objects
        java.util.List<LoanExposure> exposureModels =
            DomainMapper.toLoanExposureList(exposures);

        assertThat(exposureModels).hasSize(5);

        LoanExposure loan001 = exposureModels.stream()
            .filter(e -> "LOAN001".equals(e.loanId()))
            .findFirst().orElse(null);

        assertThat(loan001).isNotNull();
        assertThat(loan001.exposureId()).isEqualTo("EXP_LOAN001_2024");
        assertThat(loan001.netExposureAmount()).isEqualTo(240000.0);
        assertThat(loan001.currency()).isEqualTo("EUR");

        // Map credit risk mitigation
        JsonNode crm = root.get("credit_risk_mitigation");
        assertThat(crm).as("credit_risk_mitigation").isNotNull();
        assertThat(crm.isArray()).isTrue();
        assertThat(crm.size()).isEqualTo(3);

        CreditRiskMitigationDto[] crms =
            objectMapper.treeToValue(crm, CreditRiskMitigationDto[].class);

        assertThat(crms).hasSize(3);

        java.util.List<CreditRiskMitigation> crmModels =
            DomainMapper.toCrmList(crms);

        CreditRiskMitigation collForExp1 = crmModels.stream()
            .filter(c -> "EXP_LOAN001_2024".equals(c.exposureId()))
            .findFirst().orElse(null);

        assertThat(collForExp1).isNotNull();
        assertThat(collForExp1.collateralValue()).isEqualTo(10000.0);
        assertThat(collForExp1.collateralCurrency()).isEqualTo("EUR");

        // Verify loans without counterparty_lei are allowed (empty string) - LOAN002 and LOAN005
        LoanExposure loan002Model =
            exposureModels.stream().filter(e -> "LOAN002".equals(e.loanId())).findFirst().orElse(null);
        LoanExposure loan005Model =
            exposureModels.stream().filter(e -> "LOAN005".equals(e.loanId())).findFirst().orElse(null);

        assertThat(loan002Model).isNotNull();
        assertThat(loan002Model.counterpartyLei()).isEmpty();
        assertThat(loan005Model).isNotNull();
        assertThat(loan005Model.counterpartyLei()).isEmpty();

        // Countries present
        assertThat(exposureModels.stream().map(LoanExposure::borrowerCountry).toList())
            .contains("IT", "DE", "CA");
    }
}

