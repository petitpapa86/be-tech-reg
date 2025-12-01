package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.integration.RiskCalculationEventPublisher;
import com.bcbs239.regtech.riskcalculation.application.monitoring.PerformanceMetrics;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExposureValuation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for calculating risk metrics.
 * Orchestrates the risk calculation workflow using the new domain-driven architecture.
 * <p>
 * Workflow:
 * 1. Download exposure data from S3/local filesystem
 * 2. Parse JSON and create ExposureRecording objects
 * 3. Save exposures to database
 * 4. Convert to EUR using exchange rates
 * 5. Classify exposures by region and sector
 * 6. Calculate concentration indices (HHI)
 * 7. Generate portfolio analysis
 * 8. Persist results
 * 9. Publish completion event
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateRiskMetricsCommandHandler {

    private final ExposureRepository exposureRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final IFileStorageService fileStorageService;
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

            // Step 2: Parse JSON and create ExposureRecording objects
            log.info("Parsing exposure data from JSON");
            List<ExposureRecording> exposures = parseExposuresFromJson(jsonContent);

            if (exposures.isEmpty()) {
                log.warn("No exposures found in JSON file for batch: {}", command.getBatchId());
                return Result.failure(ErrorDetail.of("NO_EXPOSURES", ErrorType.BUSINESS_RULE_ERROR,
                        "No exposures found in JSON file", "calculation.no.exposures"));
            }

            log.info("Parsed {} exposures from JSON", exposures.size());

            // Step 3: Save exposures to database
            log.info("Saving {} exposures to database", exposures.size());
            exposureRepository.saveAll(exposures, command.getBatchId());
            log.info("Successfully saved {} exposures to database", exposures.size());

            // Step 4 & 5: Convert to EUR and classify exposures
            ExposureClassifier classifier = new ExposureClassifier();
            List<ClassifiedExposure> classifiedExposures = exposures.stream()
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

                        // Classify by region and sector
                        GeographicRegion region = classifier.classifyRegion(
                                exposure.classification().countryCode()
                        );
                        EconomicSector sector = classifier.classifySector(
                                exposure.classification().productType()
                        );

                        return ClassifiedExposure.of(
                                exposure.id(),
                                eurAmount,
                                region,
                                sector
                        );
                    })
                    .collect(Collectors.toList());

            log.info("Classified {} exposures", classifiedExposures.size());

            // Step 6 & 7: Analyze portfolio (calculates concentration indices and breakdowns)
            PortfolioAnalysis analysis = PortfolioAnalysis.analyze(
                    command.getBatchId(),
                    classifiedExposures
            );

            log.info("Portfolio analysis completed - Geographic HHI: {}, Sector HHI: {}",
                    analysis.getGeographicHHI().value(), analysis.getSectorHHI().value());

            // Step 8: Persist results
            portfolioAnalysisRepository.save(analysis);

            log.info("Risk metrics calculation completed successfully for batch: {}",
                    batchId);

            // Step 9: Publish success event
            eventPublisher.publishBatchCalculationCompleted(
                    batchId,
                    command.getBankId(),
                    classifiedExposures.size()
            );

            // Record batch success with exposure count
            performanceMetrics.recordBatchSuccess(batchId, classifiedExposures.size());

            return Result.success();

        } catch (Exception e) {
            log.error("Risk metrics calculation failed for batch: {}", batchId, e);

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
     * Parses exposure data from JSON content.
     * Expected JSON structure:
     * {
     * "bank_info": {...},
     * "exposures": [
     * {
     * "exposure_id": "...",
     * "instrument_id": "...",
     * "instrument_type": "LOAN|BOND|DERIVATIVE",
     * "counterparty_name": "...",
     * "counterparty_id": "...",
     * "counterparty_lei": "...",
     * "exposure_amount": 123.45,
     * "currency": "EUR",
     * "product_type": "...",
     * "balance_sheet_type": "ON_BALANCE|OFF_BALANCE",
     * "country_code": "IT"
     * }
     * ]
     * }
     *
     * @param jsonContent The JSON content containing exposure data
     * @return List of ExposureRecording objects
     */
    private List<ExposureRecording> parseExposuresFromJson(String jsonContent) throws Exception {
        List<ExposureRecording> exposures = new ArrayList<>();

        JsonNode root = objectMapper.readTree(jsonContent);
        JsonNode exposuresNode = root.get("exposures");

        if (exposuresNode == null || !exposuresNode.isArray()) {
            log.warn("No 'exposures' array found in JSON");
            return exposures;
        }

        for (JsonNode exposureNode : exposuresNode) {
            try {
                ExposureRecording exposure = parseExposureFromNode(exposureNode);
                if (exposure != null) {
                    exposures.add(exposure);
                }
            } catch (Exception e) {
                log.error("Failed to parse exposure from JSON node: {}", exposureNode, e);
                // Continue parsing other exposures
            }
        }

        return exposures;
    }

    /**
     * Parses a single exposure from a JSON node.
     */
    private ExposureRecording parseExposureFromNode(JsonNode exposureNode) {
        // Parse exposure fields
        String exposureId = exposureNode.get("exposure_id").asText();
        String instrumentId = exposureNode.get("instrument_id").asText();
        String instrumentTypeStr = exposureNode.get("instrument_type").asText();
        String counterpartyName = exposureNode.get("counterparty_name").asText();
        String counterpartyId = exposureNode.get("counterparty_id").asText();
        String counterpartyLei = exposureNode.has("counterparty_lei") ?
                exposureNode.get("counterparty_lei").asText() : "";
        double exposureAmount = exposureNode.get("exposure_amount").asDouble();
        String currency = exposureNode.get("currency").asText();
        String productType = exposureNode.get("product_type").asText();
        String balanceSheetTypeStr = exposureNode.get("balance_sheet_type").asText();
        String countryCode = exposureNode.get("country_code").asText();

        // Create domain objects
        var exposureIdObj = com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId.of(exposureId);
        var instrumentIdObj = com.bcbs239.regtech.riskcalculation.domain.exposure.InstrumentId.of(instrumentId);
        var counterparty = com.bcbs239.regtech.riskcalculation.domain.exposure.CounterpartyRef.of(
                counterpartyId, counterpartyName, counterpartyLei);
        var monetaryAmount = com.bcbs239.regtech.riskcalculation.domain.exposure.MonetaryAmount.of(
                java.math.BigDecimal.valueOf(exposureAmount), currency);

        // Parse enums
        var instrumentType = com.bcbs239.regtech.riskcalculation.domain.exposure.InstrumentType.valueOf(instrumentTypeStr);
        var balanceSheetType = com.bcbs239.regtech.riskcalculation.domain.exposure.BalanceSheetType.valueOf(balanceSheetTypeStr);

        var classification = com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureClassification.of(
                productType,
                instrumentType,
                balanceSheetType,
                countryCode
        );

        // Create ExposureRecording
        return ExposureRecording.create(
                exposureIdObj,
                instrumentIdObj,
                counterparty,
                monetaryAmount,
                classification
        );
    }
}
