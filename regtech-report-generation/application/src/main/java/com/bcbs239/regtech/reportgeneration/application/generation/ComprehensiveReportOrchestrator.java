package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.reportgeneration.application.coordination.BatchEventTracker;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.IComprehensiveReportOrchestrator;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.domain.generation.*;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import com.bcbs239.regtech.reportgeneration.domain.storage.IReportStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the generation of comprehensive reports combining risk calculation
 * and quality validation results.
 * 
 * This service:
 * - Checks for existing reports (idempotency)
 * - Aggregates data from both calculation and quality sources
 * - Generates quality recommendations
 * - Generates HTML and XBRL reports in parallel
 * - Uploads reports to S3
 * - Persists report metadata
 * - Handles failures with appropriate status updates
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 16.1, 16.2, 23.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveReportOrchestrator implements IComprehensiveReportOrchestrator {
    
    private final ComprehensiveReportDataAggregator dataAggregator;
    private final QualityRecommendationsGenerator recommendationsGenerator;
    private final HtmlReportGenerator htmlGenerator;
    private final XbrlReportGenerator xbrlGenerator;
    private final IReportStorageService storageService;
    private final IGeneratedReportRepository reportRepository;
    private final BatchEventTracker eventTracker;
    private final ReportGenerationMetrics metrics;
    
    /**
     * Generates a comprehensive report asynchronously.
     * 
     * This method:
     * 1. Checks for existing COMPLETED reports (idempotency)
     * 2. Fetches and aggregates data from both sources
     * 3. Creates GeneratedReport aggregate
     * 4. Generates quality recommendations
     * 5. Generates HTML and XBRL in parallel
     * 6. Updates report with results
     * 7. Cleans up event tracker
     * 
     * @param riskEventData the risk calculation event data
     * @param qualityEventData the quality validation event data
     * @return CompletableFuture that completes when generation finishes
     */
    @Async("reportGenerationExecutor")
    @Override
    public CompletableFuture<Void> generateComprehensiveReport(
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
                return CompletableFuture.completedFuture(null);
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
            log.info("Created report aggregate [batchId:{},reportId:{}]", batchId, report.getReportId().value());
            
            // Step 4: Generate quality recommendations
            long recommendationsStart = System.currentTimeMillis();
            List<RecommendationSection> recommendations = 
                recommendationsGenerator.generateRecommendations(reportData.getQualityResults());
            long recommendationsDuration = System.currentTimeMillis() - recommendationsStart;
            
            log.info("Quality recommendations generated [batchId:{},count:{},duration:{}ms]", 
                batchId, recommendations.size(), recommendationsDuration);
            
            // Step 5: Generate HTML and XBRL in parallel
            CompletableFuture<HtmlReportMetadata> htmlFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return generateHtmlReport(reportData, recommendations, batchId);
                } catch (Exception e) {
                    log.error("HTML generation failed [batchId:{}]", batchId, e);
                    throw new HtmlGenerationException("HTML generation failed", e);
                }
            });
            
            CompletableFuture<XbrlReportMetadata> xbrlFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return generateXbrlReport(reportData.getCalculationResults(), batchId);
                } catch (Exception e) {
                    log.error("XBRL generation failed [batchId:{}]", batchId, e);
                    throw new XbrlValidationException("XBRL generation failed", e);
                }
            });
            
            // Step 6: Wait for both generations to complete (with timeout)
            CompletableFuture.allOf(htmlFuture, xbrlFuture).get(30, TimeUnit.SECONDS);
            
            // Step 7: Update report with results
            HtmlReportMetadata htmlMetadata = htmlFuture.get();
            XbrlReportMetadata xbrlMetadata = xbrlFuture.get();
            
            report.markHtmlGenerated(htmlMetadata);
            report.markXbrlGenerated(xbrlMetadata);
            
            // Step 8: Save final report
            reportRepository.save(report);
            
            // Step 9: Cleanup event tracker
            eventTracker.cleanup(batchId);
            
            long totalDuration = System.currentTimeMillis() - startTime;
            metrics.recordReportGenerationSuccess(batchId, totalDuration);
            
            log.info("Comprehensive report generation completed [batchId:{},reportId:{},duration:{}ms]",
                batchId, report.getReportId().value(), totalDuration);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (HtmlGenerationException e) {
            // HTML failed, check if XBRL succeeded for partial status
            handlePartialFailure(batchId, "HTML generation failed: " + e.getMessage(), e);
            metrics.recordReportGenerationPartial(batchId, "html_failed");
            throw new RuntimeException("Report generation partially failed", e);
            
        } catch (XbrlValidationException e) {
            // XBRL failed, check if HTML succeeded for partial status
            handlePartialFailure(batchId, "XBRL generation failed: " + e.getMessage(), e);
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
    private HtmlReportMetadata generateHtmlReport(
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
            
            // Prepare metadata tags
            Map<String, String> metadataTags = new HashMap<>();
            metadataTags.put("batch-id", reportData.getBatchId());
            metadataTags.put("bank-id", reportData.getBankId());
            metadataTags.put("reporting-date", reportData.getReportingDate().toString());
            metadataTags.put("quality-score", reportData.getQualityResults().getOverallScore().toString());
            metadataTags.put("generated-at", Instant.now().toString());
            
            // Upload to S3
            IReportStorageService.UploadResult uploadResult = 
                storageService.uploadHtmlReport(htmlContent, fileName, metadataTags);
            
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordHtmlGenerationDuration(batchId, duration);
            
            log.info("HTML generation completed [batchId:{},fileName:{},size:{},duration:{}ms]",
                batchId, fileName, uploadResult.fileSize().toHumanReadable(), duration);
            
            return HtmlReportMetadata.create(
                uploadResult.s3Uri(),
                uploadResult.fileSize(),
                uploadResult.presignedUrl()
            );
                
        } catch (Exception e) {
            log.error("HTML generation failed [batchId:{}]", batchId, e);
            throw new HtmlGenerationException("Failed to generate HTML report", e);
        }
    }
    
    /**
     * Generates XBRL report conforming to EBA taxonomy.
     */
    private XbrlReportMetadata generateXbrlReport(CalculationResults calculationResults, String batchId) {
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
            
            // Prepare file name
            String fileName = String.format("Large_Exposures_%s_%s.xml",
                calculationResults.bankId().value(),
                calculationResults.reportingDate().toFileNameString());
            
            // Prepare metadata tags
            Map<String, String> metadataTags = new HashMap<>();
            metadataTags.put("batch-id", calculationResults.batchId().value());
            metadataTags.put("bank-id", calculationResults.bankId().value());
            metadataTags.put("reporting-date", calculationResults.reportingDate().toString());
            metadataTags.put("generated-at", Instant.now().toString());
            
            // Upload to S3
            IReportStorageService.UploadResult uploadResult = 
                storageService.uploadXbrlReport(xbrlDocument, fileName, metadataTags);
            
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordXbrlGenerationDuration(batchId, duration);
            
            log.info("XBRL generation completed [batchId:{},fileName:{},size:{},duration:{}ms]",
                batchId, fileName, uploadResult.fileSize().toHumanReadable(), duration);
            
            return XbrlReportMetadata.create(
                uploadResult.s3Uri(),
                uploadResult.fileSize(),
                uploadResult.presignedUrl(),
                XbrlValidationStatus.VALID
            );
                
        } catch (Exception e) {
            log.error("XBRL generation failed [batchId:{}]", batchId, e);
            throw new XbrlValidationException("Failed to generate XBRL report", e);
        }
    }
    
    /**
     * Handles partial failure where one format succeeded but the other failed.
     */
    private void handlePartialFailure(String batchId, String reason, Exception e) {
        try {
            Optional<GeneratedReport> reportOpt = reportRepository.findByBatchId(BatchId.of(batchId));
            if (reportOpt.isPresent()) {
                GeneratedReport report = reportOpt.get();
                report.markPartial(reason);
                reportRepository.save(report);
                
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
                
                log.error("Report marked as FAILED [batchId:{},reason:{}]", batchId, e.getMessage());
            }
        } catch (Exception saveException) {
            log.error("Failed to save FAILED status [batchId:{}]", batchId, saveException);
        }
    }
}
