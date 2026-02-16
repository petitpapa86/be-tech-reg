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
import com.bcbs239.regtech.reportgeneration.domain.generation.PdfReportGenerator;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FailureReason;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.HtmlReportMetadata;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PdfReportMetadata;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportStatus;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.XbrlReportMetadata;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.XbrlValidationStatus;

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
    private final PdfReportGenerator pdfGenerator;
    private final IStorageService storageService;  // Changed from IReportStorageService
    private final IGeneratedReportRepository reportRepository;
    private final BatchEventTracker eventTracker;
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
    public CompletableFuture<Result<GeneratedReport>> generateComprehensiveReport(
            CalculationEventData riskEventData,
            QualityEventData qualityEventData) {

        long startTime = System.currentTimeMillis();
        String batchId = riskEventData.getBatchId();

        try {
            log.info("Starting comprehensive report generation [batchId:{}]", batchId);

            Optional<GeneratedReport> existingReport = reportRepository.findByBatchId(BatchId.of(batchId));
            if (existingReport.isPresent() && existingReport.get().getStatus() == ReportStatus.COMPLETED) {
                log.info("Report already exists with COMPLETED status [batchId:{}], skipping generation", batchId);
                return CompletableFuture.completedFuture(Result.success(existingReport.get()));
            }

            long dataFetchStart = System.currentTimeMillis();
            Result<ComprehensiveReportData> aggregationResult = dataAggregator.fetchAllData(riskEventData, qualityEventData);
            long dataFetchDuration = System.currentTimeMillis() - dataFetchStart;

            if (aggregationResult.isFailure()) {
                log.error("Data aggregation failed [batchId:{}]: {}", batchId, aggregationResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return CompletableFuture.completedFuture(Result.failure(aggregationResult.errors()));
            }

            ComprehensiveReportData reportData = aggregationResult.getValueOrThrow();

            log.info("Data aggregation completed [batchId:{},duration:{}ms]", batchId, dataFetchDuration);

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

            long recommendationsStart = System.currentTimeMillis();

            List<QualityInsight> insights = reportData.getQualityResults().getRecommendations();

            List<RecommendationSection> recommendations = insights.stream()
                    .map(this::mapInsightToSection)
                    .collect(Collectors.toList());

            long recommendationsDuration = System.currentTimeMillis() - recommendationsStart;
            log.info("Quality recommendations read from storage [batchId:{},count:{},duration:{}ms]",
                    batchId, recommendations.size(), recommendationsDuration);

            log.info("Starting parallel report generation [batchId:{},timeout:{}s]", batchId, 300);
            long parallelGenerationStart = System.currentTimeMillis();

            CompletableFuture<Result<HtmlAndPdfMetadata>> htmlPdfFuture = CompletableFuture.supplyAsync(() -> {
                log.info("HTML/PDF generation task started [batchId:{}]", batchId);
                return generateHtmlAndPdf(reportData, recommendations, batchId);
            });

            CompletableFuture<Result<XbrlReportMetadata>> xbrlFuture = CompletableFuture.supplyAsync(() -> {
                log.info("XBRL generation task started [batchId:{}]", batchId);
                return generateXbrlReport(reportData.getCalculationResults(), batchId);
            });

            // Wait for both tasks to complete with 5-minute timeout (increased from 30s for large reports)
            CompletableFuture.allOf(htmlPdfFuture, xbrlFuture).get(300, TimeUnit.SECONDS);

            long parallelGenerationDuration = System.currentTimeMillis() - parallelGenerationStart;
            log.info("Parallel report generation completed [batchId:{},duration:{}ms]", batchId, parallelGenerationDuration);

            Result<HtmlAndPdfMetadata> htmlPdfResult = htmlPdfFuture.get();
            Result<XbrlReportMetadata> xbrlResult = xbrlFuture.get();

            boolean htmlPdfOk = htmlPdfResult.isSuccess();
            boolean xbrlOk = xbrlResult.isSuccess();

            if (htmlPdfOk && xbrlOk) {
                HtmlAndPdfMetadata htmlPdfMetadata = htmlPdfResult.getValueOrThrow();
                XbrlReportMetadata xbrlMetadata = xbrlResult.getValueOrThrow();
                report.markHtmlGenerated(htmlPdfMetadata.htmlMetadata());
                report.markPdfGenerated(htmlPdfMetadata.pdfMetadata());
                report.markXbrlGenerated(xbrlMetadata);
            } else if (htmlPdfOk) {
                HtmlAndPdfMetadata htmlPdfMetadata = htmlPdfResult.getValueOrThrow();
                report.markHtmlGenerated(htmlPdfMetadata.htmlMetadata());
                report.markPdfGenerated(htmlPdfMetadata.pdfMetadata());
                String reason = xbrlResult.getError().map(ErrorDetail::getMessage).orElse("XBRL generation failed");
                handlePartialFailure(batchId, reason);
            } else if (xbrlOk) {
                XbrlReportMetadata xbrlMetadata = xbrlResult.getValueOrThrow();
                report.markXbrlGenerated(xbrlMetadata);
                String reason = htmlPdfResult.getError().map(ErrorDetail::getMessage).orElse("HTML/PDF generation failed");
                handlePartialFailure(batchId, reason);
            } else {
                // Both failed
                String reason = String.format("HTML/PDF: %s | XBRL: %s",
                        htmlPdfResult.getError().map(ErrorDetail::getMessage).orElse("unknown"),
                        xbrlResult.getError().map(ErrorDetail::getMessage).orElse("unknown")
                );
                handleGenerationFailure(batchId, new Exception(reason));
                return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of("GENERATION_FAILED", ErrorType.SYSTEM_ERROR, reason, "report.generation.failed")));
            }

            // Step 8: Save final report
            reportRepository.save(report);
            unitOfWork.registerEntity(report);
            unitOfWork.saveChanges();

            // Step 9: Cleanup event tracker
            eventTracker.cleanup(batchId);

            long totalDuration = System.currentTimeMillis() - startTime;
            // Success metric handled by @Timed

            log.info("Comprehensive report generation completed [batchId:{},reportId:{},duration:{}ms]",
                    batchId, report.getReportId().value(), totalDuration);

            return CompletableFuture.completedFuture(Result.success(report));

        } catch (Exception e) {
            // Complete failure
            log.error("Comprehensive report generation failed [batchId:{}]", batchId, e);
            handleGenerationFailure(batchId, e);

            // Success metric handled by @Timed (removed)

            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of(
                    "REPORT_GENERATION_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Report generation failed: " + e.getMessage(),
                    "report.generation.failed"
            )));
        }
    }

    /**
     * Generates HTML and PDF reports with all sections and visualizations.
     */
    private Result<HtmlAndPdfMetadata> generateHtmlAndPdf(
            ComprehensiveReportData reportData,
            List<RecommendationSection> recommendations,
            String batchId) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting HTML and PDF generation [batchId:{}]", batchId);

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

            // Prepare HTML file name
            String htmlFileName = String.format("Comprehensive_Risk_Analysis_%s_%s.html",
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
            String subPath = "html/" + htmlFileName;
            Result<StorageResult> uploadResult = storageService.uploadToStorage(htmlContent, subPath, metadataTags);
            if (uploadResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("HTML_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload HTML report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.html_upload_failed");
                return Result.failure(err);
            }

            StorageResult storageResult = uploadResult.getValueOrThrow();

            // Generate presigned URL for 7 days
            // Note: For local storage, this will return the file path or a dummy URL
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(storageResult.uri(), java.time.Duration.ofDays(7));
            String presignedUrlStr = presignedUrlResult.isSuccess()
                    ? presignedUrlResult.getValueOrThrow()
                    : storageResult.uri().toString(); // Fallback to storage URI for local files

            long duration = System.currentTimeMillis() - startTime;

            log.info("HTML generation completed [batchId:{},fileName:{},size:{},duration:{}ms]",
                    batchId, htmlFileName, FileSize.ofBytes(storageResult.sizeBytes()).toHumanReadable(), duration);

            Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

            HtmlReportMetadata htmlMetadata = HtmlReportMetadata.create(
                    new S3Uri(storageResult.uri().toString()),
                    FileSize.ofBytes(storageResult.sizeBytes()),
                    new PresignedUrl(presignedUrlStr, expiresAt, true)
            );

            // Generate PDF from HTML content
            Result<PdfReportMetadata> pdfResult = generatePdfReport(htmlContent, reportData, metadataTags);
            
            if (pdfResult.isFailure()) {
                // If PDF generation fails, we should probably fail the whole HTML/PDF process 
                // because we want consistent output. Or return partial?
                // For now, let's fail it as requested "true pdf".
                return Result.failure(pdfResult.getError().orElseThrow());
            }

            return Result.success(new HtmlAndPdfMetadata(htmlMetadata, pdfResult.getValueOrThrow()));

        } catch (Exception e) {
            log.error("HTML/PDF generation failed [batchId:{}]", batchId, e);
            ErrorDetail err = ErrorDetail.of("HTML_PDF_GENERATION_FAILED", ErrorType.SYSTEM_ERROR,
                    "Failed to generate HTML/PDF report: " + e.getMessage(), "report.generation.html_pdf_failed");
            return Result.failure(err);
        }
    }

    /**
     * Generates PDF report from HTML content.
     */
    private Result<PdfReportMetadata> generatePdfReport(
            String htmlContent, 
            ComprehensiveReportData reportData,
            Map<String, String> metadataTags) {
            
        long startTime = System.currentTimeMillis();
        String batchId = reportData.getBatchId().value();
        
        try {
            log.info("Starting PDF generation [batchId:{}]", batchId);
            
            // Generate PDF bytes
            Result<byte[]> pdfBytesResult = pdfGenerator.generateFromHtml(htmlContent);
            
            if (pdfBytesResult.isFailure()) {
                return Result.failure(pdfBytesResult.getError().orElseThrow());
            }
            
            byte[] pdfBytes = pdfBytesResult.getValueOrThrow();
            
            // Prepare PDF file name
            String pdfFileName = String.format("Comprehensive_Risk_Analysis_%s_%s.pdf",
                    reportData.getBankId().value(),
                    reportData.getReportingDate().toFileNameString());
            
            // Upload PDF
            String subPath = "pdf/" + pdfFileName;
            // Use uploadToStorageBytes which handles URI creation
            Result<StorageResult> uploadResult = storageService.uploadToStorageBytes(
                pdfBytes, 
                subPath,
                "application/pdf",
                metadataTags
            );
            
            if (uploadResult.isFailure()) {
                 ErrorDetail err = ErrorDetail.of("PDF_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload PDF report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.pdf_upload_failed");
                return Result.failure(err);
            }
            
            StorageResult storageResult = uploadResult.getValueOrThrow();
            
            // Generate presigned URL
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(storageResult.uri(), java.time.Duration.ofDays(7));
            String presignedUrlStr = presignedUrlResult.isSuccess()
                    ? presignedUrlResult.getValueOrThrow()
                    : storageResult.uri().toString();
                    
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("PDF generation completed [batchId:{},fileName:{},size:{},duration:{}ms]",
                    batchId, pdfFileName, FileSize.ofBytes(storageResult.sizeBytes()).toHumanReadable(), duration);
            
            Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
            
            return Result.success(PdfReportMetadata.create(
                    new S3Uri(storageResult.uri().toString()),
                    FileSize.ofBytes(storageResult.sizeBytes()),
                    new PresignedUrl(presignedUrlStr, expiresAt, true)
            ));
            
        } catch (Exception e) {
            log.error("PDF generation failed [batchId:{}]", batchId, e);
            ErrorDetail err = ErrorDetail.of("PDF_GENERATION_FAILED", ErrorType.SYSTEM_ERROR,
                    "Failed to generate PDF report: " + e.getMessage(), "report.generation.pdf_failed");
            return Result.failure(err);
        }
    }

    private record HtmlAndPdfMetadata(HtmlReportMetadata htmlMetadata, PdfReportMetadata pdfMetadata) {}

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
            String subPath = "xbrl/" + fileName;
            Result<StorageResult> uploadResult = storageService.uploadToStorage(xbrlContent, subPath, metadataTags);
            if (uploadResult.isFailure()) {
                ErrorDetail err = ErrorDetail.of("XBRL_UPLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                        "Failed to upload XBRL report to storage: " + uploadResult.getError().orElseThrow().getMessage(),
                        "report.generation.xbrl_upload_failed");
                return Result.failure(err);
            }

            StorageResult storageResult = uploadResult.getValueOrThrow();
            
            Result<String> presignedUrlResult = storageService.generatePresignedUrl(storageResult.uri(), java.time.Duration.ofDays(7));
            String presignedUrlStr = presignedUrlResult.isSuccess()
                    ? presignedUrlResult.getValueOrThrow()
                    : storageResult.uri().toString();

            long duration = System.currentTimeMillis() - startTime;

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
