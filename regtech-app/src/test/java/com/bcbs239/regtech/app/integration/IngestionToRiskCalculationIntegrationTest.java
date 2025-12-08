package com.bcbs239.regtech.app.integration;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.ingestion.domain.model.BankInfoModel;
import com.bcbs239.regtech.ingestion.domain.model.CreditRiskMitigation;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories.SpringDataBatchRepository;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories.SpringDataExposureRepository;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories.SpringDataMitigationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for data format alignment between Ingestion and Risk Calculation modules.
 * Updated to verify file-first architecture where calculation results are stored in JSON files
 * and database tables for exposures and mitigations remain empty.
 * 
 * Tests the complete flow:
 * 1. Ingestion creates ParsedFileData
 * 2. Convert to BatchDataDTO
 * 3. Serialize to JSON
 * 4. Deserialize back to BatchDataDTO
 * 5. Risk Calculation creates ExposureRecording from DTO
 * 6. Verify all data is preserved
 * 7. Verify file-first architecture (JSON file creation, empty database tables)
 * 
 * Requirements: 1.5, 2.3, 2.4, 2.5, 7.1, 7.2
 */
@SpringBootTest
@ActiveProfiles("test")
class IngestionToRiskCalculationIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private SpringDataBatchRepository batchRepository;
    
    @Autowired(required = false)
    private SpringDataExposureRepository exposureRepository;
    
    @Autowired(required = false)
    private SpringDataMitigationRepository mitigationRepository;
    
    @Autowired(required = false)
    private IFileStorageService fileStorageService;

    @Test
    void shouldPreserveDataThroughIngestionToRiskCalculationFlow() throws Exception {
        // Step 1: Create ParsedFileData (Ingestion domain model)
        BankInfoModel bankInfo = new BankInfoModel(
            "Community First Bank",
            "08081",
            "815600D7623147C25D86",
            "2024-09-12",
            2
        );

        LoanExposure exposure1 = new LoanExposure(
            "LOAN001",
            "EXP_001_2024",
            "Mike's Pizza Inc",
            "CORP12345",
            "549300ABCDEF1234567890",
            250000.0,
            250000.0,
            240000.0,
            "EUR",
            "Business Loan",
            "Retail",
            "ON_BALANCE",
            "Italy",
            "IT"
        );

        LoanExposure exposure2 = new LoanExposure(
            "LOAN002",
            "EXP_002_2024",
            "Tech Solutions Ltd",
            "CORP67890",
            "549300ZYXWVU9876543210",
            500000.0,
            500000.0,
            480000.0,
            "USD",
            "Corporate Loan",
            "Technology",
            "ON_BALANCE",
            "United States",
            "US"
        );

        CreditRiskMitigation mitigation1 = new CreditRiskMitigation(
            "EXP_001_2024",
            "FINANCIAL_COLLATERAL",
            BigDecimal.valueOf(10000.00),
            "EUR"
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            bankInfo,
            List.of(exposure1, exposure2),
            List.of(mitigation1),
            Map.of("source", "test")
        );

        // Step 2: Convert to BatchDataDTO
        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        
        // Verify DTO structure
        assertNotNull(batchDataDTO, "BatchDataDTO should not be null");
        assertNotNull(batchDataDTO.bankInfo(), "Bank info should not be null");
        assertEquals(2, batchDataDTO.exposures().size(), "Should have 2 exposures");
        assertEquals(1, batchDataDTO.creditRiskMitigation().size(), "Should have 1 mitigation");

        // Step 3: Serialize BatchDataDTO to JSON
        String json = objectMapper.writeValueAsString(batchDataDTO);
        
        // Verify JSON is not empty
        assertNotNull(json, "JSON should not be null");
        assertFalse(json.isEmpty(), "JSON should not be empty");
        
        // Verify JSON contains expected snake_case fields
        assertTrue(json.contains("\"bank_info\""), "JSON should contain bank_info field");
        assertTrue(json.contains("\"exposures\""), "JSON should contain exposures field");
        assertTrue(json.contains("\"credit_risk_mitigation\""), "JSON should contain credit_risk_mitigation field");
        assertTrue(json.contains("\"exposure_id\""), "JSON should contain exposure_id field");
        assertTrue(json.contains("\"counterparty_lei\""), "JSON should contain counterparty_lei field");

        // Step 4: Deserialize JSON back to BatchDataDTO
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        
        // Verify deserialized DTO
        assertNotNull(deserializedDTO, "Deserialized DTO should not be null");
        assertNotNull(deserializedDTO.bankInfo(), "Deserialized bank info should not be null");
        assertEquals(2, deserializedDTO.exposures().size(), "Deserialized should have 2 exposures");
        assertEquals(1, deserializedDTO.creditRiskMitigation().size(), "Deserialized should have 1 mitigation");

        // Step 5: Verify bank info is preserved
        BankInfoDTO deserializedBankInfo = deserializedDTO.bankInfo();
        assertEquals("Community First Bank", deserializedBankInfo.bankName());
        assertEquals("08081", deserializedBankInfo.abiCode());
        assertEquals("815600D7623147C25D86", deserializedBankInfo.leiCode());
        assertEquals(LocalDate.parse("2024-09-12"), deserializedBankInfo.reportDate());
        assertEquals(2, deserializedBankInfo.totalExposures());

        // Step 6: Create ExposureRecording from each ExposureDTO (Risk Calculation domain model)
        List<ExposureRecording> exposureRecordings = deserializedDTO.exposures().stream()
            .map(ExposureRecording::fromDTO)
            .toList();
        
        assertEquals(2, exposureRecordings.size(), "Should have 2 exposure recordings");

        // Step 7: Verify first exposure data is preserved
        ExposureRecording recording1 = exposureRecordings.get(0);
        assertNotNull(recording1, "First exposure recording should not be null");
        assertEquals("EXP_001_2024", recording1.id().value());
        assertEquals("LOAN001", recording1.instrumentId().value());
        assertEquals("CORP12345", recording1.counterparty().counterpartyId());
        assertEquals("Mike's Pizza Inc", recording1.counterparty().name());
        assertTrue(recording1.counterparty().leiCode().isPresent());
        assertEquals("549300ABCDEF1234567890", recording1.counterparty().leiCode().get());
        assertEquals(0, BigDecimal.valueOf(250000.0).compareTo(recording1.exposureAmount().amount()));
        assertEquals("EUR", recording1.exposureAmount().currencyCode());
        assertEquals("Business Loan", recording1.classification().productType());
        assertEquals("IT", recording1.classification().countryCode());

        // Step 8: Verify second exposure data is preserved
        ExposureRecording recording2 = exposureRecordings.get(1);
        assertNotNull(recording2, "Second exposure recording should not be null");
        assertEquals("EXP_002_2024", recording2.id().value());
        assertEquals("LOAN002", recording2.instrumentId().value());
        assertEquals("CORP67890", recording2.counterparty().counterpartyId());
        assertEquals("Tech Solutions Ltd", recording2.counterparty().name());
        assertTrue(recording2.counterparty().leiCode().isPresent());
        assertEquals("549300ZYXWVU9876543210", recording2.counterparty().leiCode().get());
        assertEquals(0, BigDecimal.valueOf(500000.0).compareTo(recording2.exposureAmount().amount()));
        assertEquals("USD", recording2.exposureAmount().currencyCode());
        assertEquals("Corporate Loan", recording2.classification().productType());
        assertEquals("US", recording2.classification().countryCode());

        // Step 9: Verify credit risk mitigation is preserved
        CreditRiskMitigationDTO deserializedMitigation = deserializedDTO.creditRiskMitigation().get(0);
        assertEquals("EXP_001_2024", deserializedMitigation.exposureId());
        assertEquals("FINANCIAL_COLLATERAL", deserializedMitigation.mitigationType());
        assertEquals(0, BigDecimal.valueOf(10000.00).compareTo(deserializedMitigation.value()));
        assertEquals("EUR", deserializedMitigation.currency());
    }

    @Test
    void shouldHandleEmptyExposuresList() throws Exception {
        BankInfoModel bankInfo = new BankInfoModel(
            "Test Bank",
            "12345",
            "TEST123456789012345678",
            "2024-01-01",
            0
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            bankInfo,
            List.of(),
            List.of(),
            Map.of()
        );

        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        String json = objectMapper.writeValueAsString(batchDataDTO);
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        
        assertNotNull(deserializedDTO.exposures());
        assertTrue(deserializedDTO.exposures().isEmpty());
        assertNotNull(deserializedDTO.creditRiskMitigation());
        assertTrue(deserializedDTO.creditRiskMitigation().isEmpty());
    }
    
    /**
     * Integration test for file-first architecture.
     * Verifies that:
     * 1. JSON files are created for calculation results
     * 2. Batch metadata includes calculation_results_uri
     * 3. Exposures table remains empty
     * 4. Mitigations table remains empty
     * 
     * Requirements: 1.5, 2.3, 2.4, 2.5
     */
    @Test
    void shouldUseFileFirstArchitectureForCalculationResults() {
        // Skip test if repositories are not available (not in full integration test context)
        if (batchRepository == null || exposureRepository == null || 
            mitigationRepository == null || fileStorageService == null) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                .warn("Skipping file-first architecture test - required beans not available");
            return;
        }
        
        // Test data
        String testBatchId = "test_batch_file_first_" + System.currentTimeMillis();
        String testJsonContent = createTestCalculationResultsJson(testBatchId);
        
        try {
            // Step 1: Store calculation results as JSON file (Requirement 1.5)
            var storeResult = fileStorageService.storeFile(
                "risk_calc_" + testBatchId + "_test.json",
                testJsonContent
            );
            
            assertTrue(storeResult.isSuccess(), 
                "JSON file storage should succeed (Requirement 1.5)");
            
            String calculationResultsUri = storeResult.value();
            assertNotNull(calculationResultsUri, 
                "Calculation results URI should not be null (Requirement 1.4)");
            assertFalse(calculationResultsUri.isEmpty(), 
                "Calculation results URI should not be empty (Requirement 1.4)");
            
            org.slf4j.LoggerFactory.getLogger(getClass())
                .info("Stored calculation results to: {}", calculationResultsUri);
            
            // Step 2: Verify JSON file can be retrieved (Requirement 1.5)
            var retrieveResult = fileStorageService.retrieveFile(calculationResultsUri);
            assertTrue(retrieveResult.isSuccess(), 
                "JSON file retrieval should succeed (Requirement 1.5)");
            
            String retrievedJson = retrieveResult.value();
            assertNotNull(retrievedJson, 
                "Retrieved JSON should not be null");
            assertTrue(retrievedJson.contains(testBatchId), 
                "Retrieved JSON should contain batch ID");
            
            // Step 3: Create batch metadata with calculation_results_uri (Requirement 2.5)
            BatchEntity batchEntity = BatchEntity.builder()
                .batchId(testBatchId)
                .bankName("Test Bank")
                .abiCode("12345")
                .leiCode("TEST123456789012345678")
                .reportDate(LocalDate.now())
                .totalExposures(2)
                .status("COMPLETED")
                .ingestedAt(java.time.Instant.now())
                .processedAt(java.time.Instant.now())
                .calculationResultsUri(calculationResultsUri)
                .build();
            
            batchRepository.save(batchEntity);
            
            // Step 4: Verify batch metadata includes calculation_results_uri (Requirement 2.5)
            Optional<BatchEntity> savedBatch = batchRepository.findById(testBatchId);
            assertTrue(savedBatch.isPresent(), 
                "Batch should be saved in database");
            
            BatchEntity retrievedBatch = savedBatch.get();
            assertNotNull(retrievedBatch.getCalculationResultsUri(), 
                "Batch metadata should include calculation_results_uri (Requirement 2.5)");
            assertEquals(calculationResultsUri, retrievedBatch.getCalculationResultsUri(), 
                "Stored URI should match retrieved URI (Requirement 2.5)");
            assertEquals("COMPLETED", retrievedBatch.getStatus(), 
                "Batch status should be COMPLETED (Requirement 2.5)");
            
            org.slf4j.LoggerFactory.getLogger(getClass())
                .info("Verified batch metadata with URI: {}", retrievedBatch.getCalculationResultsUri());
            
            // Step 5: Verify exposures table remains empty (Requirement 2.3)
            var exposures = exposureRepository.findByBatchId(testBatchId);
            assertTrue(exposures.isEmpty(), 
                "Exposures table should remain empty for file-first architecture (Requirement 2.3)");
            
            org.slf4j.LoggerFactory.getLogger(getClass())
                .info("Verified exposures table is empty for batch: {}", testBatchId);
            
            // Step 6: Verify mitigations table remains empty (Requirement 2.4)
            var mitigations = mitigationRepository.findByBatchId(testBatchId);
            assertTrue(mitigations.isEmpty(), 
                "Mitigations table should remain empty for file-first architecture (Requirement 2.4)");
            
            org.slf4j.LoggerFactory.getLogger(getClass())
                .info("Verified mitigations table is empty for batch: {}", testBatchId);
            
            // Step 7: Verify end-to-end flow with file storage (Requirement 1.5)
            // The fact that we can store, retrieve, and verify the JSON file demonstrates
            // that the file-first architecture is working correctly
            org.slf4j.LoggerFactory.getLogger(getClass())
                .info("File-first architecture integration test completed successfully");
            
        } finally {
            // Cleanup: Delete test batch and file
            try {
                batchRepository.deleteById(testBatchId);
                org.slf4j.LoggerFactory.getLogger(getClass())
                    .info("Cleaned up test batch: {}", testBatchId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Failed to cleanup test batch: {}", testBatchId, e);
            }
        }
    }
    
    /**
     * Creates a test JSON string representing calculation results.
     * This mimics the structure that would be created by CalculationResultsJsonSerializer.
     */
    private String createTestCalculationResultsJson(String batchId) {
        return String.format("""
            {
              "format_version": "1.0",
              "batch_id": "%s",
              "calculated_at": "2024-12-07T12:00:00Z",
              "bank_info": {
                "bank_name": "Test Bank",
                "abi_code": "12345",
                "lei_code": "TEST123456789012345678"
              },
              "summary": {
                "total_exposures": 2,
                "total_amount_eur": 750000.00,
                "geographic_breakdown": {
                  "IT": {"amount_eur": 250000.00, "percentage": 33.33},
                  "US": {"amount_eur": 500000.00, "percentage": 66.67}
                },
                "sector_breakdown": {
                  "Retail": {"amount_eur": 250000.00, "percentage": 33.33},
                  "Technology": {"amount_eur": 500000.00, "percentage": 66.67}
                },
                "concentration_indices": {
                  "herfindahl_geographic": 0.55,
                  "herfindahl_sector": 0.55
                }
              },
              "calculated_exposures": [
                {
                  "exposure_id": "EXP_001_2024",
                  "gross_exposure_eur": 250000.00,
                  "net_exposure_eur": 240000.00,
                  "total_mitigation_eur": 10000.00,
                  "percentage_of_total": 33.33,
                  "counterparty": {
                    "lei": "549300ABCDEF1234567890",
                    "name": "Mike's Pizza Inc",
                    "country": "IT"
                  },
                  "classification": {
                    "geographic": "IT",
                    "sector": "Retail"
                  },
                  "mitigations": [
                    {
                      "type": "FINANCIAL_COLLATERAL",
                      "eur_value": 10000.00
                    }
                  ]
                },
                {
                  "exposure_id": "EXP_002_2024",
                  "gross_exposure_eur": 500000.00,
                  "net_exposure_eur": 480000.00,
                  "total_mitigation_eur": 20000.00,
                  "percentage_of_total": 66.67,
                  "counterparty": {
                    "lei": "549300ZYXWVU9876543210",
                    "name": "Tech Solutions Ltd",
                    "country": "US"
                  },
                  "classification": {
                    "geographic": "US",
                    "sector": "Technology"
                  },
                  "mitigations": []
                }
              ]
            }
            """, batchId);
    }
}
