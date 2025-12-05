package com.bcbs239.regtech.app.integration;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.ingestion.domain.model.BankInfoModel;
import com.bcbs239.regtech.ingestion.domain.model.CreditRiskMitigation;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IngestionToRiskCalculationIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPreserveDataThroughIngestionToRiskCalculationFlow() throws Exception {
        BankInfoModel bankInfo = new BankInfoModel("Community First Bank", "08081", "815600D7623147C25D86", "2024-09-12", 2);
        LoanExposure exposure1 = new LoanExposure("LOAN001", "EXP_001_2024", "Mike's Pizza Inc", "CORP12345", "549300ABCDEF1234567890", 250000.0, 250000.0, 240000.0, "EUR", "Business Loan", "Retail", "ON_BALANCE", "Italy", "IT");
        CreditRiskMitigation mitigation1 = new CreditRiskMitigation("EXP_001_2024", "FINANCIAL_COLLATERAL", BigDecimal.valueOf(10000.00), "EUR");
        ParsedFileData parsedFileData = new ParsedFileData(bankInfo, List.of(exposure1), List.of(mitigation1), Map.of("source", "test"));
        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        assertNotNull(batchDataDTO);
        String json = objectMapper.writeValueAsString(batchDataDTO);
        assertTrue(json.contains("bank_info"));
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        assertNotNull(deserializedDTO);
        List<ExposureRecording> exposureRecordings = deserializedDTO.exposures().stream().map(ExposureRecording::fromDTO).toList();
        assertEquals(1, exposureRecordings.size());
        ExposureRecording recording1 = exposureRecordings.get(0);
        assertEquals("EXP_001_2024", recording1.id().value());
        assertEquals("LOAN001", recording1.instrumentId().value());
    }
}
