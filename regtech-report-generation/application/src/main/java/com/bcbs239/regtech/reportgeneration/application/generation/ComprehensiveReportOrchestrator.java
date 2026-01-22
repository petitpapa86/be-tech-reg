package com.bcbs239.regtech.reportgeneration.application.generation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.recommendations.QualityInsight;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.IStorageService;
import com.bcbs239.regtech.core.domain.storage.StorageResult;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.reportgeneration.application.coordination.BatchEventTracker;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.application.storage.StorageMetadataService;
import com.bcbs239.regtech.reportgeneration.application.util.XmlSerializer;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport;
import com.bcbs239.regtech.reportgeneration.domain.generation.HtmlReportGenerator;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import com.bcbs239.regtech.reportgeneration.domain.generation.RecommendationSection;
import com.bcbs239.regtech.reportgeneration.domain.generation.ReportMetadata;
import com.bcbs239.regtech.reportgeneration.domain.generation.XbrlReportGenerator;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FailureReason;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.HtmlReportMetadata;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportStatus;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.XbrlReportMetadata;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.XbrlValidationStatus;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
public class ComprehensiveReportOrchestrator {

    private final IReportDataSource dataAggregator;
    private final HtmlReportGenerator htmlGenerator;
    private final XbrlReportGenerator xbrlGenerator;
    private final IStorageService storageService;  // Changed from IReportStorageService
    private final IGeneratedReportRepository reportRepository;
    private final BatchEventTracker eventTracker;
    private final ReportGenerationMetrics metrics;
    private final BaseUnitOfWork unitOfWork;
    private final StorageMetadataService storageMetadataService;
    private final XmlSerializer xmlSerializer;


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
     * @return a CompletableFuture containing the result of the report generation
     */
    @Async("reportGenerationExecutor")
    @Timed(value = "reportgeneration.comprehensive", description = "Time taken to generate comprehensive report")
    public CompletableFuture<Result<GeneratedReport>> generateComprehensiveReport(
            CalculationEventData riskEventData,
            QualityEventData qualityEventData) {

        long startTime = System.currentTimeMillis();
        String batchId = riskEventData.getBatchId();

        try {
            log.info("Starting comprehensive report generation [batchId:{}]", batchId);
            metrics.recordReportGenerationStart(batchId);

            // Step 1: Check for existing COMPLETED report (idempotency)
            Optional<GeneratedReport> existingReport = reportRepository.findByBatchId(BatchId.of(batchId));
            if (existingReport.isPresent() && existingReport.get().getStatus() == ReportStatus.COMPLETED) {
                log.info("Report already exists with COMPLETED status [batchId:{}], skipping generation", batchId);
                metrics.recordDuplicateSkipped(batchId);
                return CompletableFuture.completedFuture(Result.success(existingReport.get()));
            }

            // Step 2: Fetch and aggregate data from both sources
            long dataFetchStart = System.currentTimeMillis();
            Result<ComprehensiveReportData> aggregationResult = dataAggregator.fetchAllData(riskEventData, qualityEventData);
            long dataFetchDuration = System.currentTimeMillis() - dataFetchStart;
            metrics.recordDataFetchDuration(batchId, dataFetchDuration);

            if (aggregationResult.isFailure()) {
                log.error("Data aggregation failed [batchId:{}]: {}", batchId, aggregationResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return CompletableFuture.completedFuture(Result.failure(aggregationResult.errors()));
            }

            ComprehensiveReportData reportData = aggregationResult.getValueOrThrow();

            log.info("Data aggregation completed [batchId:{},duration:{}ms]", batchId, dataFetchDuration);

            // Step 3: Create GeneratedReport aggregate
            GeneratedReport report = GeneratedReport.createComprehensiveReport(
                    reportData.getBatchId(),
                    reportData.getBankId(),
                    reportData.getReportingDate(),
                    reportData.getOverallScoreAsBigDecimal(),
                    reportData.getComplianceStatus()
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
            List<RecommendationSection> recommendations = insights.stream()
                    .map(this::mapInsightToSection)
                    .collect(Collectors.toList());

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
                return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of("GENERATION_FAILED", ErrorType.SYSTEM_ERROR, reason, "report.generation.failed")));
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

            return CompletableFuture.completedFuture(Result.success(report));

        } catch (Exception e) {
            // Complete failure
            log.error("Comprehensive report generation failed [batchId:{}]", batchId, e);
            handleGenerationFailure(batchId, e);

            long totalDuration = System.currentTimeMillis() - startTime;
            metrics.recordReportGenerationFailure(batchId, e.getClass().getSimpleName(), totalDuration);

            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of(
                    "REPORT_GENERATION_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Report generation failed: " + e.getMessage(),
                    "report.generation.failed"
            )));
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
                    reportData.getBatchId(),
                    reportData.getBankId(),
                    reportData.getBankName(),
                    reportData.getReportingDate(),
                    Instant.now()
            );

            // Generate comprehensive HTML with both calculation and quality data
            Result<String> htmlContentResult = htmlGenerator.generateComprehensive(
                    reportData.getCalculationResults(),
                    reportData.getQualityResults(),
                    recommendations,
                    metadata
            );
            
            if (htmlContentResult.isFailure()) {
                return Result.failure(htmlContentResult.getError().orElseThrow());
            }
            
            String htmlContent = htmlContentResult.getValueOrThrow();

            // Prepare file name
            String fileName = String.format("Comprehensive_Risk_Analysis_%s_%s.html",
                    reportData.getBankId().value(),
                    reportData.getReportingDate().toFileNameString());

            // Prepare metadata tags via application port (infra implements)
            Map<String, String> metadataTags = storageMetadataService.buildForHtml(
                    reportData.getBatchId().value(),
                    reportData.getBankId().value(),
                    reportData.getReportingDate().toString(),
                    reportData.getOverallScoreAsBigDecimal().toString(),
                    reportData.getBankName().value()
            );

            // Upload using shared storage service
            StorageUri uri = StorageUri.parse("s3://" + storageMetadataService.getStorageBucket() + "/html/" + fileName);
            Result<StorageResult> uploadResult = storageService.upload(htmlContent, uri, metadataTags);
            if (uploadResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("HTML_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload HTML report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.html_upload_failed");
                return Result.failure(err);
            }

            StorageResult storageResult = uploadResult.getValueOrThrow();

            // Generate presigned URL for 7 days
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(uri, java.time.Duration.ofDays(7));
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
            Result<Document> xbrlDocumentResult = xbrlGenerator.generate(calculationResults, metadata);
            
            if (xbrlDocumentResult.isFailure()) {
                return Result.failure(xbrlDocumentResult.getError().orElseThrow());
            }
            
            Document xbrlDocument = xbrlDocumentResult.getValueOrThrow();

            // Convert DOM Document to String for upload (via application port)
            var xbrlContentResult = xmlSerializer.convertDocumentToString(xbrlDocument);
            if (xbrlContentResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("XBRL_SERIALIZATION_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to serialize XBRL document: " + xbrlContentResult.getError().map(ErrorDetail::getMessage).orElse("unknown"),
                        "report.generation.xbrl_serialization_failed");
                return Result.failure(err);
            }
            String xbrlContent = xbrlContentResult.getValueOrThrow();

            // Prepare file name
            String fileName = String.format("Large_Exposures_%s_%s.xml",
                    calculationResults.bankId().value(),
                    calculationResults.reportingDate().toFileNameString());

            // Prepare metadata tags via application port (infra implements)
            Map<String, String> metadataTags = storageMetadataService.buildForXbrl(
                    calculationResults.batchId().value(),
                    calculationResults.bankId().value(),
                    calculationResults.reportingDate().toString(),
                    calculationResults.bankName().value()
            );

            // Upload using shared storage service
            StorageUri uri = StorageUri.parse("s3://" + storageMetadataService.getStorageBucket() + "/xbrl/" + fileName);
            Result<StorageResult> uploadResult = storageService.upload(xbrlContent, uri, metadataTags);
            if (uploadResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("XBRL_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload XBRL report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.xbrl_upload_failed");
                return Result.failure(err);
            }

            StorageResult storageResult = uploadResult.getValueOrThrow();

            // Generate presigned URL for 7 days
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(uri, java.time.Duration.ofDays(7));
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
     * Maps a single QualityInsight to RecommendationSection
     */
    private RecommendationSection mapInsightToSection(QualityInsight insight) {
        // Map severity to icon and color
        String icon = insight.severity().getIcon();
        String colorClass = insight.severity().getColor();

        // Derive title from insight (domain provides mapping)
        String title = insight.ruleId();

        return RecommendationSection.builder()
                .icon(icon)
                .colorClass(colorClass)
                .title(title)
                .content(insight.message())
                .bullets(insight.actionItems())
                .build();
    }

    /**
     * Handles partial failure where one format succeeded but the other failed.
     */
    private void handlePartialFailure(String batchId, String reason) {
        Optional<GeneratedReport> reportOpt = reportRepository.findByBatchId(BatchId.of(batchId));
        if (reportOpt.isPresent()) {
            GeneratedReport report = reportOpt.get();
            report.markPartial(reason);
            reportRepository.save(report);
            unitOfWork.registerEntity(report);
            unitOfWork.saveChanges();

            log.warn("Report marked as PARTIAL [batchId:{},reason:{}]", batchId, reason);
        }
    }

    /**
     * Handles complete generation failure.
     */
    private void handleGenerationFailure(String batchId, Exception e) {
        Optional<GeneratedReport> reportOpt = reportRepository.findByBatchId(BatchId.of(batchId));
        if (reportOpt.isPresent()) {
            GeneratedReport report = reportOpt.get();
            report.markFailed(FailureReason.of(e.getMessage()));
            reportRepository.save(report);
            unitOfWork.registerEntity(report);
            unitOfWork.saveChanges();

            log.error("Report marked as FAILED [batchId:{},reason:{}]", batchId, e.getMessage());
        }

    }
}
