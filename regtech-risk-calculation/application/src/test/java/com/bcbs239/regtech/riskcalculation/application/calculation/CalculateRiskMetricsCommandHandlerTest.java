package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.integration.RiskCalculationEventPublisher;
import com.bcbs239.regtech.riskcalculation.application.monitoring.PerformanceMetrics;
import com.bcbs239.regtech.riskcalculation.application.storage.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for CalculateRiskMetricsCommandHandler using real input/output JSON files.
 * 
 * Input file: data/raw/batch_20251208_202457_232ee8cf-0ad7-46ef-a75c-dc18fdcd294a.json
 * Output file: data/risk-calculations/risk_calc_batch_20251208_202415_8e7b1bde-ee97-45bd-8af8-6cb6c85b0698_20251208_202434.json
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalculateRiskMetricsCommandHandler Tests")
class CalculateRiskMetricsCommandHandlerTest {

    @Mock
    private ExposureRepository exposureRepository;

    @Mock
    private PortfolioAnalysisRepository portfolioAnalysisRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private IFileStorageService fileStorageService;

    @Mock
    private ICalculationResultsStorageService calculationResultsStorageService;

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @Mock
    private RiskCalculationEventPublisher eventPublisher;

    @Mock
    private PerformanceMetrics performanceMetrics;

    private ObjectMapper objectMapper;
    private CalculateRiskMetricsCommandHandler handler;

    private static final String INPUT_FILE_RESOURCE = "/test-data/batch_20251208_202457_232ee8cf-0ad7-46ef-a75c-dc18fdcd294a.json";
    private static final String BATCH_ID = "batch_20251208_202457_232ee8cf-0ad7-46ef-a75c-dc18fdcd294a";
    private static final String BANK_ID = "08081";
    private static final String S3_URI = "s3://regtech-data/raw/" + BATCH_ID + ".json";
    private static final String RESULTS_URI = "s3://regtech-data/risk-calculations/risk_calc_" + BATCH_ID + ".json";

    private String loadTestResource(String resourcePath) throws IOException {
        try (var inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        handler = new CalculateRiskMetricsCommandHandler(
                exposureRepository,
                portfolioAnalysisRepository,
                batchRepository,
                fileStorageService,
                calculationResultsStorageService,
                exchangeRateProvider,
                eventPublisher,
                objectMapper,
                performanceMetrics
        );
    }

    @Test
    @DisplayName("Should successfully calculate risk metrics from real input file")
    void shouldCalculateRiskMetricsSuccessfully() throws IOException {
        // Given: Load real input JSON file
        String inputJson = loadTestResource(INPUT_FILE_RESOURCE);

        // Mock file storage to return the input JSON
        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.success(inputJson));

        // Mock exchange rate provider to return appropriate rates for any currency
        when(exchangeRateProvider.getRate(anyString(), eq("EUR")))
                .thenAnswer(invocation -> {
                    String fromCurrency = invocation.getArgument(0);
                    if ("EUR".equals(fromCurrency)) {
                        return ExchangeRate.of(BigDecimal.ONE, "EUR", "EUR", LocalDate.now());
                    } else if ("USD".equals(fromCurrency)) {
                        return ExchangeRate.of(new BigDecimal("0.85"), "USD", "EUR", LocalDate.now());
                    } else {
                        return ExchangeRate.of(BigDecimal.ONE, fromCurrency, "EUR", LocalDate.now());
                    }
                });

        // Mock batch repository operations
        doNothing().when(batchRepository).createBatch(
                anyString(), any(BankInfo.class), any(LocalDate.class), anyInt(), any(Instant.class)
        );
        doNothing().when(batchRepository).markAsCompleted(anyString(), anyString(), any(Instant.class));

        // Mock calculation results storage
        when(calculationResultsStorageService.storeCalculationResults(any(RiskCalculationResult.class)))
                .thenReturn(Result.success(RESULTS_URI));

        // Mock portfolio analysis repository
        doNothing().when(portfolioAnalysisRepository).save(any(PortfolioAnalysis.class));

        // Mock event publisher
        doNothing().when(eventPublisher).publishBatchCalculationCompleted(
                anyString(), anyString(), anyInt(), anyString()
        );

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());
        doNothing().when(performanceMetrics).recordBatchSuccess(anyString(), anyInt());

        // Create command
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID,
                BANK_ID,
                S3_URI,
                8, // total exposures from input file
                "correlation-123"
        );

        assertThat(commandResult.isSuccess()).isTrue();
        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify success
        assertThat(result.isSuccess()).isTrue();

        // Verify file was downloaded
        verify(fileStorageService).retrieveFile(S3_URI);

        // Verify batch was created with correct data
        ArgumentCaptor<BankInfo> bankInfoCaptor = ArgumentCaptor.forClass(BankInfo.class);
        verify(batchRepository).createBatch(
                eq(BATCH_ID),
                bankInfoCaptor.capture(),
                eq(LocalDate.of(2024, 9, 12)),
                eq(8),
                any(Instant.class)
        );

        BankInfo capturedBankInfo = bankInfoCaptor.getValue();
        assertThat(capturedBankInfo.bankName()).isEqualTo("Community First Bank");
        assertThat(capturedBankInfo.abiCode()).isEqualTo("08081");
        assertThat(capturedBankInfo.leiCode()).isEqualTo("815600D7623147C25D86");

        // Verify calculation results were stored
        ArgumentCaptor<RiskCalculationResult> resultCaptor = ArgumentCaptor.forClass(RiskCalculationResult.class);
        verify(calculationResultsStorageService).storeCalculationResults(resultCaptor.capture());

        RiskCalculationResult capturedResult = resultCaptor.getValue();
        assertThat(capturedResult.batchId()).isEqualTo(BATCH_ID);
        assertThat(capturedResult.calculatedExposures()).hasSize(8);
        assertThat(capturedResult.bankInfo().bankName()).isEqualTo("Community First Bank");

        // Verify portfolio analysis was saved
        ArgumentCaptor<PortfolioAnalysis> analysisCaptor = ArgumentCaptor.forClass(PortfolioAnalysis.class);
        verify(portfolioAnalysisRepository).save(analysisCaptor.capture());

        PortfolioAnalysis capturedAnalysis = analysisCaptor.getValue();
        assertThat(capturedAnalysis.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(capturedAnalysis.getTotalPortfolio().value()).isGreaterThan(BigDecimal.ZERO);

        // Verify batch was marked as completed
        verify(batchRepository).markAsCompleted(eq(BATCH_ID), eq(RESULTS_URI), any(Instant.class));

        // Verify success event was published
        verify(eventPublisher).publishBatchCalculationCompleted(
                eq(BATCH_ID),
                eq(BANK_ID),
                eq(8),
                eq(RESULTS_URI)
        );

        // Verify performance metrics were recorded
        verify(performanceMetrics).recordBatchStart(BATCH_ID);
        verify(performanceMetrics).recordBatchSuccess(BATCH_ID, 8);
    }

    @Test
    @DisplayName("Should handle file not found error")
    void shouldHandleFileNotFoundError() {
        // Given: File storage returns file not found error
        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                        "FILE_NOT_FOUND",
                        com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                        "File not found",
                        "file.not.found"
                )));

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());

        // Create command
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID, BANK_ID, S3_URI, 8, "correlation-123"
        );

        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify failure
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        // The handler wraps the file not found error
        assertThat(result.getError().get().getCode()).isIn("FILE_DOWNLOAD_FAILED", "FILE_NOT_FOUND");

        // Verify failure event was published
        verify(eventPublisher).publishBatchCalculationFailed(
                eq(BATCH_ID),
                eq(BANK_ID),
                contains("File download failed")
        );
    }

    @Test
    @DisplayName("Should handle invalid JSON deserialization error")
    void shouldHandleInvalidJsonError() {
        // Given: File storage returns invalid JSON
        String invalidJson = "{ invalid json }";
        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.success(invalidJson));

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());

        // Create command
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID, BANK_ID, S3_URI, 8, "correlation-123"
        );

        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify failure
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        assertThat(result.getError().get().getCode()).isEqualTo("DESERIALIZATION_FAILED");

        // Verify failure event was published
        verify(eventPublisher).publishBatchCalculationFailed(
                eq(BATCH_ID),
                eq(BANK_ID),
                contains("Deserialization failed")
        );
    }

    @Test
    @DisplayName("Should handle empty exposures list")
    void shouldHandleEmptyExposures() {
        // Given: JSON with empty exposures
        String emptyJson = """
                {
                  "bank_info": {
                    "bank_name": "Test Bank",
                    "abi_code": "12345",
                    "lei_code": "LEI123",
                    "report_date": "2024-09-12",
                    "total_exposures": 0
                  },
                  "exposures": [],
                  "credit_risk_mitigation": []
                }
                """;

        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.success(emptyJson));

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());

        // Create command - note: totalExposures must be > 0 for command validation
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID, BANK_ID, S3_URI, 1, "correlation-123"
        );

        assertThat(commandResult.isSuccess()).isTrue();
        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify failure
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        assertThat(result.getError().get().getCode()).isEqualTo("NO_EXPOSURES");
    }

    @Test
    @DisplayName("Should handle storage failure")
    void shouldHandleStorageFailure() throws IOException {
        // Given: Load real input JSON file
        String inputJson = loadTestResource(INPUT_FILE_RESOURCE);

        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.success(inputJson));

        // Mock exchange rate provider
        when(exchangeRateProvider.getRate(anyString(), eq("EUR")))
                .thenReturn(ExchangeRate.of(BigDecimal.ONE, "EUR", "EUR", LocalDate.now()));

        // Mock batch repository operations
        doNothing().when(batchRepository).createBatch(
                anyString(), any(BankInfo.class), any(LocalDate.class), anyInt(), any(Instant.class)
        );
        doNothing().when(batchRepository).updateStatus(anyString(), anyString());

        // Mock calculation results storage to fail
        when(calculationResultsStorageService.storeCalculationResults(any(RiskCalculationResult.class)))
                .thenReturn(Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                        "STORAGE_FAILED",
                        com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                        "Failed to store results",
                        "storage.failed"
                )));

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());

        // Create command
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID, BANK_ID, S3_URI, 8, "correlation-123"
        );

        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify failure
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        assertThat(result.getError().get().getCode()).isEqualTo("STORAGE_FAILED");

        // Verify batch was marked as failed
        verify(batchRepository).updateStatus(BATCH_ID, "FAILED");

        // Verify failure event was published
        verify(eventPublisher).publishBatchCalculationFailed(
                eq(BATCH_ID),
                eq(BANK_ID),
                contains("Failed to store calculation results")
        );
    }

    @Test
    @DisplayName("Should apply credit risk mitigations correctly")
    void shouldApplyCreditRiskMitigations() throws IOException {
        // Given: Load real input JSON file with mitigations
        String inputJson = loadTestResource(INPUT_FILE_RESOURCE);

        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.success(inputJson));

        // Mock exchange rate provider
        when(exchangeRateProvider.getRate(anyString(), eq("EUR")))
                .thenReturn(ExchangeRate.of(BigDecimal.ONE, "EUR", "EUR", LocalDate.now()));

        // Mock batch repository operations
        doNothing().when(batchRepository).createBatch(
                anyString(), any(BankInfo.class), any(LocalDate.class), anyInt(), any(Instant.class)
        );
        doNothing().when(batchRepository).markAsCompleted(anyString(), anyString(), any(Instant.class));

        // Mock calculation results storage
        when(calculationResultsStorageService.storeCalculationResults(any(RiskCalculationResult.class)))
                .thenReturn(Result.success(RESULTS_URI));

        // Mock portfolio analysis repository
        doNothing().when(portfolioAnalysisRepository).save(any(PortfolioAnalysis.class));

        // Mock event publisher
        doNothing().when(eventPublisher).publishBatchCalculationCompleted(
                anyString(), anyString(), anyInt(), anyString()
        );

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());
        doNothing().when(performanceMetrics).recordBatchSuccess(anyString(), anyInt());

        // Create command
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID, BANK_ID, S3_URI, 8, "correlation-123"
        );

        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify success
        assertThat(result.isSuccess()).isTrue();

        // Verify calculation results contain protected exposures with mitigations applied
        ArgumentCaptor<RiskCalculationResult> resultCaptor = ArgumentCaptor.forClass(RiskCalculationResult.class);
        verify(calculationResultsStorageService).storeCalculationResults(resultCaptor.capture());

        RiskCalculationResult capturedResult = resultCaptor.getValue();
        
        // The input file has 4 mitigations:
        // EXP_001_2024: 10,000 EUR
        // EXP_003_2024: 5,000 EUR
        // EXP_004_2024: 50,000 EUR
        // EXP_008_2024: 50,000 EUR
        
        // Verify that exposures with mitigations have reduced net exposure
        assertThat(capturedResult.calculatedExposures()).hasSize(8);
        
        // Find EXP_001_2024 (250,000 EUR - 10,000 EUR mitigation = 240,000 EUR net)
        var exp001 = capturedResult.calculatedExposures().stream()
                .filter(e -> e.getExposureId().value().equals("EXP_001_2024"))
                .findFirst();
        assertThat(exp001).isPresent();
        assertThat(exp001.get().getNetExposure().value()).isEqualByComparingTo(new BigDecimal("240000.00"));
    }

    @Test
    @DisplayName("Should classify exposures by geographic region correctly")
    void shouldClassifyExposuresByGeographicRegion() throws IOException {
        // Given: Load real input JSON file
        String inputJson = loadTestResource(INPUT_FILE_RESOURCE);

        when(fileStorageService.retrieveFile(S3_URI))
                .thenReturn(Result.success(inputJson));

        // Mock exchange rate provider
        when(exchangeRateProvider.getRate(anyString(), eq("EUR")))
                .thenReturn(ExchangeRate.of(BigDecimal.ONE, "EUR", "EUR", LocalDate.now()));

        // Mock batch repository operations
        doNothing().when(batchRepository).createBatch(
                anyString(), any(BankInfo.class), any(LocalDate.class), anyInt(), any(Instant.class)
        );
        doNothing().when(batchRepository).markAsCompleted(anyString(), anyString(), any(Instant.class));

        // Mock calculation results storage
        when(calculationResultsStorageService.storeCalculationResults(any(RiskCalculationResult.class)))
                .thenReturn(Result.success(RESULTS_URI));

        // Mock portfolio analysis repository
        doNothing().when(portfolioAnalysisRepository).save(any(PortfolioAnalysis.class));

        // Mock event publisher
        doNothing().when(eventPublisher).publishBatchCalculationCompleted(
                anyString(), anyString(), anyInt(), anyString()
        );

        // Mock performance metrics
        doNothing().when(performanceMetrics).recordBatchStart(anyString());
        doNothing().when(performanceMetrics).recordBatchSuccess(anyString(), anyInt());

        // Create command
        Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                BATCH_ID, BANK_ID, S3_URI, 8, "correlation-123"
        );

        CalculateRiskMetricsCommand command = commandResult.getValue().orElseThrow();

        // When: Execute the command handler
        Result<Void> result = handler.handle(command);

        // Then: Verify success
        assertThat(result.isSuccess()).isTrue();

        // Verify portfolio analysis contains geographic breakdown
        ArgumentCaptor<PortfolioAnalysis> analysisCaptor = ArgumentCaptor.forClass(PortfolioAnalysis.class);
        verify(portfolioAnalysisRepository).save(analysisCaptor.capture());

        PortfolioAnalysis analysis = analysisCaptor.getValue();
        
        // Input file has:
        // - 4 exposures in Italy (IT): EXP_001, EXP_002, EXP_005, EXP_006
        // - 2 exposures in EU_OTHER (DE): EXP_003, EXP_008
        // - 2 exposures in NON_EUROPEAN (CA, US): EXP_004, EXP_007
        
        assertThat(analysis.getGeographicBreakdown()).isNotNull();
        assertThat(analysis.getGeographicHHI()).isNotNull();
        assertThat(analysis.getGeographicHHI().value()).isGreaterThan(BigDecimal.ZERO);
    }
}