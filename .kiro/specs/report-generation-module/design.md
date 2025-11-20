# Design Document

## Overview

The Report Generation Module generates comprehensive HTML and XBRL-XML reports combining Large Exposures (Grandi Esposizioni) analysis with Data Quality validation results. The module follows Domain-Driven Design principles, implements event-driven architecture, and integrates with shared infrastructure components. The system waits for BOTH BatchCalculationCompletedEvent and BatchQualityCompletedEvent before generating a single comprehensive report that includes risk analysis and quality assessment with dynamic recommendations.

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Report Generation Module                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Presentation Layer                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Health          │  │ Metrics         │  │ Status          │                │
│  │ Controller      │  │ Collector       │  │ Controller      │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Application Layer                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Report Event    │  │ Comprehensive   │  │ Quality Rec.    │                │
│  │ Listener        │  │ Report Orch.    │  │ Generator       │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Data Aggregator │  │ HTML Generator  │  │ XBRL Generator  │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Generated       │  │ Report          │  │ Quality         │                │
│  │ Report          │  │ Coordinator     │  │ Results         │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Calculation     │  │ Value Objects   │  │ Domain Events   │                │
│  │ Results         │  │                 │  │                 │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ JPA Repository  │  │ S3 Storage      │  │ File Path       │                │
│  │                 │  │ Service         │  │ Resolver        │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Event Publisher │  │ Health Checker  │  │ Metrics         │                │
│  │                 │  │                 │  │ Registry        │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Event Flow Architecture

```
┌─────────────────┐    ┌─────────────────┐
│ Risk Calculation│    │ Data Quality    │
│ Module          │    │ Module          │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────┐    ┌─────────────────┐
│BatchCalculation │    │BatchQuality     │
│CompletedEvent   │    │CompletedEvent   │
│(s3Uri: calc)    │    │(s3Uri: quality) │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     ▼
          ┌─────────────────┐
          │ Report Event    │
          │ Listener        │
          └─────────┬───────┘
                    │
                    ▼
          ┌─────────────────┐
          │ Batch Event     │
          │ Tracker         │
          │ (Wait for BOTH) │
          └─────────┬───────┘
                    │
                    ▼ (Both Present)
          ┌─────────────────┐
          │ Comprehensive   │
          │ Report          │
          │ Orchestrator    │
          └─────────┬───────┘
                    │
                    ▼
    ┌───────────────┴───────────────┐
    │                               │
    ▼                               ▼
┌─────────────────┐    ┌─────────────────┐
│ Fetch Calc JSON │    │ Fetch Quality   │
│ from S3/FS      │    │ JSON from S3/FS │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     ▼
          ┌─────────────────┐
          │ Generate        │
          │ Comprehensive   │
          │ Report          │
          │ (HTML + XBRL)   │
          └─────────────────┘
```


## Domain Layer

### Aggregates

#### GeneratedReport Aggregate

```java
@Entity
@Table(name = "generated_reports")
public class GeneratedReport extends BaseAggregateRoot {
    @Id
    private ReportId reportId;
    
    @Embedded
    private BatchId batchId;
    
    @Embedded
    private BankId bankId;
    
    @Enumerated(EnumType.STRING)
    private ReportType reportType; // COMPREHENSIVE
    
    @Embedded
    private ReportingDate reportingDate;
    
    @Embedded
    private HtmlReportMetadata htmlMetadata;
    
    @Embedded
    private XbrlReportMetadata xbrlMetadata;
    
    // Quality-specific metadata
    private BigDecimal overallQualityScore;
    
    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;
    
    @Embedded
    private ProcessingTimestamps timestamps;
    
    @Enumerated(EnumType.STRING)
    private ReportStatus status;
    
    @Embedded
    private FailureReason failureReason;
    
    // Domain methods
    public static GeneratedReport createComprehensiveReport(
            BatchId batchId,
            BankId bankId,
            ReportingDate reportingDate,
            BigDecimal qualityScore,
            ComplianceStatus complianceStatus) {
        
        var report = new GeneratedReport();
        report.reportId = ReportId.generate();
        report.batchId = batchId;
        report.bankId = bankId;
        report.reportType = ReportType.COMPREHENSIVE;
        report.reportingDate = reportingDate;
        report.overallQualityScore = qualityScore;
        report.complianceStatus = complianceStatus;
        report.timestamps = ProcessingTimestamps.started();
        report.status = ReportStatus.IN_PROGRESS;
        
        // Register domain event
        report.registerEvent(new ReportGenerationStartedEvent(
            report.reportId, 
            report.batchId, 
            report.bankId
        ));
        
        return report;
    }
    
    public void markHtmlGenerated(HtmlReportMetadata htmlMetadata) {
        this.htmlMetadata = htmlMetadata;
        this.timestamps = this.timestamps.withHtmlCompleted();
        
        if (this.xbrlMetadata != null) {
            this.markCompleted();
        }
    }
    
    public void markXbrlGenerated(XbrlReportMetadata xbrlMetadata) {
        this.xbrlMetadata = xbrlMetadata;
        this.timestamps = this.timestamps.withXbrlCompleted();
        
        if (this.htmlMetadata != null) {
            this.markCompleted();
        }
    }
    
    private void markCompleted() {
        this.status = ReportStatus.COMPLETED;
        this.timestamps = this.timestamps.withCompleted();
        
        // Register domain event
        this.registerEvent(new ReportGeneratedEvent(
            this.reportId,
            this.batchId,
            this.bankId,
            this.reportType,
            this.reportingDate,
            this.htmlMetadata.getS3Uri(),
            this.xbrlMetadata.getS3Uri(),
            this.htmlMetadata.getPresignedUrl(),
            this.xbrlMetadata.getPresignedUrl(),
            this.htmlMetadata.getFileSize(),
            this.xbrlMetadata.getFileSize(),
            this.overallQualityScore,
            this.complianceStatus,
            this.timestamps.getGenerationDuration(),
            Instant.now()
        ));
    }
    
    public void markFailed(FailureReason reason) {
        this.status = ReportStatus.FAILED;
        this.failureReason = reason;
        this.timestamps = this.timestamps.withFailed();
        
        // Register domain event
        this.registerEvent(new ReportGenerationFailedEvent(
            this.reportId,
            this.batchId,
            this.bankId,
            reason,
            Instant.now()
        ));
    }
    
    public void markPartial(String reason) {
        this.status = ReportStatus.PARTIAL;
        this.failureReason = FailureReason.of(reason);
        this.timestamps = this.timestamps.withCompleted();
        
        // Still publish success event for partial reports
        this.registerEvent(new ReportGeneratedEvent(
            this.reportId,
            this.batchId,
            this.bankId,
            this.reportType,
            this.reportingDate,
            this.htmlMetadata != null ? this.htmlMetadata.getS3Uri() : null,
            this.xbrlMetadata != null ? this.xbrlMetadata.getS3Uri() : null,
            this.htmlMetadata != null ? this.htmlMetadata.getPresignedUrl() : null,
            this.xbrlMetadata != null ? this.xbrlMetadata.getPresignedUrl() : null,
            this.htmlMetadata != null ? this.htmlMetadata.getFileSize() : null,
            this.xbrlMetadata != null ? this.xbrlMetadata.getFileSize() : null,
            this.overallQualityScore,
            this.complianceStatus,
            this.timestamps.getGenerationDuration(),
            Instant.now()
        ));
    }
}
```


#### CalculationResults Domain Object

```java
public class CalculationResults {
    private final BatchId batchId;
    private final BankId bankId;
    private final String bankName;
    private final ReportingDate reportingDate;
    private final AmountEur tierOneCapital;
    private final Integer totalExposures;
    private final AmountEur totalAmount;
    private final Integer limitBreaches;
    private final List<CalculatedExposure> exposures;
    private final GeographicBreakdown geographicBreakdown;
    private final SectorBreakdown sectorBreakdown;
    private final ConcentrationIndices concentrationIndices;
    private final ProcessingTimestamps processingTimestamps;
    
    public List<CalculatedExposure> getLargeExposures() {
        return exposures.stream()
            .filter(exposure -> exposure.getPercentageOfCapital().getValue() >= 10.0)
            .collect(Collectors.toList());
    }
    
    public List<CalculatedExposure> getNonCompliantExposures() {
        return exposures.stream()
            .filter(exposure -> exposure.getPercentageOfCapital().getValue() > 25.0)
            .collect(Collectors.toList());
    }
}
```

#### QualityResults Domain Object

```java
public class QualityResults {
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant timestamp;
    private final Integer totalExposures;
    private final Integer validExposures;
    private final Integer totalErrors;
    private final Map<QualityDimension, BigDecimal> dimensionScores;
    private final List<Object> batchErrors;
    private final List<ExposureResult> exposureResults;
    
    // Computed properties
    private final BigDecimal overallScore;
    private final QualityGrade overallGrade;
    private final ComplianceStatus complianceStatus;
    private final AttentionLevel attentionLevel;
    
    public QualityResults(BatchId batchId, BankId bankId, Instant timestamp,
                         Integer totalExposures, Integer validExposures, Integer totalErrors,
                         Map<QualityDimension, BigDecimal> dimensionScores,
                         List<Object> batchErrors, List<ExposureResult> exposureResults) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.timestamp = timestamp;
        this.totalExposures = totalExposures;
        this.validExposures = validExposures;
        this.totalErrors = totalErrors;
        this.dimensionScores = dimensionScores;
        this.batchErrors = batchErrors;
        this.exposureResults = exposureResults;
        
        // Calculate derived values
        this.overallScore = calculateOverallScore();
        this.overallGrade = QualityGrade.fromScore(this.overallScore);
        this.complianceStatus = ComplianceStatus.fromScore(this.overallScore);
        this.attentionLevel = AttentionLevel.fromScore(this.overallScore);
    }
    
    private BigDecimal calculateOverallScore() {
        return dimensionScores.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(dimensionScores.size()), 2, RoundingMode.HALF_UP);
    }
    
    public Map<QualityDimension, ErrorDimensionSummary> getErrorDistributionByDimension() {
        Map<QualityDimension, ErrorDimensionSummary> distribution = new HashMap<>();
        
        for (ExposureResult result : exposureResults) {
            for (ValidationError error : result.getErrors()) {
                QualityDimension dimension = QualityDimension.valueOf(error.getDimension());
                ErrorDimensionSummary summary = distribution.computeIfAbsent(
                    dimension, k -> new ErrorDimensionSummary()
                );
                summary.incrementCount();
            }
        }
        
        // Calculate percentages
        distribution.values().forEach(summary -> {
            summary.setPercentage(
                new BigDecimal(summary.getCount())
                    .divide(new BigDecimal(totalErrors), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
            );
        });
        
        return distribution;
    }
    
    public List<Map.Entry<String, Long>> getTopErrorTypes(int limit) {
        return exposureResults.stream()
            .flatMap(result -> result.getErrors().stream())
            .collect(Collectors.groupingBy(
                ValidationError::getRuleCode,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

### Domain Services

#### ReportCoordinator

```java
@Component
public class ReportCoordinator {
    private final BatchEventTracker eventTracker;
    private final ComprehensiveReportOrchestrator reportOrchestrator;
    
    public void handleCalculationCompleted(BatchCalculationCompletedEvent event) {
        String batchId = event.getBatchId();
        
        eventTracker.markRiskComplete(batchId, event);
        
        if (eventTracker.areBothComplete(batchId)) {
            BatchEvents events = eventTracker.getBothEvents(batchId);
            reportOrchestrator.generateComprehensiveReport(
                events.getRiskEvent(),
                events.getQualityEvent()
            );
        }
    }
    
    public void handleQualityCompleted(BatchQualityCompletedEvent event) {
        String batchId = event.getBatchId();
        
        eventTracker.markQualityComplete(batchId, event);
        
        if (eventTracker.areBothComplete(batchId)) {
            BatchEvents events = eventTracker.getBothEvents(batchId);
            reportOrchestrator.generateComprehensiveReport(
                events.getRiskEvent(),
                events.getQualityEvent()
            );
        }
    }
}
```

#### BatchEventTracker

```java
@Component
public class BatchEventTracker {
    private final ConcurrentHashMap<String, BatchEvents> tracker = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    
    public BatchEventTracker() {
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
        // Schedule cleanup every 30 minutes
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEvents, 30, 30, TimeUnit.MINUTES);
    }
    
    public void markRiskComplete(String batchId, BatchCalculationCompletedEvent event) {
        BatchEvents events = tracker.computeIfAbsent(batchId, k -> new BatchEvents());
        events.setRiskEvent(event);
        events.setRiskComplete(true);
        if (events.getFirstEventTime() == null) {
            events.setFirstEventTime(Instant.now());
        }
    }
    
    public void markQualityComplete(String batchId, BatchQualityCompletedEvent event) {
        BatchEvents events = tracker.computeIfAbsent(batchId, k -> new BatchEvents());
        events.setQualityEvent(event);
        events.setQualityComplete(true);
        if (events.getFirstEventTime() == null) {
            events.setFirstEventTime(Instant.now());
        }
    }
    
    public boolean areBothComplete(String batchId) {
        BatchEvents events = tracker.get(batchId);
        return events != null && events.isRiskComplete() && events.isQualityComplete();
    }
    
    public BatchEvents getBothEvents(String batchId) {
        return tracker.get(batchId);
    }
    
    public void cleanup(String batchId) {
        tracker.remove(batchId);
    }
    
    private void cleanupExpiredEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        tracker.entrySet().removeIf(entry -> 
            entry.getValue().getFirstEventTime().isBefore(cutoff)
        );
    }
    
    public static class BatchEvents {
        private BatchCalculationCompletedEvent riskEvent;
        private BatchQualityCompletedEvent qualityEvent;
        private boolean riskComplete;
        private boolean qualityComplete;
        private Instant firstEventTime;
        
        // Getters and setters
    }
}
```


## Application Layer

### Orchestrators

#### ComprehensiveReportOrchestrator

```java
@Service
@Slf4j
public class ComprehensiveReportOrchestrator {
    private final ComprehensiveReportDataAggregator dataAggregator;
    private final QualityRecommendationsGenerator recommendationsGenerator;
    private final HtmlReportGenerator htmlGenerator;
    private final XbrlReportGenerator xbrlGenerator;
    private final IReportStorageService storageService;
    private final IGeneratedReportRepository reportRepository;
    private final BatchEventTracker eventTracker;
    private final MeterRegistry meterRegistry;
    
    @Async("reportGenerationExecutor")
    public CompletableFuture<Void> generateComprehensiveReport(
            BatchCalculationCompletedEvent riskEvent,
            BatchQualityCompletedEvent qualityEvent) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        String batchId = riskEvent.getBatchId();
        
        try {
            log.info("Starting comprehensive report generation for batch: {}", batchId);
            
            // Check for existing report
            if (reportRepository.existsByBatchIdAndStatus(batchId, ReportStatus.COMPLETED)) {
                log.info("Report already exists for batch: {}, skipping generation", batchId);
                return CompletableFuture.completedFuture(null);
            }
            
            // Step 1: Fetch and aggregate data from both sources
            ComprehensiveReportData reportData = dataAggregator.fetchAllData(riskEvent, qualityEvent);
            
            // Step 2: Create report aggregate
            GeneratedReport report = GeneratedReport.createComprehensiveReport(
                BatchId.of(batchId),
                BankId.of(reportData.getBankId()),
                ReportingDate.of(reportData.getReportingDate()),
                reportData.getQualityResults().getOverallScore(),
                reportData.getQualityResults().getComplianceStatus()
            );
            
            // Save initial report
            reportRepository.save(report);
            
            // Step 3: Generate quality recommendations
            List<RecommendationSection> recommendations = 
                recommendationsGenerator.generateRecommendations(reportData.getQualityResults());
            
            // Step 4: Generate HTML report (parallel with XBRL)
            CompletableFuture<HtmlReportMetadata> htmlFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return generateHtmlReport(reportData, recommendations);
                } catch (Exception e) {
                    log.error("HTML generation failed for batch: {}", batchId, e);
                    throw new RuntimeException("HTML generation failed", e);
                }
            });
            
            // Step 5: Generate XBRL report (parallel with HTML)
            CompletableFuture<XbrlReportMetadata> xbrlFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return generateXbrlReport(reportData.getCalculationResults());
                } catch (Exception e) {
                    log.error("XBRL generation failed for batch: {}", batchId, e);
                    throw new RuntimeException("XBRL generation failed", e);
                }
            });
            
            // Step 6: Wait for both generations to complete
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
            
            log.info("Comprehensive report generation completed for batch: {}", batchId);
            
            meterRegistry.counter("report.generation.comprehensive.success").increment();
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Comprehensive report generation failed for batch: {}", batchId, e);
            
            // Handle partial success
            handleGenerationFailure(batchId, e);
            
            meterRegistry.counter("report.generation.comprehensive.failure", 
                "failure_reason", e.getClass().getSimpleName()).increment();
            
            throw new RuntimeException("Report generation failed", e);
            
        } finally {
            sample.stop(Timer.builder("report.generation.comprehensive.duration")
                .register(meterRegistry));
        }
    }
    
    private HtmlReportMetadata generateHtmlReport(
            ComprehensiveReportData reportData, 
            List<RecommendationSection> recommendations) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Create comprehensive context combining both risk and quality data
            ComprehensiveReportContext context = ComprehensiveReportContext.builder()
                .calculationResults(reportData.getCalculationResults())
                .qualityResults(reportData.getQualityResults())
                .recommendations(recommendations)
                .reportMetadata(ReportMetadata.builder()
                    .reportId(ReportId.generate().getValue())
                    .batchId(reportData.getBatchId())
                    .bankId(reportData.getBankId())
                    .bankName(reportData.getBankName())
                    .reportingDate(reportData.getReportingDate())
                    .generatedAt(Instant.now())
                    .build())
                .build();
            
            // Generate HTML using Thymeleaf
            String htmlContent = htmlGenerator.generateComprehensiveReport(context);
            
            // Upload to S3
            String fileName = String.format("Comprehensive_Risk_Analysis_%s_%s.html", 
                reportData.getBankId(), 
                reportData.getReportingDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            S3Uri s3Uri = storageService.uploadHtmlReport(fileName, htmlContent);
            PresignedUrl presignedUrl = storageService.generatePresignedUrl(s3Uri, Duration.ofHours(1));
            
            return HtmlReportMetadata.builder()
                .s3Uri(s3Uri)
                .presignedUrl(presignedUrl)
                .fileSize(FileSize.of(htmlContent.getBytes(StandardCharsets.UTF_8).length))
                .contentType("text/html")
                .build();
                
        } finally {
            sample.stop(Timer.builder("report.generation.html.duration")
                .register(meterRegistry));
        }
    }
    
    private XbrlReportMetadata generateXbrlReport(CalculationResults calculationResults) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Generate XBRL using calculation data only
            String xbrlContent = xbrlGenerator.generateLargeExposuresXbrl(calculationResults);
            
            // Upload to S3
            String fileName = String.format("Large_Exposures_%s_%s.xml", 
                calculationResults.getBankId().getValue(), 
                calculationResults.getReportingDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            S3Uri s3Uri = storageService.uploadXbrlReport(fileName, xbrlContent);
            PresignedUrl presignedUrl = storageService.generatePresignedUrl(s3Uri, Duration.ofHours(1));
            
            return XbrlReportMetadata.builder()
                .s3Uri(s3Uri)
                .presignedUrl(presignedUrl)
                .fileSize(FileSize.of(xbrlContent.getBytes(StandardCharsets.UTF_8).length))
                .contentType("application/xml")
                .build();
                
        } finally {
            sample.stop(Timer.builder("report.generation.xbrl.duration")
                .register(meterRegistry));
        }
    }
    
    private void handleGenerationFailure(String batchId, Exception e) {
        try {
            Optional<GeneratedReport> reportOpt = reportRepository.findByBatchId(batchId);
            if (reportOpt.isPresent()) {
                GeneratedReport report = reportOpt.get();
                report.markFailed(FailureReason.of(e.getMessage()));
                reportRepository.save(report);
            }
        } catch (Exception saveException) {
            log.error("Failed to save error state for batch: {}", batchId, saveException);
        }
    }
}
```

### Quality Recommendations Generator

```java
@Service
@Slf4j
public class QualityRecommendationsGenerator {
    
    public List<RecommendationSection> generateRecommendations(QualityResults qualityResults) {
        List<RecommendationSection> recommendations = new ArrayList<>();
        
        // 1. Critical situation (if score < 60%)
        if (qualityResults.getOverallScore().compareTo(new BigDecimal("60")) < 0) {
            recommendations.add(generateCriticalSituationSection(qualityResults));
        }
        
        // 2. Dimension-specific issues
        recommendations.addAll(generateDimensionSpecificSections(qualityResults));
        
        // 3. Error pattern analysis (based on actual errors)
        recommendations.addAll(generateErrorPatternSections(qualityResults));
        
        // 4. Positive aspects (if any dimension >= 95%)
        generatePositiveAspectsSection(qualityResults)
            .ifPresent(recommendations::add);
        
        // 5. Action plan (always included)
        recommendations.add(generateActionPlanSection(qualityResults));
        
        // Limit to 6 sections for readability
        return recommendations.stream().limit(6).collect(Collectors.toList());
    }
    
    private RecommendationSection generateCriticalSituationSection(QualityResults qualityResults) {
        BigDecimal score = qualityResults.getOverallScore();
        int invalidCount = qualityResults.getTotalExposures() - qualityResults.getValidExposures();
        int totalCount = qualityResults.getTotalExposures();
        
        String content = String.format(
            "Il punteggio complessivo di qualità del <strong>%.1f%%</strong> indica una situazione critica. " +
            "<strong>%d esposizioni su %d</strong> presentano errori che richiedono azione immediata.",
            score, invalidCount, totalCount
        );
        
        List<String> bullets = Arrays.asList(
            "Bloccare l'invio della segnalazione fino alla risoluzione degli errori critici",
            "Convocare riunione urgente del team Data Quality",
            "Identificare e correggere immediatamente le esposizioni con errori CRITICAL",
            "Implementare controlli aggiuntivi nei processi di data entry"
        );
        
        return RecommendationSection.builder()
            .icon("🚨")
            .colorClass("red")
            .title("Situazione Critica - Azione Immediata Richiesta")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    private List<RecommendationSection> generateDimensionSpecificSections(QualityResults qualityResults) {
        List<RecommendationSection> sections = new ArrayList<>();
        
        for (Map.Entry<QualityDimension, BigDecimal> entry : qualityResults.getDimensionScores().entrySet()) {
            QualityDimension dimension = entry.getKey();
            BigDecimal score = entry.getValue();
            
            if (dimension.isCritical(score)) {
                sections.add(generateDimensionSection(dimension, score, qualityResults));
            }
        }
        
        return sections;
    }
    
    private RecommendationSection generateDimensionSection(
            QualityDimension dimension, 
            BigDecimal score, 
            QualityResults qualityResults) {
        
        switch (dimension) {
            case COMPLETENESS:
                return generateCompletenessSection(score, qualityResults);
            case ACCURACY:
                return generateAccuracySection(score, qualityResults);
            case CONSISTENCY:
                return generateConsistencySection(score, qualityResults);
            case TIMELINESS:
                return generateTimelinessSection(score, qualityResults);
            case UNIQUENESS:
                return generateUniquenessSection(score, qualityResults);
            case VALIDITY:
                return generateValiditySection(score, qualityResults);
            default:
                return generateGenericDimensionSection(dimension, score);
        }
    }
    
    // Additional helper methods for each dimension...
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Key Properties for Comprehensive Report Generation

Property 1: Dual event coordination waits for both events
*For any* batch ID, when only one event (either calculation or quality) has arrived, report generation should NOT be triggered; when BOTH events have arrived, report generation should be triggered exactly once
**Validates: Requirements 1.4, 1.5, 5.1**

Property 2: Comprehensive report includes both sections
*For any* generated HTML report, the content should include BOTH Large Exposures analysis section AND Data Quality analysis section with recommendations
**Validates: Requirements 5.3, 6.1, 8.1**

Property 3: Quality recommendations are contextual
*For any* quality results with specific error patterns, the generated recommendations should reference the actual error types found, not generic advice
**Validates: Requirements 9.1, 9.2, 9.3**

Property 4: Report metadata includes quality metrics
*For any* generated report database record, it should include overall_quality_score and compliance_status fields populated from quality results
**Validates: Requirements 13.4**

Property 5: Event tracker cleanup removes expired events
*For any* batch event older than 24 hours in the tracker, the cleanup job should remove it from memory
**Validates: Requirements 1.6**

Property 6: Parallel generation completes both formats
*For any* comprehensive report generation, HTML and XBRL generation should execute in parallel and both complete before marking report as COMPLETED
**Validates: Requirements 5.5, 16.2, 16.3**

Property 7: Data aggregator fetches both JSON files
*For any* report generation request, the data aggregator should fetch BOTH calculation JSON and quality JSON before proceeding
**Validates: Requirements 3.1, 3.2, 5.2**

Property 8: Quality score determines recommendation severity
*For any* quality results with score < 60%, a critical situation recommendation should be generated; for scores >= 60%, dimension-specific recommendations should be generated based on thresholds
**Validates: Requirements 9.1, 9.2**

Property 9: Report type is COMPREHENSIVE
*For any* generated report, the report_type field should be set to COMPREHENSIVE, not LARGE_EXPOSURES or DATA_QUALITY
**Validates: Requirements 5.1, 13.1**

Property 10: Idempotency prevents duplicate generation
*For any* batch ID with existing COMPLETED status, triggering report generation again should skip regeneration and return existing record
**Validates: Requirements 23.2**

## Error Handling

### Error Categories and Recovery Strategies

**Transient Errors** (Automatic Retry)
- Network timeouts during S3 operations
- Temporary S3 service unavailability
- Database connection timeouts
- Temporary file system issues

Recovery: EventRetryProcessor with exponential backoff (1s, 2s, 4s, 8s, 16s)

**Permanent Errors** (No Retry)
- S3 permission denied (403)
- S3 bucket not found (404)
- Invalid credentials
- Schema validation errors that cannot be auto-corrected

Recovery: Log CRITICAL error, alert operations team, mark status as FAILED

**Data Quality Errors** (Validation Failures)
- Malformed JSON
- Missing required fields
- Invalid checksums
- Schema violations

Recovery: Save invalid data for analysis, mark status as FAILED, alert data quality team

**Partial Failures** (Degraded Success)
- HTML generated but XBRL validation fails
- XBRL generated but HTML template rendering fails
- One file uploaded but other fails

Recovery: Mark status as PARTIAL, save successfully generated artifacts, alert for manual review

## Testing Strategy

### Unit Testing

**Framework**: JUnit 5 with Mockito
**Target Coverage**: ≥85% line coverage, ≥75% branch coverage

**Components to Test**:
- Domain entities (GeneratedReport aggregate)
- Value objects (ReportId, ReportStatus, ComplianceStatus)
- Domain services (ReportCoordinator, BatchEventTracker, QualityRecommendationsGenerator)
- Application services (ComprehensiveReportOrchestrator, ComprehensiveReportDataAggregator)
- Event listeners (ReportEventListener)
- Repository implementations (JpaGeneratedReportRepository)

**Test Patterns**:
- Mock external dependencies (S3, database, event bus)
- Test business logic in isolation
- Verify domain events are raised correctly
- Test error handling paths
- Verify idempotency logic

### Property-Based Testing

**Framework**: jqwik (Java property-based testing library)
**Configuration**: Minimum 100 iterations per property test

**Key Properties to Test**:
- Property 1: Dual event coordination (generate random event arrival orders)
- Property 3: Quality recommendations contextuality (generate random error patterns)
- Property 8: Quality score thresholds (generate random scores and verify recommendations)
- Property 10: Idempotency (generate random batch states)

**Generator Strategies**:
- Smart generators that constrain to valid input space
- Generate edge cases (empty lists, boundary values, null optionals)
- Generate realistic domain objects (valid quality scores 0-100, proper date ranges)

### Integration Testing

**Framework**: Spring Boot Test with Testcontainers
**Containers**: PostgreSQL, LocalStack (S3 emulation)

**Test Scenarios**:
1. **Happy Path**: Both events arrive, comprehensive report generates successfully, files uploaded, metadata saved
2. **Reverse Event Order**: Quality event arrives before calculation event
3. **Duplicate Events**: Same event arrives multiple times
4. **S3 Failure**: S3 unavailable, fallback to local filesystem
5. **Database Failure**: DB insert fails, compensating transaction creates fallback record
6. **Partial Generation**: HTML succeeds but XBRL validation fails
7. **Quality Recommendations**: Various quality scores generate appropriate recommendations
8. **Retry Success**: Failed event retries and succeeds
9. **Stale Event**: Event older than 24 hours is rejected

**Verification**:
- Database state matches expected
- S3 objects exist with correct metadata
- Domain events published to outbox
- Metrics emitted correctly
- Logs contain expected entries
- Recommendations match error patterns

## Design Decisions and Rationales

### Decision 1: Comprehensive Report Approach

**Decision**: Generate single comprehensive report combining Large Exposures and Data Quality analysis instead of separate reports

**Rationale**:
- Provides holistic view of both risk exposure and data reliability
- Reduces cognitive load for analysts (single report to review)
- Ensures quality context is always available when reviewing risk metrics
- Simplifies report distribution and archival

**Trade-offs**:
- Larger file sizes
- More complex HTML template
- Requires coordination of dual events
- Benefits: Better user experience, contextual analysis, single source of truth

### Decision 2: Dual Event Coordination

**Decision**: Wait for both BatchCalculationCompletedEvent and BatchQualityCompletedEvent before generating reports

**Rationale**:
- Reports should only be generated when both calculation and quality validation are complete
- Prevents generating reports from incomplete or invalid data
- Coordinator service maintains in-memory tracking of pending events
- Thread-safe with ConcurrentHashMap for concurrent event arrival

**Trade-offs**:
- Adds complexity with event coordination logic
- Requires memory for tracking pending events
- Benefits: Ensures data quality and completeness

### Decision 3: Dynamic Quality Recommendations

**Decision**: Generate contextual recommendations based on actual error patterns found in data

**Rationale**:
- Generic recommendations provide little actionable value
- Specific error types (e.g., "COMPLETENESS_MATURITY_MISSING") enable targeted fixes
- Error counts and percentages help prioritize remediation efforts
- Dimension-specific guidance addresses root causes

**Trade-offs**:
- More complex recommendation generation logic
- Requires analysis of error distribution
- Benefits: Actionable guidance, faster issue resolution, better data quality outcomes

### Decision 4: Quality Metadata in Report Aggregate

**Decision**: Include overallQualityScore and complianceStatus directly in GeneratedReport aggregate

**Rationale**:
- Enables quick filtering and querying of reports by quality level
- Supports dashboard views showing quality trends over time
- Allows alerting on low-quality report generation
- Provides audit trail of quality at report generation time

**Trade-offs**:
- Duplicates data from quality results JSON
- Adds fields to aggregate
- Benefits: Query performance, reporting capabilities, audit trail

### Decision 5: Parallel HTML and XBRL Generation

**Decision**: Generate HTML and XBRL reports in parallel using CompletableFuture

**Rationale**:
- Reduces total generation time by ~40%
- HTML and XBRL generation are independent operations
- Leverages multi-core processors effectively
- Improves user experience with faster report availability

**Trade-offs**:
- More complex error handling
- Requires thread pool management
- Benefits: Performance, scalability, user experience

## Module Structure

```
regtech-report-generation/
├── pom.xml (parent POM)
├── domain/
│   ├── pom.xml
│   └── src/main/java/.../reportgeneration/domain/
│       ├── generation/          # Report generation aggregates and logic
│       ├── coordination/        # Event coordination logic
│       ├── validation/          # XBRL and data validation
│       ├── storage/             # Storage abstractions
│       └── shared/              # Shared value objects and exceptions
├── application/
│   ├── pom.xml
│   └── src/main/java/.../reportgeneration/application/
│       ├── generation/          # Report generation commands and handlers
│       ├── coordination/        # Event coordination service
│       ├── integration/         # Event listeners and publishers
│       └── monitoring/          # Performance monitoring
├── infrastructure/
│   ├── pom.xml
│   └── src/main/java/.../reportgeneration/infrastructure/
│       ├── database/            # JPA repositories and entities
│       ├── filestorage/         # S3 and local file storage implementations
│       ├── templates/           # Thymeleaf configuration and templates
│       ├── xbrl/                # XBRL generation and validation
│       └── config/              # Spring configuration
└── presentation/
    ├── pom.xml
    └── src/main/java/.../reportgeneration/presentation/
        ├── health/              # Health check indicators
        └── monitoring/          # Metrics collectors
```

**Dependency Flow**: Domain ← Application ← Infrastructure ← Presentation

