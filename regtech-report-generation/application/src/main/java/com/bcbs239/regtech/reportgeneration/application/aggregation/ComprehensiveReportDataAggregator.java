package com.bcbs239.regtech.reportgeneration.application.aggregation;

import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculatedExposure;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.ConcentrationIndices;
import com.bcbs239.regtech.reportgeneration.domain.generation.GeographicBreakdown;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.SectorBreakdown;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.AmountEur;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ProcessingTimestamps;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.QualityDimension;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportingDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive Report Data Aggregator
 * 
 * Fetches and aggregates calculation and quality data from S3 (production) or
 * local filesystem (development) for comprehensive report generation.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 5.2
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComprehensiveReportDataAggregator {
    
    private final CoreS3Service coreS3Service;
    private final FilePathResolver filePathResolver;
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
            QualityResults qualityResults = fetchQualityData(qualityEvent);
            
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
     * Fetch calculation data from storage
     * 
     * Requirements: 3.1, 3.3
     */
    public CalculationResults fetchCalculationData(CalculationEventData event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Fetching calculation data for batch: {}", event.getBatchId());
            
            String filePath = filePathResolver.resolveCalculationPath(event.getBatchId());
            String jsonContent = fetchFileContent(filePath);
            
            // Parse JSON and map to domain object
            CalculationResults results = mapCalculationJson(jsonContent, event);
            
            log.debug("Successfully fetched calculation data for batch: {}", event.getBatchId());
            
            meterRegistry.counter("report.data.calculation.fetch.success").increment();
            
            return results;
            
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
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Fetching quality data for batch: {}", event.getBatchId());
            
            String filePath = filePathResolver.resolveQualityPath(event.getBatchId());
            String jsonContent = fetchFileContent(filePath);
            
            // Parse JSON and map to domain object
            QualityResults results = mapQualityJson(jsonContent, event);
            
            log.debug("Successfully fetched quality data for batch: {}", event.getBatchId());
            
            meterRegistry.counter("report.data.quality.fetch.success").increment();
            
            return results;
            
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
     * Fetch file content from S3 or local filesystem
     * 
     * Requirements: 3.5, 3.6, 3.7
     */
    public String fetchFileContent(String filePath) throws IOException {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            if (filePath.startsWith("s3://")) {
                return fetchFromS3(filePath);
            } else {
                return fetchFromLocalFilesystem(filePath);
            }
        } finally {
            sample.stop(Timer.builder("report.data.file.fetch.duration")
                .tag("storage_type", filePath.startsWith("s3://") ? "s3" : "local")
                .register(meterRegistry));
        }
    }
    
    /**
     * Fetch file content from S3
     */
    private String fetchFromS3(String s3Uri) throws IOException {
        log.debug("Fetching file from S3: {}", s3Uri);
        
        try {
            FilePathResolver.S3Location location = FilePathResolver.parseS3Uri(s3Uri);
            
            // Set timeout for S3 operation
            long startTime = System.currentTimeMillis();
            
            ResponseInputStream<GetObjectResponse> response = 
                coreS3Service.getObjectStream(location.bucket(), location.key());
            
            // Check timeout
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > FILE_DOWNLOAD_TIMEOUT_MS) {
                throw new IOException("S3 download timeout exceeded: " + elapsed + "ms");
            }
            
            String content = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            
            log.debug("Successfully fetched {} bytes from S3: {}", content.length(), s3Uri);
            
            meterRegistry.counter("report.data.s3.fetch.success").increment();
            meterRegistry.summary("report.data.s3.fetch.bytes").record(content.length());
            
            return content;
            
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: {}", s3Uri);
            meterRegistry.counter("report.data.s3.fetch.not_found").increment();
            throw new IOException("File not found in S3: " + s3Uri, e);
            
        } catch (S3Exception e) {
            log.error("S3 error fetching file: {}", s3Uri, e);
            meterRegistry.counter("report.data.s3.fetch.error",
                "error_code", String.valueOf(e.statusCode())).increment();
            throw new IOException("S3 error fetching file: " + s3Uri, e);
        }
    }
    
    /**
     * Fetch file content from local filesystem
     */
    private String fetchFromLocalFilesystem(String filePath) throws IOException {
        log.debug("Fetching file from local filesystem: {}", filePath);
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                log.error("File not found on local filesystem: {}", filePath);
                meterRegistry.counter("report.data.local.fetch.not_found").increment();
                throw new IOException("File not found: " + filePath);
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            
            log.debug("Successfully fetched {} bytes from local filesystem: {}", 
                content.length(), filePath);
            
            meterRegistry.counter("report.data.local.fetch.success").increment();
            meterRegistry.summary("report.data.local.fetch.bytes").record(content.length());
            
            return content;
            
        } catch (IOException e) {
            log.error("Error reading file from local filesystem: {}", filePath, e);
            meterRegistry.counter("report.data.local.fetch.error").increment();
            throw e;
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
            
            // Extract basic fields
            String batchId = root.path("batchId").asText();
            String bankId = root.path("bankId").asText();
            String bankName = root.path("bankName").asText();
            String reportingDateStr = root.path("reportingDate").asText();
            
            // Parse reporting date
            LocalDate reportingDate = LocalDate.parse(reportingDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            
            // Extract financial data
            BigDecimal tierOneCapital = new BigDecimal(root.path("tierOneCapital").asText());
            int totalExposures = root.path("totalExposures").asInt();
            BigDecimal totalAmount = new BigDecimal(root.path("totalAmount").asText());
            int limitBreaches = root.path("limitBreaches").asInt();
            
            // Extract exposures
            List<CalculatedExposure> exposures = mapExposures(root.path("exposures"));
            
            // Extract breakdowns
            GeographicBreakdown geographicBreakdown = mapGeographicBreakdown(root.path("geographicBreakdown"));
            SectorBreakdown sectorBreakdown = mapSectorBreakdown(root.path("sectorBreakdown"));
            ConcentrationIndices concentrationIndices = mapConcentrationIndices(root.path("concentrationIndices"));
            
            // Extract timestamps
            ProcessingTimestamps timestamps = mapProcessingTimestamps(root.path("processingTimestamps"));
            
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
    
    /**
     * Map quality JSON to domain object
     */
    private QualityResults mapQualityJson(String jsonContent, QualityEventData event) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            
            // Extract basic fields
            String batchId = root.path("batchId").asText();
            String bankId = root.path("bankId").asText();
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
                batchErrorsNode.forEach(node -> batchErrors.add(node));
            }
            
            // Extract exposure results
            List<QualityResults.ExposureResult> exposureResults = mapExposureResults(root.path("exposureResults"));
            
            return new QualityResults(
                BatchId.of(batchId),
                BankId.of(bankId),
                timestamp,
                totalExposures,
                validExposures,
                totalErrors,
                dimensionScores,
                batchErrors,
                exposureResults
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
                boolean valid = exposureNode.path("valid").asBoolean();
                
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
}
