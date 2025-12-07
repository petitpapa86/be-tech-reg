package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.riskcalculation.application.integration.RiskCalculationEventPublisher;
import com.bcbs239.regtech.riskcalculation.application.monitoring.PerformanceMetrics;
import com.bcbs239.regtech.riskcalculation.application.storage.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.protection.Mitigation;
import com.bcbs239.regtech.riskcalculation.domain.protection.MitigationType;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command handler for calculating risk metrics.
 * Orchestrates the risk calculation workflow using the file-first architecture.
 * <p>
 * Workflow:
 * 1. Download exposure data from S3/local filesystem
 * 2. Parse JSON and create ExposureRecording objects
 * 3. Create batch record
 * 4. Convert to EUR using exchange rates
 * 5. Classify exposures by region and sector
 * 6. Apply mitigations to create ProtectedExposure objects
 * 7. Calculate concentration indices (HHI)
 * 8. Generate portfolio analysis
 * 9. Store complete results to JSON file
 * 10. Update batch metadata with results URI
 * 11. Persist optional portfolio summary
 * 12. Publish completion event
 * 
 * Requirements: 1.3, 1.5, 2.3, 2.4, 2.5
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateRiskMetricsCommandHandler {

    private final ExposureRepository exposureRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository batchRepository;
    private final IFileStorageService fileStorageService;
    private final ICalculationResultsStorageService calculationResultsStorageService;
    private final ExchangeRateProvider exchangeRateProvider;
    private final RiskCalculationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final PerformanceMetrics performanceMetrics;

    /**
     * Handles the risk metrics calculation command.
     *
     * @param command The calculation command
     * @return Result indicating success or failure
     */
    @Transactional
    public Result<Void> handle(CalculateRiskMetricsCommand command) {
        String batchId = command.getBatchId();

        // Record batch start
        performanceMetrics.recordBatchStart(batchId);

        log.info("Starting risk metrics calculation for batch: {} from bank: {}",
                batchId, command.getBankId());

        try {
            // Step 1: Download exposure data from S3/local filesystem
            log.info("Downloading exposure data from: {}", command.getS3Uri());
            Result<String> downloadResult = fileStorageService.retrieveFile(command.getS3Uri());

            if (downloadResult.isFailure()) {
                ErrorDetail error = downloadResult.getError().orElse(
                        ErrorDetail.of("FILE_DOWNLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                                "Failed to download exposure data", "calculation.file.download.failed")
                );
                return Result.failure(error);
            }

            String jsonContent = downloadResult.getValue().orElse("");
            log.info("Downloaded exposure data, size: {} bytes", jsonContent.length());

            // Step 2: Parse JSON and create ExposureRecording objects using BatchDataDTO
            log.info("Deserializing batch data from JSON to BatchDataDTO");
            List<ExposureRecording> exposures;
            
            try {
                exposures = parseExposuresFromJson(jsonContent);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize batch data for batch: {}", batchId, e);
                return Result.failure(ErrorDetail.of(
                    "DESERIALIZATION_FAILED", 
                    ErrorType.SYSTEM_ERROR,
                    "Failed to deserialize batch data from JSON: " + e.getMessage(), 
                    "calculation.deserialization.failed"
                ));
            }

            if (exposures.isEmpty()) {
                log.warn("No exposures found in JSON file for batch: {}", command.getBatchId());
                return Result.failure(ErrorDetail.of("NO_EXPOSURES", ErrorType.BUSINESS_RULE_ERROR,
                        "No exposures found in JSON file", "calculation.no.exposures"));
            }

            log.info("Successfully deserialized and converted {} exposures from BatchDataDTO", exposures.size());

            // Step 3: Create batch record (required before saving exposures due to FK constraint)
            log.info("Creating batch record for batch: {}", command.getBatchId());
            try {
                BatchDataDTO batchData = objectMapper.readValue(jsonContent, BatchDataDTO.class);
                BankInfo bankInfo = batchData.bankInfo() != null 
                    ? BankInfo.fromDTO(batchData.bankInfo())
                    : BankInfo.of("Unknown", "00000", "UNKNOWN");
                
                LocalDate reportDate = batchData.bankInfo() != null 
                    ? batchData.bankInfo().reportDate() 
                    : java.time.LocalDate.now();
                
                batchRepository.createBatch(
                    command.getBatchId(),
                    bankInfo,
                    reportDate,
                    exposures.size(),
                    java.time.Instant.now()
                );
                log.info("Successfully created batch record for batch: {}", command.getBatchId());
            } catch (Exception e) {
                log.error("Failed to create batch record for batch: {}", command.getBatchId(), e);
                return Result.failure(ErrorDetail.of(
                    "BATCH_CREATION_FAILED", 
                    ErrorType.SYSTEM_ERROR,
                    "Failed to create batch record: " + e.getMessage(), 
                    "calculation.batch.creation.failed"
                ));
            }

            // Step 4: Convert to EUR, classify exposures, and apply mitigations
            log.info("Converting {} exposures to EUR and applying mitigations", exposures.size());
            ExposureClassifier classifier = new ExposureClassifier();
            
            // Parse mitigations from JSON (map of exposureId -> list of mitigations)
            java.util.Map<String, List<com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO>> mitigationsByExposure = parseMitigationsFromJson(jsonContent);
            log.info("Parsed mitigations for {} exposures from JSON", mitigationsByExposure.size());
            
            // Create ProtectedExposure objects with mitigations applied
            List<ProtectedExposure> protectedExposures = exposures.stream()
                    .map(exposure -> {
                        // Convert to EUR using exchange rate provider
                        EurAmount eurAmount;
                        try {
                            String currency = exposure.exposureAmount().currencyCode();
                            if ("EUR".equals(currency)) {
                                eurAmount = EurAmount.of(exposure.exposureAmount().amount());
                            } else {
                                var rate = exchangeRateProvider.getRate(currency, "EUR");
                                BigDecimal eurValue = exposure.exposureAmount().amount().multiply(rate.rate());
                                eurAmount = EurAmount.of(eurValue);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to convert exposure {} to EUR, using zero", exposure.id());
                            eurAmount = EurAmount.zero();
                        }

                        // Find mitigations for this exposure
                        List<Mitigation> exposureMitigations = new ArrayList<>();
                        List<com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO> exposureMitigationDTOs = 
                                mitigationsByExposure.getOrDefault(exposure.id().value(), List.of());
                        
                        for (com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO mitigationDTO : exposureMitigationDTOs) {
                            try {
                                // Convert mitigation to EUR
                                EurAmount mitigationEur;
                                if ("EUR".equals(mitigationDTO.currency())) {
                                    mitigationEur = EurAmount.of(mitigationDTO.value());
                                } else {
                                    var rate = exchangeRateProvider.getRate(mitigationDTO.currency(), "EUR");
                                    BigDecimal eurValue = mitigationDTO.value().multiply(rate.rate());
                                    mitigationEur = EurAmount.of(eurValue);
                                }
                                
                                // Parse mitigation type
                                MitigationType mitigationType = MitigationType.valueOf(mitigationDTO.mitigationType());
                                exposureMitigations.add(Mitigation.reconstitute(mitigationType, mitigationEur));
                            } catch (Exception e) {
                                log.warn("Failed to convert mitigation to EUR for exposure {}, skipping", exposure.id(), e);
                            }
                        }

                        // Create ProtectedExposure with mitigations
                        return ProtectedExposure.calculate(
                                ExposureId.of(exposure.id().value()),
                                eurAmount,
                                exposureMitigations
                        );
                    })
                    .collect(Collectors.toList());

            log.info("Created {} protected exposures with mitigations applied", protectedExposures.size());

            // Step 5: Classify exposures for portfolio analysis
            List<ClassifiedExposure> classifiedExposures = exposures.stream()
                    .map(exposure -> {
                        // Find corresponding protected exposure to get EUR amount
                        ProtectedExposure protectedExposure = protectedExposures.stream()
                                .filter(pe -> pe.getExposureId().value().equals(exposure.id().value()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Protected exposure not found"));

                        // Classify by region and sector
                        GeographicRegion region = classifier.classifyRegion(
                                exposure.classification().countryCode()
                        );
                        EconomicSector sector = classifier.classifySector(
                                exposure.classification().productType()
                        );

                        return ClassifiedExposure.of(
                                exposure.id(),
                                protectedExposure.getNetExposure(),
                                region,
                                sector
                        );
                    })
                    .collect(Collectors.toList());

            log.info("Classified {} exposures", classifiedExposures.size());

            // Step 6: Analyze portfolio (calculates concentration indices and breakdowns)
            PortfolioAnalysis analysis = PortfolioAnalysis.analyze(
                    command.getBatchId(),
                    classifiedExposures
            );

            log.info("Portfolio analysis completed - Geographic HHI: {}, Sector HHI: {}",
                    analysis.getGeographicHHI().value(), analysis.getSectorHHI().value());

            // Step 7: Build RiskCalculationResult
            BatchDataDTO batchData = objectMapper.readValue(jsonContent, BatchDataDTO.class);
            BankInfo bankInfo = batchData.bankInfo() != null 
                ? BankInfo.fromDTO(batchData.bankInfo())
                : BankInfo.of("Unknown", "00000", "UNKNOWN");
            
            RiskCalculationResult calculationResult = new RiskCalculationResult(
                    command.getBatchId(),
                    bankInfo,
                    protectedExposures,
                    analysis,
                    java.time.Instant.now()
            );

            // Step 8: Store calculation results to JSON file
            log.info("Storing calculation results to file storage for batch: {}", batchId);
            Result<String> storageResult = calculationResultsStorageService.storeCalculationResults(calculationResult);
            
            if (storageResult.isFailure()) {
                ErrorDetail error = storageResult.getError().orElse(
                        ErrorDetail.of("STORAGE_FAILED", ErrorType.SYSTEM_ERROR,
                                "Failed to store calculation results", "calculation.storage.failed")
                );
                log.error("Failed to store calculation results for batch: {}", batchId);
                
                // Mark batch as failed
                batchRepository.updateStatus(batchId, "FAILED");
                
                // Publish failure event
                eventPublisher.publishBatchCalculationFailed(
                        batchId,
                        command.getBankId(),
                        "Failed to store calculation results: " + error.getMessage()
                );
                
                return Result.failure(error);
            }

            String resultsUri = storageResult.getValue().orElseThrow();
            log.info("Successfully stored calculation results at: {}", resultsUri);

            // Step 9: Update batch metadata with results URI and mark as completed
            // Requirement 2.5: Update batch status to COMPLETED and store calculation_results_uri
            batchRepository.markAsCompleted(command.getBatchId(), resultsUri, java.time.Instant.now());
            log.info("Marked batch as completed with results URI: {}", resultsUri);

            // Step 10: Persist optional portfolio analysis summary
            portfolioAnalysisRepository.save(analysis);
            log.info("Persisted portfolio analysis summary");

            log.info("Risk metrics calculation completed successfully for batch: {}",
                    batchId);

            // Step 11: Publish success event
            eventPublisher.publishBatchCalculationCompleted(
                    batchId,
                    command.getBankId(),
                    protectedExposures.size()
            );

            // Record batch success with exposure count
            performanceMetrics.recordBatchSuccess(batchId, protectedExposures.size());

            return Result.success();

        } catch (Exception e) {
            log.error("Risk metrics calculation failed for batch: {}", batchId, e);

            // Mark batch as failed
            try {
                batchRepository.updateStatus(batchId, "FAILED");
            } catch (Exception ex) {
                log.error("Failed to update batch status to FAILED for batch: {}", batchId, ex);
            }

            // Record batch failure with error message
            performanceMetrics.recordBatchFailure(batchId, e.getMessage());

            // Publish failure event
            eventPublisher.publishBatchCalculationFailed(
                    batchId,
                    command.getBankId(),
                    e.getMessage()
            );

            return Result.failure(ErrorDetail.of("CALCULATION_FAILED", ErrorType.SYSTEM_ERROR,
                    "Risk calculation failed: " + e.getMessage(), "calculation.failed"));
        }
    }

    /**
     * Parses exposure data from JSON content using BatchDataDTO.
     * Expected JSON structure:
     * {
     *   "bank_info": {
     *     "bank_name": "...",
     *     "abi_code": "...",
     *     "lei_code": "...",
     *     "report_date": "2024-09-12",
     *     "total_exposures": 8
     *   },
     *   "exposures": [
     *     {
     *       "exposure_id": "...",
     *       "instrument_id": "...",
     *       "instrument_type": "LOAN|BOND|DERIVATIVE",
     *       "counterparty_name": "...",
     *       "counterparty_id": "...",
     *       "counterparty_lei": "...",
     *       "exposure_amount": 123.45,
     *       "currency": "EUR",
     *       "product_type": "...",
     *       "balance_sheet_type": "ON_BALANCE|OFF_BALANCE",
     *       "country_code": "IT"
     *     }
     *   ],
     *   "credit_risk_mitigation": [...]
     * }
     *
     * @param jsonContent The JSON content containing batch data
     * @return List of ExposureRecording objects
     * @throws JsonProcessingException if JSON deserialization fails
     */
    private List<ExposureRecording> parseExposuresFromJson(String jsonContent) throws JsonProcessingException {
        List<ExposureRecording> exposures = new ArrayList<>();

        try {
            // Deserialize JSON to BatchDataDTO
            BatchDataDTO batchData = objectMapper.readValue(jsonContent, BatchDataDTO.class);
            
            if (batchData == null) {
                log.error("Failed to deserialize JSON to BatchDataDTO: result is null");
                throw new JsonProcessingException("Deserialization resulted in null BatchDataDTO") {};
            }
            
            // Log bank information if available
            if (batchData.bankInfo() != null) {
                BankInfo bankInfo = BankInfo.fromDTO(batchData.bankInfo());
                log.info("Processing batch from bank: {} (ABI: {}, LEI: {})", 
                    bankInfo.bankName(), bankInfo.abiCode(), bankInfo.leiCode());
            } else {
                log.warn("No bank_info found in batch data");
            }
            
            // Check if exposures list exists
            if (batchData.exposures() == null || batchData.exposures().isEmpty()) {
                log.warn("No exposures found in BatchDataDTO");
                return exposures;
            }
            
            log.info("Deserializing {} exposures from BatchDataDTO", batchData.exposures().size());
            
            // Convert each ExposureDTO to ExposureRecording using fromDTO
            for (ExposureDTO exposureDTO : batchData.exposures()) {
                try {
                    ExposureRecording exposure = ExposureRecording.fromDTO(exposureDTO);
                    exposures.add(exposure);
                } catch (Exception e) {
                    log.error("Failed to convert ExposureDTO to ExposureRecording [exposureId:{}]: {}", 
                        exposureDTO.exposureId(), e.getMessage(), e);
                    // Continue processing other exposures
                }
            }
            
            log.info("Successfully converted {} exposures from DTOs", exposures.size());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to BatchDataDTO: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing exposures from JSON: {}", e.getMessage(), e);
            throw new JsonProcessingException("Unexpected error during parsing: " + e.getMessage(), e) {};
        }

        return exposures;
    }

    /**
     * Parses mitigation data from JSON content using BatchDataDTO.
     * Returns a map of exposureId -> list of mitigations for that exposure.
     * 
     * @param jsonContent The JSON content containing batch data
     * @return Map of exposure ID to list of mitigation DTOs
     * @throws JsonProcessingException if JSON deserialization fails
     */
    private Map<String, List<com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO>> parseMitigationsFromJson(String jsonContent) throws JsonProcessingException {
        try {
            // Deserialize JSON to BatchDataDTO
            BatchDataDTO batchData = objectMapper.readValue(jsonContent, BatchDataDTO.class);
            
            if (batchData == null || batchData.creditRiskMitigation() == null) {
                log.info("No credit risk mitigation data found in batch");
                return Map.of();
            }
            
            log.info("Deserializing {} mitigations from BatchDataDTO", batchData.creditRiskMitigation().size());
            
            // Group mitigations by exposure ID
            Map<String, List<com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO>> mitigationsByExposure = 
                    batchData.creditRiskMitigation().stream()
                            .collect(Collectors.groupingBy(com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO::exposureId));
            
            log.info("Successfully grouped mitigations for {} exposures", mitigationsByExposure.size());
            
            return mitigationsByExposure;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to BatchDataDTO for mitigations: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing mitigations from JSON: {}", e.getMessage(), e);
            throw new JsonProcessingException("Unexpected error during mitigation parsing: " + e.getMessage(), e) {};
        }
    }
}
