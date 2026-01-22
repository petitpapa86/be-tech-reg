package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.recommendations.QualityInsight;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationSeverity;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.IStorageService;
import com.bcbs239.regtech.core.domain.storage.StorageResult;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.reportgeneration.application.coordination.BatchEventTracker;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.IComprehensiveReportOrchestrator;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.domain.generation.*;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.micrometer.core.annotation.Timed;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import com.bcbs239.regtech.reportgeneration.infrastructure.util.XmlUtils;
import com.bcbs239.regtech.reportgeneration.infrastructure.storage.StorageMetadataBuilder;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Orchestrates the generation of comprehensive reports combining risk calculation
 * and quality validation results.
 * <p>
 * This service:
 * - Checks for existing reports (idempotency)
 * - Aggregates data from both calculation and quality sources
 * - Generates quality recommendations
 * - Generates HTML and XBRL reports in parallel
 * - Uploads reports to S3 using shared IStorageService
 * - Persists report metadata
 * - Handles failures with appropriate status updates
 * <p>
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 16.1, 16.2, 23.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveReportOrchestrator implements IComprehensiveReportOrchestrator {

    private final ComprehensiveReportDataAggregator dataAggregator;
    private final HtmlReportGenerator htmlGenerator;
    private final XbrlReportGenerator xbrlGenerator;
    private final IStorageService storageService;  // Changed from IReportStorageService
    private final IGeneratedReportRepository reportRepository;
    private final BatchEventTracker eventTracker;
    private final ReportGenerationMetrics metrics;
    private final BaseUnitOfWork unitOfWork;

    @Value("${report-generation.storage.type:local}")
    private String storageType;

    @Value("${report-generation.storage.local.base-path:./data/reports}")
    private String localBasePath;

    @Value("${storage.s3.bucket-name:bcbs239-reports}")
    private String s3BucketName;

    @Value("${storage.s3.report-prefix:reports/}")
    private String reportPrefix;

    /**
     * Builds a storage URI based on configured storage type.
     *
     * @param subPath The sub-path within the storage (e.g., "html/report.html")
     * @return StorageUri for S3 or local filesystem
     */
    private StorageUri buildStorageUri(String subPath) {
        if ("s3".equalsIgnoreCase(storageType)) {
            String s3Path = reportPrefix + subPath;
            return StorageUri.parse("s3://" + s3BucketName + "/" + s3Path);
        } else {
            // Local filesystem - build file:// URI
            java.nio.file.Path basePath = java.nio.file.Paths.get(localBasePath);
            java.nio.file.Path fullPath = basePath.resolve(subPath);
            String fileUri = fullPath.toUri().toString();
            return StorageUri.parse(fileUri);
        }
    }

    /**
     * Generates a comprehensive report asynchronously.
     * <p>
     * This method:
     * 1. Checks for existing COMPLETED reports (idempotency)
     * 2. Fetches and aggregates data from both sources
     * 3. Creates GeneratedReport aggregate
     * 4. Generates quality recommendations
     * 5. Generates HTML and XBRL in parallel
     * 6. Updates report with results
     * 7. Cleans up event tracker
     *
     * @param riskEventData    the risk calculation event data
     * @param qualityEventData the quality validation event data
     */
    @Async("reportGenerationExecutor")
    @Override
    @Timed(value = "reportgeneration.comprehensive", description = "Time taken to generate comprehensive report")
    public void generateComprehensiveReport(
            CalculationEventData riskEventData,
            QualityEventData qualityEventData) {

        long startTime = System.currentTimeMillis();
        String batchId = riskEventData.getBatchId();

        try {
            log.info("Starting comprehensive report generation [batchId:{}]", batchId);
            metrics.recordReportGenerationStart(batchId);

            // Step 1: Check for existing COMPLETED report (idempotency)
            if (reportRepository.existsByBatchIdAndStatus(BatchId.of(batchId), ReportStatus.COMPLETED)) {
                log.info("Report already exists with COMPLETED status [batchId:{}], skipping generation", batchId);
                metrics.recordDuplicateSkipped(batchId);
                return;
            }

            // Step 2: Fetch and aggregate data from both sources
            long dataFetchStart = System.currentTimeMillis();
            ComprehensiveReportData reportData = dataAggregator.fetchAllData(riskEventData, qualityEventData);
            long dataFetchDuration = System.currentTimeMillis() - dataFetchStart;
            metrics.recordDataFetchDuration(batchId, dataFetchDuration);

            log.info("Data aggregation completed [batchId:{},duration:{}ms]", batchId, dataFetchDuration);

            // Step 3: Create GeneratedReport aggregate
            GeneratedReport report = GeneratedReport.createComprehensiveReport(
                    BatchId.of(batchId),
                    BankId.of(reportData.getBankId()),
                    ReportingDate.of(reportData.getReportingDate()),
                    reportData.getQualityResults().getOverallScore(),
                    reportData.getQualityResults().getComplianceStatus()
            );

            // Save initial report with IN_PROGRESS status
            reportRepository.save(report);
            unitOfWork.registerEntity(report);
            unitOfWork.saveChanges();
            log.info("Created report aggregate [batchId:{},reportId:{}]", batchId, report.getReportId().value());

            // Step 4: Read pre-generated quality recommendations from storage
            // Recommendations are generated by data-quality module during validation
            long recommendationsStart = System.currentTimeMillis();

            // Get recommendations from quality results (already parsed from storage)
            List<QualityInsight> insights = reportData.getQualityResults().getRecommendations();

            // Map QualityInsight (regtech-core) to RecommendationSection (report-generation)
            List<RecommendationSection> recommendations = mapToRecommendationSections(insights);

            long recommendationsDuration = System.currentTimeMillis() - recommendationsStart;
            log.info("Quality recommendations read from storage [batchId:{},count:{},duration:{}ms]",
                    batchId, recommendations.size(), recommendationsDuration);

            // Step 5: Generate HTML and XBRL in parallel
            CompletableFuture<Result<HtmlReportMetadata>> htmlFuture = CompletableFuture.supplyAsync(() ->
                    generateHtmlReport(reportData, recommendations, batchId)
            );

            CompletableFuture<Result<XbrlReportMetadata>> xbrlFuture = CompletableFuture.supplyAsync(() ->
                    generateXbrlReport(reportData.getCalculationResults(), batchId)
            );

            // Step 6: Wait for both generations to complete (with timeout)
            CompletableFuture.allOf(htmlFuture, xbrlFuture).get(30, TimeUnit.SECONDS);

            // Step 7: Retrieve results
            Result<HtmlReportMetadata> htmlResult = htmlFuture.get();
            Result<XbrlReportMetadata> xbrlResult = xbrlFuture.get();

            boolean htmlOk = htmlResult.isSuccess();
            boolean xbrlOk = xbrlResult.isSuccess();

            if (htmlOk && xbrlOk) {
                HtmlReportMetadata htmlMetadata = htmlResult.getValueOrThrow();
                XbrlReportMetadata xbrlMetadata = xbrlResult.getValueOrThrow();
                report.markHtmlGenerated(htmlMetadata);
                report.markXbrlGenerated(xbrlMetadata);
            } else if (htmlOk) {
                HtmlReportMetadata htmlMetadata = htmlResult.getValueOrThrow();
                report.markHtmlGenerated(htmlMetadata);
                String reason = xbrlResult.getError().map(ErrorDetail::getMessage).orElse("XBRL generation failed");
                handlePartialFailure(batchId, reason);
                metrics.recordReportGenerationPartial(batchId, "xbrl_failed");
            } else if (xbrlOk) {
                XbrlReportMetadata xbrlMetadata = xbrlResult.getValueOrThrow();
                report.markXbrlGenerated(xbrlMetadata);
                String reason = htmlResult.getError().map(ErrorDetail::getMessage).orElse("HTML generation failed");
                handlePartialFailure(batchId, reason);
                metrics.recordReportGenerationPartial(batchId, "html_failed");
            } else {
                // Both failed
                String reason = String.format("HTML: %s | XBRL: %s",
                        htmlResult.getError().map(ErrorDetail::getMessage).orElse("unknown"),
                        xbrlResult.getError().map(ErrorDetail::getMessage).orElse("unknown")
                );
                handleGenerationFailure(batchId, new Exception(reason));
                metrics.recordReportGenerationFailure(batchId, "both_failed", System.currentTimeMillis() - startTime);
            }

            // Step 8: Save final report
            reportRepository.save(report);
            unitOfWork.registerEntity(report);
            unitOfWork.saveChanges();

            // Step 9: Cleanup event tracker
            eventTracker.cleanup(batchId);

            long totalDuration = System.currentTimeMillis() - startTime;
            metrics.recordReportGenerationSuccess(batchId, totalDuration);

            log.info("Comprehensive report generation completed [batchId:{},reportId:{},duration:{}ms]",
                    batchId, report.getReportId().value(), totalDuration);

        } catch (HtmlGenerationException e) {
            // HTML failed, check if XBRL succeeded for partial status
            handlePartialFailure(batchId, "HTML generation failed: " + e.getMessage());
            metrics.recordReportGenerationPartial(batchId, "html_failed");
            throw new RuntimeException("Report generation partially failed", e);

        } catch (XbrlValidationException e) {
            // XBRL failed, check if HTML succeeded for partial status
            handlePartialFailure(batchId, "XBRL generation failed: " + e.getMessage());
            metrics.recordReportGenerationPartial(batchId, "xbrl_failed");
            throw new RuntimeException("Report generation partially failed", e);

        } catch (Exception e) {
            // Complete failure
            log.error("Comprehensive report generation failed [batchId:{}]", batchId, e);
            handleGenerationFailure(batchId, e);

            long totalDuration = System.currentTimeMillis() - startTime;
            metrics.recordReportGenerationFailure(batchId, e.getClass().getSimpleName(), totalDuration);

            throw new RuntimeException("Report generation failed", e);
        }
    }

    /**
     * Generates HTML report with all sections and visualizations.
     */
        private Result<HtmlReportMetadata> generateHtmlReport(
            ComprehensiveReportData reportData,
            List<RecommendationSection> recommendations,
            String batchId) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting HTML generation [batchId:{}]", batchId);

            // Create report metadata
            ReportMetadata metadata = new ReportMetadata(
                    BatchId.of(reportData.getBatchId()),
                    BankId.of(reportData.getBankId()),
                    reportData.getBankName(),
                    ReportingDate.of(reportData.getReportingDate()),
                    Instant.now()
            );

            // Generate comprehensive HTML with both calculation and quality data
            String htmlContent = htmlGenerator.generateComprehensive(
                    reportData.getCalculationResults(),
                    reportData.getQualityResults(),
                    recommendations,
                    metadata
            );

            // Prepare file name
            String fileName = String.format("Comprehensive_Risk_Analysis_%s_%s.html",
                    reportData.getBankId(),
                    reportData.getReportingDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

                // Prepare metadata tags (moved to infra helper)
                Map<String, String> metadataTags = StorageMetadataBuilder.buildForHtml(
                    reportData.getBatchId(),
                    reportData.getBankId(),
                    reportData.getReportingDate().toString(),
                    reportData.getQualityResults().getOverallScore().toString(),
                    reportData.getBankName()
                );

            // Build storage URI (S3 or local based on configuration)
            StorageUri uri = buildStorageUri("html/" + fileName);

            // Upload using shared storage service
            Result<StorageResult> uploadResult = storageService.upload(htmlContent, uri, metadataTags);
            if (uploadResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("HTML_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload HTML report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.html_upload_failed");
                return Result.failure(err);
            }

            StorageResult storageResult = uploadResult.getValueOrThrow();

            // Generate presigned URL for 7 days (S3 only)
            // For local storage, use the file:// URI directly
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(
                    storageResult.uri(),
                    java.time.Duration.ofDays(7)
            );
            String presignedUrlStr = presignedUrlResult.isSuccess()
                    ? presignedUrlResult.getValueOrThrow()
                    : storageResult.uri().toString(); // Fallback to storage URI for local files

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordHtmlGenerationDuration(batchId, duration);

            log.info("HTML generation completed [batchId:{},fileName:{},size:{},duration:{}ms]",
                    batchId, fileName, FileSize.ofBytes(storageResult.sizeBytes()).toHumanReadable(), duration);

            Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

                HtmlReportMetadata metadataResult = HtmlReportMetadata.create(
                    new S3Uri(storageResult.uri().toString()),
                    FileSize.ofBytes(storageResult.sizeBytes()),
                    new PresignedUrl(presignedUrlStr, expiresAt, true)
                );

                return Result.success(metadataResult);

        } catch (Exception e) {
            log.error("HTML generation failed [batchId:{}]", batchId, e);
            ErrorDetail err = ErrorDetail.of("HTML_GENERATION_FAILED", ErrorType.SYSTEM_ERROR,
                    "Failed to generate HTML report: " + e.getMessage(), "report.generation.html_failed");
            return Result.failure(err);
        }
    }

    /**
     * Generates XBRL report conforming to EBA taxonomy.
     */
    private Result<XbrlReportMetadata> generateXbrlReport(CalculationResults calculationResults, String batchId) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting XBRL generation [batchId:{}]", batchId);

            // Create report metadata
            ReportMetadata metadata = new ReportMetadata(
                    calculationResults.batchId(),
                    calculationResults.bankId(),
                    calculationResults.bankName(),
                    calculationResults.reportingDate(),
                    Instant.now()
            );

            // Generate XBRL using domain service
            Document xbrlDocument = xbrlGenerator.generate(calculationResults, metadata);

            // Convert DOM Document to String for upload (infra utility)
            String xbrlContent = XmlUtils.convertDocumentToString(xbrlDocument);

            // Prepare file name
            String fileName = String.format("Large_Exposures_%s_%s.xml",
                    calculationResults.bankId().value(),
                    calculationResults.reportingDate().toFileNameString());

                // Prepare metadata tags (moved to infra helper)
                Map<String, String> metadataTags = StorageMetadataBuilder.buildForXbrl(
                    calculationResults.batchId().value(),
                    calculationResults.bankId().value(),
                    calculationResults.reportingDate().toString(),
                    calculationResults.bankName()
                );

            // Build storage URI (S3 or local based on configuration)
            StorageUri uri = buildStorageUri("xbrl/" + fileName);

            // Upload using shared storage service
            Result<StorageResult> uploadResult = storageService.upload(xbrlContent, uri, metadataTags);
            if (uploadResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("XBRL_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload XBRL report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.xbrl_upload_failed");
                return Result.failure(err);
            }

            StorageResult storageResult = uploadResult.getValueOrThrow();

            // Generate presigned URL for 7 days (S3 only)
            // For local storage, use the file:// URI directly
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(
                    storageResult.uri(),
                    java.time.Duration.ofDays(7)
            );
            String presignedUrlStr = presignedUrlResult.isSuccess()
                    ? presignedUrlResult.getValueOrThrow()
                    : storageResult.uri().toString(); // Fallback to storage URI for local files

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordXbrlGenerationDuration(batchId, duration);

            log.info("XBRL generation completed [batchId:{},fileName:{},size:{},duration:{}ms]",
                    batchId, fileName, FileSize.ofBytes(storageResult.sizeBytes()).toHumanReadable(), duration);

                Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
                XbrlReportMetadata xbrlMetadata = XbrlReportMetadata.create(
                    new S3Uri(storageResult.uri().toString()),
                    FileSize.ofBytes(storageResult.sizeBytes()),
                    new PresignedUrl(presignedUrlStr, expiresAt, true),
                    XbrlValidationStatus.VALID
                );
                return Result.success(xbrlMetadata);

        } catch (Exception e) {
                log.error("XBRL generation failed [batchId:{}]", batchId, e);
                ErrorDetail err = ErrorDetail.of("XBRL_GENERATION_FAILED", ErrorType.SYSTEM_ERROR,
                    "Failed to generate XBRL report: " + e.getMessage(), "report.generation.xbrl_failed");
                return Result.failure(err);
        }
    }

    /**
     * Converts DOM Document to String for upload.
     */
    // Moved to infrastructure util: XmlUtils.convertDocumentToString

    /**
     * Maps QualityInsight (from regtech-core) to RecommendationSection (report-generation domain).
     * <p>
     * Transformation:
     * - ruleId → used to derive title (e.g., "critical_situation" → "Situazione Critica")
     * - severity → mapped to icon and colorClass (CRITICAL → 🚨/red, HIGH → ⚠️/yellow, etc.)
     * - message → content
     * - actionItems → bullets
     * - locale → used for localization (already applied in message/actionItems)
     */
    private List<RecommendationSection> mapToRecommendationSections(List<QualityInsight> insights) {
        return insights.stream()
                .map(this::mapInsightToSection)
                .collect(Collectors.toList());
    }

    /**
     * Maps a single QualityInsight to RecommendationSection
     */
    private RecommendationSection mapInsightToSection(QualityInsight insight) {
        // Map severity to icon and color
        String icon = mapSeverityToIcon(insight.severity());
        String colorClass = mapSeverityToColorClass(insight.severity());

        // Derive title from rule ID or use message (first 50 chars)
        String title = deriveTitleFromRuleId(insight.ruleId());

        return RecommendationSection.builder()
                .icon(icon)
                .colorClass(colorClass)
                .title(title)
                .content(insight.message())
                .bullets(insight.actionItems())
                .build();
    }

    /**
     * Maps RecommendationSeverity to emoji icon
     */
    private String mapSeverityToIcon(RecommendationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "🚨";  // Critical alert
            case HIGH -> "⚠️";      // Warning
            case MEDIUM -> "ℹ️";    // Information
            case LOW -> "💡";       // Lightbulb (suggestion)
            case SUCCESS -> "✅";   // Checkmark (excellent)
        };
    }

    /**
     * Maps RecommendationSeverity to CSS color class
     */
    private String mapSeverityToColorClass(RecommendationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "red";     // Critical issues
            case HIGH -> "orange";      // High priority
            case MEDIUM -> "yellow";    // Medium priority
            case LOW -> "blue";         // Low priority suggestions
            case SUCCESS -> "green";    // Positive aspects
        };
    }

    /**
     * Derives a human-readable title from rule ID
     */
    private String deriveTitleFromRuleId(String ruleId) {
        return switch (ruleId) {
            case "critical_situation" -> "Situazione Critica - Azione Immediata Richiesta";
            case "dimension_below_threshold" -> "Dimensioni di Qualità da Migliorare";
            case "excellent_dimensions" -> "Eccellenti Risultati di Qualità";
            case "action_plan" -> "Piano d'Azione per il Miglioramento";
            default -> "Raccomandazioni sulla Qualità dei Dati";  // Generic fallback
        };
    }

    /**
     * Handles partial failure where one format succeeded but the other failed.
     */
    private void handlePartialFailure(String batchId, String reason) {
        try {
            Optional<GeneratedReport> reportOpt = reportRepository.findByBatchId(BatchId.of(batchId));
            if (reportOpt.isPresent()) {
                GeneratedReport report = reportOpt.get();
                report.markPartial(reason);
                reportRepository.save(report);
                unitOfWork.registerEntity(report);
                unitOfWork.saveChanges();

                log.warn("Report marked as PARTIAL [batchId:{},reason:{}]", batchId, reason);
            }
        } catch (Exception saveException) {
            log.error("Failed to save PARTIAL status [batchId:{}]", batchId, saveException);
        }
    }

    /**
     * Handles complete generation failure.
     */
    private void handleGenerationFailure(String batchId, Exception e) {
        try {
            Optional<GeneratedReport> reportOpt = reportRepository.findByBatchId(BatchId.of(batchId));
            if (reportOpt.isPresent()) {
                GeneratedReport report = reportOpt.get();
                report.markFailed(FailureReason.of(e.getMessage()));
                reportRepository.save(report);
                unitOfWork.registerEntity(report);
                unitOfWork.saveChanges();

                log.error("Report marked as FAILED [batchId:{},reason:{}]", batchId, e.getMessage());
            }
        } catch (Exception saveException) {
            log.error("Failed to save FAILED status [batchId:{}]", batchId, saveException);
        }
    }
}
