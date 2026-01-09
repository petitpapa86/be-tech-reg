package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.core.domain.recommendations.QualityInsight;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationSeverity;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.IStorageService;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.domain.generation.*;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Comprehensive Report Data Aggregator
 * 
 * Fetches and aggregates calculation and quality data from storage using shared IStorageService.
 * Eliminates duplicate file I/O logic by delegating to regtech-core storage abstraction.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 5.2
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComprehensiveReportDataAggregator {
    
    private final IStorageService storageService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    private static final long FILE_DOWNLOAD_TIMEOUT_MS = 30000; // 30 seconds
    
    /**
     * Fetch all data required for comprehensive report generation
     * 
     * Requirements: 3.1, 3.2, 5.2
     */
    public ComprehensiveReportData fetchAllData(
            CalculationEventData calculationEvent,
            QualityEventData qualityEvent) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.info("Fetching comprehensive report data for batch: {}", calculationEvent.getBatchId());
            
            // Fetch both data sources
            CalculationResults calculationResults = fetchCalculationData(calculationEvent);
            QualityResults qualityResults = fetchQualityData(qualityEvent, calculationEvent.getBankId());
            
            // Validate data consistency
            validateDataConsistency(calculationResults, qualityResults);
            
            // Build comprehensive data
            ComprehensiveReportData reportData = ComprehensiveReportData.builder()
                .batchId(calculationEvent.getBatchId())
                .bankId(calculationEvent.getBankId())
                .bankName(calculationResults.bankName())
                .reportingDate(calculationResults.reportingDate().value())
                .calculationResults(calculationResults)
                .qualityResults(qualityResults)
                .build();
            
            reportData.validate();
            
            log.info("Successfully fetched comprehensive report data for batch: {}", 
                calculationEvent.getBatchId());
            
            meterRegistry.counter("report.data.aggregation.success").increment();
            
            return reportData;
            
        } catch (Exception e) {
            log.error("Failed to fetch comprehensive report data for batch: {}", 
                calculationEvent.getBatchId(), e);
            
            meterRegistry.counter("report.data.aggregation.failure",
                "failure_reason", e.getClass().getSimpleName()).increment();
            
            throw new DataAggregationException(
                "Failed to fetch comprehensive report data for batch: " + calculationEvent.getBatchId(),
                e
            );
            
        } finally {
            sample.stop(Timer.builder("report.data.aggregation.duration")
                .register(meterRegistry));
        }
    }
    
    /**
     * Fetch calculation data from storage using shared IStorageService
     * 
     * Requirements: 3.1, 3.3
     */
    public CalculationResults fetchCalculationData(CalculationEventData event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Fetching calculation data for batch: {}", event.getBatchId());
            
            // Parse URI using shared StorageUri
            StorageUri uri = StorageUri.parse(event.getResultFileUri());
            
            // Download using shared storage service
            Result<String> downloadResult = storageService.download(uri);
            if (downloadResult.isFailure()) {
                throw new DataAggregationException(
                    "Failed to download calculation data from storage: " + 
                    downloadResult.getError().orElseThrow().getMessage()
                );
            }
            
            String jsonContent = downloadResult.getValueOrThrow();
            
            // Parse JSON and map to domain object
            CalculationResults results = mapCalculationJson(jsonContent, event);
            
            log.debug("Successfully fetched calculation data for batch: {}", event.getBatchId());
            
            meterRegistry.counter("report.data.calculation.fetch.success").increment();
            
            return results;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid calculation data URI for batch: {}", event.getBatchId(), e);
            
            meterRegistry.counter("report.data.calculation.fetch.failure",
                "failure_reason", "InvalidURI").increment();
            
            throw new DataAggregationException(
                "Invalid storage URI for batch: " + event.getBatchId(),
                e
            );
            
        } catch (Exception e) {
            log.error("Failed to fetch calculation data for batch: {}", event.getBatchId(), e);
            
            meterRegistry.counter("report.data.calculation.fetch.failure",
                "failure_reason", e.getClass().getSimpleName()).increment();
            
            throw new DataAggregationException(
                "Failed to fetch calculation data for batch: " + event.getBatchId(),
                e
            );
            
        } finally {
            sample.stop(Timer.builder("report.data.calculation.fetch.duration")
                .register(meterRegistry));
        }
    }
    
    /**
     * Fetch quality data from storage
     * 
     * Requirements: 3.2, 3.4
     */
    public QualityResults fetchQualityData(QualityEventData event) {
        return fetchQualityData(event, null);
    }

    private QualityResults fetchQualityData(QualityEventData event, String canonicalBankId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Fetching quality data for batch: {}", event.getBatchId());
            
            // Parse URI using shared StorageUri
            StorageUri uri = StorageUri.parse(event.getResultFileUri());
            
            // Download using shared storage service
            Result<String> downloadResult = storageService.download(uri);
            if (downloadResult.isFailure()) {
                throw new DataAggregationException(
                    "Failed to download quality data from storage: " + 
                    downloadResult.getError().orElseThrow().getMessage()
                );
            }
            
            String jsonContent = downloadResult.getValueOrThrow();
            
            // Parse JSON and map to domain object
            QualityResults results = mapQualityJson(jsonContent, event, canonicalBankId);
            
            log.debug("Successfully fetched quality data for batch: {}", event.getBatchId());
            
            meterRegistry.counter("report.data.quality.fetch.success").increment();
            
            return results;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid quality data URI for batch: {}", event.getBatchId(), e);
            
            meterRegistry.counter("report.data.quality.fetch.failure",
                "failure_reason", "InvalidURI").increment();
            
            throw new DataAggregationException(
                "Invalid storage URI for batch: " + event.getBatchId(),
                e
            );
            
        } catch (Exception e) {
            log.error("Failed to fetch quality data for batch: {}", event.getBatchId(), e);
            
            meterRegistry.counter("report.data.quality.fetch.failure",
                "failure_reason", e.getClass().getSimpleName()).increment();
            
            throw new DataAggregationException(
                "Failed to fetch quality data for batch: " + event.getBatchId(),
                e
            );
            
        } finally {
            sample.stop(Timer.builder("report.data.quality.fetch.duration")
                .register(meterRegistry));
        }
    }
    
    /**
     * Validate data consistency between calculation and quality results
     * 
     * Requirements: 4.2, 4.3, 4.4
     */
    public void validateDataConsistency(
            CalculationResults calculationResults,
            QualityResults qualityResults) {
        
        log.debug("Validating data consistency for batch: {}", 
            calculationResults.batchId().value());
        
        // Validate batch ID consistency
        if (!calculationResults.batchId().value().equals(
                qualityResults.getBatchId().value())) {
            throw new DataAggregationException(
                String.format("Batch ID mismatch: calculation=%s, quality=%s",
                    calculationResults.batchId().value(),
                    qualityResults.getBatchId().value())
            );
        }
        
        // Validate bank ID consistency
        if (!calculationResults.bankId().value().equals(
                qualityResults.getBankId().value())) {
            throw new DataAggregationException(
                String.format("Bank ID mismatch: calculation=%s, quality=%s",
                    calculationResults.bankId().value(),
                    qualityResults.getBankId().value())
            );
        }
        
        // Validate exposure count consistency (with tolerance for filtering)
        int calcExposures = calculationResults.totalExposures();
        int qualityExposures = qualityResults.getTotalExposures();
        
        if (Math.abs(calcExposures - qualityExposures) > 10) {
            log.warn("Exposure count mismatch (tolerance exceeded): calculation={}, quality={}",
                calcExposures, qualityExposures);
            // Don't throw exception, just log warning as some filtering may occur
        }
        
        log.debug("Data consistency validation passed for batch: {}", 
            calculationResults.batchId().value());
    }
    
    /**
     * Map calculation JSON to domain object
     */
    private CalculationResults mapCalculationJson(String jsonContent, CalculationEventData event) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            // Detect risk-calculation v1 schema (snake_case)
            boolean isRiskSchema = root.hasNonNull("format_version") || root.hasNonNull("batch_id") || root.hasNonNull("calculated_at");

            String batchId;
            String bankId;
            String bankName;
            LocalDate reportingDate;
            BigDecimal tierOneCapital;
            int totalExposures;
            BigDecimal totalAmount;
            int limitBreaches;
            List<CalculatedExposure> exposures;
            GeographicBreakdown geographicBreakdown;
            SectorBreakdown sectorBreakdown;
            ConcentrationIndices concentrationIndices;
            ProcessingTimestamps timestamps;

            if (isRiskSchema) {
                batchId = firstNonBlank(root.path("batch_id").asText(), event.getBatchId());

                JsonNode bankInfo = root.path("bank_info");
                bankId = firstNonBlank(event.getBankId(), bankInfo.path("abi_code").asText());
                bankName = firstNonBlank(bankInfo.path("bank_name").asText(), "Unknown Bank");

                Instant calculatedAt = parseInstantRequired(root.path("calculated_at").asText(), "calculated_at");
                reportingDate = calculatedAt.atZone(ZoneOffset.UTC).toLocalDate();

                // Not present in current risk output; keep non-null for domain invariants
                tierOneCapital = safeBigDecimal(bankInfo.path("tier_one_capital"), BigDecimal.ZERO);

                JsonNode summary = root.path("summary");
                totalExposures = summary.path("total_exposures").asInt(root.path("calculated_exposures").isArray() ? root.path("calculated_exposures").size() : 0);
                totalAmount = safeBigDecimal(summary.path("total_amount_eur"), BigDecimal.ZERO);
                limitBreaches = summary.path("limit_breaches").asInt(0);

                exposures = mapExposuresFromRisk(root, tierOneCapital, totalAmount);

                // Prefer explicit summary value, but fall back to computed breaches when missing
                if (limitBreaches <= 0 && !exposures.isEmpty()) {
                    limitBreaches = (int) exposures.stream().filter(e -> !e.compliant()).count();
                }

                // If total amount is missing, fall back to sum of exposures
                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0 && !exposures.isEmpty()) {
                    totalAmount = exposures.stream()
                        .map(CalculatedExposure::amountEur)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                // If total exposures is missing/zero, fall back to mapped exposures size
                if (totalExposures <= 0 && !exposures.isEmpty()) {
                    totalExposures = exposures.size();
                }

                geographicBreakdown = mapGeographicBreakdownFromRisk(summary.path("geographic_breakdown"));
                sectorBreakdown = mapSectorBreakdownFromRisk(summary.path("sector_breakdown"));
                concentrationIndices = mapConcentrationIndicesFromRisk(summary.path("concentration_indices"));
                timestamps = new ProcessingTimestamps(calculatedAt, null, null, calculatedAt, null);
            } else {
                // Legacy (camelCase) schema
                batchId = firstNonBlank(root.path("batchId").asText(), event.getBatchId());
                bankId = firstNonBlank(root.path("bankId").asText(), event.getBankId());
                bankName = root.path("bankName").asText();

                String reportingDateStr = root.path("reportingDate").asText();
                reportingDate = parseLocalDateRequired(reportingDateStr, "reportingDate");

                tierOneCapital = safeBigDecimal(root.path("tierOneCapital"), null);
                totalExposures = root.path("totalExposures").asInt();
                totalAmount = safeBigDecimal(root.path("totalAmount"), null);
                limitBreaches = root.path("limitBreaches").asInt();

                exposures = mapExposures(root.path("exposures"));
                geographicBreakdown = mapGeographicBreakdown(root.path("geographicBreakdown"));
                sectorBreakdown = mapSectorBreakdown(root.path("sectorBreakdown"));
                concentrationIndices = mapConcentrationIndices(root.path("concentrationIndices"));
                timestamps = mapProcessingTimestamps(root.path("processingTimestamps"));
            }
            
            return new CalculationResults(
                BatchId.of(batchId),
                BankId.of(bankId),
                bankName,
                ReportingDate.of(reportingDate),
                AmountEur.of(tierOneCapital),
                totalExposures,
                AmountEur.of(totalAmount),
                limitBreaches,
                exposures,
                geographicBreakdown,
                sectorBreakdown,
                concentrationIndices,
                timestamps
            );
            
        } catch (Exception e) {
            log.error("Failed to parse calculation JSON", e);
            throw new DataAggregationException("Failed to parse calculation JSON", e);
        }
    }

    private List<CalculatedExposure> mapExposuresFromRisk(JsonNode root, BigDecimal tierOneCapital, BigDecimal totalAmountEur) {
        JsonNode exposuresNode = root.path("calculated_exposures");
        if (!exposuresNode.isArray()) {
            exposuresNode = root.path("exposures");
        }

        if (!exposuresNode.isArray()) {
            return List.of();
        }

        BigDecimal capital = tierOneCapital == null ? BigDecimal.ZERO : tierOneCapital;
        BigDecimal total = totalAmountEur == null ? BigDecimal.ZERO : totalAmountEur;

        List<CalculatedExposure> exposures = new ArrayList<>();
        exposuresNode.forEach(node -> {
            String exposureId = firstNonBlank(node.path("exposure_id").asText(), node.path("exposureId").asText());

            String counterpartyName = firstNonBlank(
                node.path("counterparty_name").asText(),
                node.path("client_name").asText(),
                node.path("counterparty_ref").asText(),
                exposureId.isBlank() ? "Unknown" : ("Exposure " + exposureId)
            );

            Optional<String> leiCode = Optional.ofNullable(firstNonBlank(node.path("lei_code").asText(), node.path("leiCode").asText()))
                .filter(s -> !s.isBlank());

            String identifierType;
            if (leiCode.isPresent()) {
                identifierType = "LEI";
            } else if (!firstNonBlank(node.path("counterparty_ref").asText(), node.path("counterpartyRef").asText()).isBlank()) {
                identifierType = "REF";
            } else if (!exposureId.isBlank()) {
                identifierType = "EXPOSURE_ID";
            } else {
                identifierType = "NAME";
            }

            String countryCode = firstNonBlank(
                node.path("country").asText(),
                node.path("country_code").asText(),
                node.path("countryCode").asText(),
                "N/A"
            );

            String sectorCode = firstNonBlank(
                node.path("economic_sector").asText(),
                node.path("sector").asText(),
                node.path("sector_category").asText(),
                node.path("sectorCode").asText(),
                "other"
            );

            Optional<String> rating = Optional.ofNullable(firstNonBlank(node.path("rating").asText(), node.path("credit_rating").asText()))
                .filter(s -> !s.isBlank());

            BigDecimal originalAmount = safeBigDecimal(firstNumberNode(node,
                "original_amount",
                "originalAmount",
                "gross_exposure_eur",
                "grossExposureEur",
                "net_exposure_eur",
                "netExposureEur",
                "eur_amount",
                "amount_eur"
            ), BigDecimal.ZERO);

            String originalCurrency = firstNonBlank(node.path("original_currency").asText(), node.path("originalCurrency").asText(), "EUR");

            BigDecimal amountEur = safeBigDecimal(firstNumberNode(node,
                "amount_eur",
                "eur_amount",
                "net_exposure_eur",
                "gross_exposure_eur",
                "amountEur",
                "netExposureEur",
                "grossExposureEur"
            ), BigDecimal.ZERO);

            BigDecimal amountAfterCrm = safeBigDecimal(firstNumberNode(node,
                "amount_after_crm_eur",
                "amountAfterCrm",
                "mitigated_amount_eur",
                "mitigatedAmountEur",
                "net_exposure_eur",
                "netExposureEur",
                "amount_eur",
                "eur_amount"
            ), amountEur);

            BigDecimal pctCapital;
            if (capital.compareTo(BigDecimal.ZERO) > 0) {
                pctCapital = safePercent(amountAfterCrm, capital);
            } else if (total.compareTo(BigDecimal.ZERO) > 0) {
                // best-effort fallback when eligible capital is not present in the risk JSON
                pctCapital = safePercent(amountAfterCrm, total);
            } else {
                pctCapital = BigDecimal.ZERO;
            }

            boolean compliant = pctCapital.compareTo(new BigDecimal("25")) <= 0;

            exposures.add(new CalculatedExposure(
                counterpartyName,
                leiCode,
                identifierType,
                countryCode,
                sectorCode,
                rating,
                originalAmount,
                originalCurrency,
                amountEur,
                amountAfterCrm,
                BigDecimal.ZERO,
                amountAfterCrm,
                pctCapital,
                compliant
            ));
        });

        return exposures;
    }

    private static JsonNode firstNumberNode(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }
        for (String field : fieldNames) {
            if (field == null || field.isBlank()) {
                continue;
            }
            JsonNode v = node.path(field);
            if (!v.isMissingNode() && !v.isNull() && !(v.isTextual() && v.asText().isBlank())) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) {
            return BigDecimal.ZERO;
        }
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator
            .multiply(BigDecimal.valueOf(100))
            .divide(denominator, 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Map quality JSON to domain object
     */
    private QualityResults mapQualityJson(String jsonContent, QualityEventData event, String canonicalBankId) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            
            // Extract basic fields
            String batchId = root.path("batchId").asText();
            if (batchId == null || batchId.isBlank()) {
                batchId = event.getBatchId();
            }

            String bankId = root.path("bankId").asText();
            if (bankId == null || bankId.isBlank()) {
                bankId = (canonicalBankId != null && !canonicalBankId.isBlank()) ? canonicalBankId : event.getBankId();
            }
            String timestampStr = root.path("timestamp").asText();
            Instant timestamp = Instant.parse(timestampStr);
            
            int totalExposures = root.path("totalExposures").asInt();
            int validExposures = root.path("validExposures").asInt();
            int totalErrors = root.path("totalErrors").asInt();
            
            // Extract dimension scores
            Map<QualityDimension, BigDecimal> dimensionScores = mapDimensionScores(root.path("dimensionScores"));
            
            // Extract batch errors
            List<Object> batchErrors = new ArrayList<>();
            JsonNode batchErrorsNode = root.path("batchErrors");
            if (batchErrorsNode.isArray()) {
                batchErrorsNode.forEach(batchErrors::add);
            }
            
            // Extract exposure results
            List<QualityResults.ExposureResult> exposureResults = mapExposureResults(root.path("exposureResults"));
            
            // Extract recommendations (if present)
            List<QualityInsight> recommendations = mapRecommendations(root.path("recommendations"));
            
            return new QualityResults(
                BatchId.of(batchId),
                BankId.of(bankId),
                timestamp,
                totalExposures,
                validExposures,
                totalErrors,
                dimensionScores,
                batchErrors,
                exposureResults,
                recommendations
            );
            
        } catch (Exception e) {
            log.error("Failed to parse quality JSON", e);
            throw new DataAggregationException("Failed to parse quality JSON", e);
        }
    }
    
    // Helper mapping methods
    
    private List<CalculatedExposure> mapExposures(JsonNode exposuresNode) {
        List<CalculatedExposure> exposures = new ArrayList<>();
        
        if (exposuresNode.isArray()) {
            exposuresNode.forEach(node -> {
                exposures.add(new CalculatedExposure(
                    node.path("counterpartyName").asText(),
                    Optional.ofNullable(node.path("leiCode").asText("")).filter(s -> !s.isEmpty()),
                    node.path("identifierType").asText("CONCAT"),
                    node.path("countryCode").asText(),
                    node.path("sectorCode").asText(),
                    Optional.ofNullable(node.path("rating").asText("")).filter(s -> !s.isEmpty()),
                    new BigDecimal(node.path("originalAmount").asText("0")),
                    node.path("originalCurrency").asText("EUR"),
                    new BigDecimal(node.path("amountEur").asText()),
                    new BigDecimal(node.path("amountAfterCrm").asText("0")),
                    new BigDecimal(node.path("tradingBookPortion").asText("0")),
                    new BigDecimal(node.path("nonTradingBookPortion").asText("0")),
                    new BigDecimal(node.path("percentageOfCapital").asText()),
                    node.path("compliant").asBoolean()
                ));
            });
        }
        
        return exposures;
    }
    
    private GeographicBreakdown mapGeographicBreakdown(JsonNode node) {
        return new GeographicBreakdown(
            AmountEur.of(new BigDecimal(node.path("italyAmount").asText("0"))),
            new BigDecimal(node.path("italyPercentage").asText("0")),
            node.path("italyCount").asInt(0),
            AmountEur.of(new BigDecimal(node.path("euOtherAmount").asText("0"))),
            new BigDecimal(node.path("euOtherPercentage").asText("0")),
            node.path("euOtherCount").asInt(0),
            AmountEur.of(new BigDecimal(node.path("nonEuropeanAmount").asText("0"))),
            new BigDecimal(node.path("nonEuropeanPercentage").asText("0")),
            node.path("nonEuropeanCount").asInt(0)
        );
    }
    
    private SectorBreakdown mapSectorBreakdown(JsonNode node) {
        return new SectorBreakdown(
            AmountEur.of(new BigDecimal(node.path("retailMortgageAmount").asText("0"))),
            new BigDecimal(node.path("retailMortgagePercentage").asText("0")),
            node.path("retailMortgageCount").asInt(0),
            AmountEur.of(new BigDecimal(node.path("sovereignAmount").asText("0"))),
            new BigDecimal(node.path("sovereignPercentage").asText("0")),
            node.path("sovereignCount").asInt(0),
            AmountEur.of(new BigDecimal(node.path("corporateAmount").asText("0"))),
            new BigDecimal(node.path("corporatePercentage").asText("0")),
            node.path("corporateCount").asInt(0),
            AmountEur.of(new BigDecimal(node.path("bankingAmount").asText("0"))),
            new BigDecimal(node.path("bankingPercentage").asText("0")),
            node.path("bankingCount").asInt(0),
            AmountEur.of(new BigDecimal(node.path("otherAmount").asText("0"))),
            new BigDecimal(node.path("otherPercentage").asText("0")),
            node.path("otherCount").asInt(0)
        );
    }
    
    private ConcentrationIndices mapConcentrationIndices(JsonNode node) {
        return new ConcentrationIndices(
            new BigDecimal(node.path("geographicHerfindahl").asText("0")),
            new BigDecimal(node.path("sectorHerfindahl").asText("0"))
        );
    }
    
    private ProcessingTimestamps mapProcessingTimestamps(JsonNode node) {
        return new ProcessingTimestamps(
            Instant.parse(node.path("startedAt").asText()),
            node.has("htmlCompletedAt") ? Instant.parse(node.path("htmlCompletedAt").asText()) : null,
            node.has("xbrlCompletedAt") ? Instant.parse(node.path("xbrlCompletedAt").asText()) : null,
            node.has("completedAt") ? Instant.parse(node.path("completedAt").asText()) : null,
            node.has("failedAt") ? Instant.parse(node.path("failedAt").asText()) : null
        );
    }
    
    private Map<QualityDimension, BigDecimal> mapDimensionScores(JsonNode node) {
        Map<QualityDimension, BigDecimal> scores = new HashMap<>();
        
        node.fields().forEachRemaining(entry -> {
            try {
                QualityDimension dimension = QualityDimension.valueOf(entry.getKey().toUpperCase());
                BigDecimal score = new BigDecimal(entry.getValue().asText());
                scores.put(dimension, score);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown quality dimension: {}", entry.getKey());
            }
        });
        
        return scores;
    }
    
    private List<QualityResults.ExposureResult> mapExposureResults(JsonNode node) {
        List<QualityResults.ExposureResult> results = new ArrayList<>();
        
        if (node.isArray()) {
            node.forEach(exposureNode -> {
                String exposureId = exposureNode.path("exposureId").asText();
                boolean valid = exposureNode.has("valid")
                        ? exposureNode.path("valid").asBoolean()
                        : exposureNode.path("isValid").asBoolean();
                
                List<QualityResults.ValidationError> errors = new ArrayList<>();
                JsonNode errorsNode = exposureNode.path("errors");
                if (errorsNode.isArray()) {
                    errorsNode.forEach(errorNode -> {
                        errors.add(new QualityResults.ValidationError(
                            errorNode.path("dimension").asText(),
                            errorNode.path("ruleCode").asText(),
                            errorNode.path("message").asText(),
                            errorNode.path("fieldName").asText(),
                            errorNode.path("severity").asText()
                        ));
                    });
                }
                
                results.add(new QualityResults.ExposureResult(exposureId, valid, errors));
            });
        }
        
        return results;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private static BigDecimal safeBigDecimal(JsonNode node, BigDecimal defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalArgumentException("Missing required numeric value");
        }

        if (node.isNumber()) {
            return node.decimalValue();
        }

        String text = node.asText();
        if (text == null || text.isBlank()) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalArgumentException("Missing required numeric value");
        }
        return new BigDecimal(text);
    }

    private static LocalDate parseLocalDateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required date field: " + fieldName);
        }

        // Allow ISO date or ISO instant/offset datetime (use first 10 chars)
        String normalized = value.trim();
        if (normalized.length() >= 10 && normalized.charAt(4) == '-' && normalized.charAt(7) == '-') {
            normalized = normalized.substring(0, 10);
        }

        return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static Instant parseInstantRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required timestamp field: " + fieldName);
        }
        return Instant.parse(value);
    }

    private GeographicBreakdown mapGeographicBreakdownFromRisk(JsonNode breakdownNode) {
        if (breakdownNode == null || breakdownNode.isMissingNode() || !breakdownNode.isObject()) {
            return mapGeographicBreakdown(objectMapper.createObjectNode());
        }

        JsonNode italy = breakdownNode.path("italy");
        JsonNode euOther = breakdownNode.path("eu_other");
        JsonNode nonEuropean = breakdownNode.path("non_european");

        return new GeographicBreakdown(
            AmountEur.of(safeBigDecimal(italy.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(italy.path("percentage"), BigDecimal.ZERO),
            0,
            AmountEur.of(safeBigDecimal(euOther.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(euOther.path("percentage"), BigDecimal.ZERO),
            0,
            AmountEur.of(safeBigDecimal(nonEuropean.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(nonEuropean.path("percentage"), BigDecimal.ZERO),
            0
        );
    }

    private SectorBreakdown mapSectorBreakdownFromRisk(JsonNode breakdownNode) {
        if (breakdownNode == null || breakdownNode.isMissingNode() || !breakdownNode.isObject()) {
            return mapSectorBreakdown(objectMapper.createObjectNode());
        }

        JsonNode retailMortgage = breakdownNode.path("retail_mortgage");
        JsonNode sovereign = breakdownNode.path("sovereign");
        JsonNode corporate = breakdownNode.path("corporate");
        JsonNode banking = breakdownNode.path("banking");
        JsonNode other = breakdownNode.path("other");

        return new SectorBreakdown(
            AmountEur.of(safeBigDecimal(retailMortgage.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(retailMortgage.path("percentage"), BigDecimal.ZERO),
            0,
            AmountEur.of(safeBigDecimal(sovereign.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(sovereign.path("percentage"), BigDecimal.ZERO),
            0,
            AmountEur.of(safeBigDecimal(corporate.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(corporate.path("percentage"), BigDecimal.ZERO),
            0,
            AmountEur.of(safeBigDecimal(banking.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(banking.path("percentage"), BigDecimal.ZERO),
            0,
            AmountEur.of(safeBigDecimal(other.path("amount_eur"), BigDecimal.ZERO)),
            safeBigDecimal(other.path("percentage"), BigDecimal.ZERO),
            0
        );
    }

    /**
     * Map recommendations JSON array to list of QualityInsight objects
     * Parses recommendations generated during data quality validation
     */
    private List<QualityInsight> mapRecommendations(JsonNode recommendationsNode) {
        List<QualityInsight> recommendations = new ArrayList<>();
        
        if (recommendationsNode == null || !recommendationsNode.isArray()) {
            log.debug("No recommendations found in quality data");
            return recommendations;
        }
        
        recommendationsNode.forEach(node -> {
            try {
                String ruleId = node.path("ruleId").asText();
                String severityStr = node.path("severity").asText();
                String message = node.path("message").asText();
                String localeStr = node.path("locale").asText("en-US");
                
                // Parse action items
                List<String> actionItems = new ArrayList<>();
                JsonNode actionItemsNode = node.path("actionItems");
                if (actionItemsNode.isArray()) {
                    actionItemsNode.forEach(item -> actionItems.add(item.asText()));
                }
                
                // Map severity string to enum
                RecommendationSeverity severity;
                try {
                    severity = RecommendationSeverity.valueOf(severityStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown recommendation severity: {}, defaulting to MEDIUM", severityStr);
                    severity = RecommendationSeverity.MEDIUM;
                }
                
                // Parse locale
                Locale locale;
                try {
                    locale = Locale.forLanguageTag(localeStr);
                } catch (Exception e) {
                    log.warn("Invalid locale: {}, defaulting to en-US", localeStr);
                    locale = Locale.US;
                }
                
                recommendations.add(new QualityInsight(
                    ruleId,
                    severity,
                    message,
                    actionItems,
                    locale
                ));
                
            } catch (Exception e) {
                log.error("Failed to parse recommendation: {}", node, e);
                // Continue parsing other recommendations
            }
        });
        
        log.debug("Parsed {} recommendations from quality data", recommendations.size());
        return recommendations;
    }
    
    private ConcentrationIndices mapConcentrationIndicesFromRisk(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isObject()) {
            return mapConcentrationIndices(objectMapper.createObjectNode());
        }

        return new ConcentrationIndices(
            safeBigDecimal(node.path("herfindahl_geographic"), BigDecimal.ZERO),
            safeBigDecimal(node.path("herfindahl_sector"), BigDecimal.ZERO)
        );
    }
}
