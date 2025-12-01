package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.services.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.application.integration.RiskCalculationEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for CalculateRiskMetricsCommandHandler that verifies:
 * 1. Reading real data from file
 * 2. Performing risk calculations
 * 3. Storing results in filesystem (JSON)
 * 4. Storing summary in database
 * 5. Dual storage strategy with file references
 */
class CalculateRiskMetricsCommandHandlerIntegrationTest {

    @TempDir
    Path tempDir;

    private CalculateRiskMetricsCommandHandler commandHandler;
    private ICalculationResultsStorageService calculationResultsStorageService;
    private CalculationResultsJsonSerializer jsonSerializer;
    private BatchSummaryRepository batchSummaryRepository;
    private RiskCalculationEventPublisher eventPublisher;
    private ExchangeRateProvider exchangeRateProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Create real instances
        objectMapper = new ObjectMapper();
        jsonSerializer = new CalculationResultsJsonSerializer(objectMapper);
        
        // Mock dependencies
        calculationResultsStorageService = mock(ICalculationResultsStorageService.class);
        batchSummaryRepository = mock(BatchSummaryRepository.class);
        eventPublisher = mock(RiskCalculationEventPublisher.class);
        exchangeRateProvider = mock(ExchangeRateProvider.class);
        
        // Mock exchange rate provider to return 1.0 for EUR (no conversion needed)
        when(exchangeRateProvider.getExchangeRate(any(), any())).thenReturn(1.0);
        
        // Create command handler
        commandHandler = new CalculateRiskMetricsCommandHandler(
            null, // fileStorageService not needed for this test
            exchangeRateProvider,
            calculationResultsStorageService,
            jsonSerializer,
            batchSummaryRepository,
            eventPublisher
        );
    }

    @Test
    void shouldCalculateRiskMetricsAndStoreBothInDatabaseAndFilesystem() throws Exception {
        // Given: Real data file
        String inputFilePath = "data/raw/batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json";
        String batchId = "batch_20251127_164816";
        String s3Uri = "file:///" + inputFilePath;
        
        // Verify the file exists
        Path dataFile = Paths.get(inputFilePath);
        assertThat(dataFile).exists();
        
        // Read and parse the file to verify structure
        String jsonContent = Files.readString(dataFile);
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        
        assertThat(rootNode.has("bank_info")).isTrue();
        assertThat(rootNode.has("exposures")).isTrue();
        assertThat(rootNode.get("exposures").isArray()).isTrue();
        
        // Mock the storage service to simulate successful file storage
        String outputFileUri = tempDir.resolve("calculated/calc_" + batchId + ".json").toString();
        when(calculationResultsStorageService.storeCalculationResults(any(), eq(batchId), any()))
            .thenAnswer(invocation -> {
                String json = invocation.getArgument(0);
                // Verify JSON is valid
                JsonNode resultNode = objectMapper.readTree(json);
                assertThat(resultNode.has("batch_id")).isTrue();
                assertThat(resultNode.has("summary")).isTrue();
                assertThat(resultNode.has("calculated_exposures")).isTrue();
                
                // Write to temp file for verification
                Files.createDirectories(tempDir.resolve("calculated"));
                Files.writeString(tempDir.resolve("calculated/calc_" + batchId + ".json"), json);
                
                return Result.success(FileStorageUri.of(outputFileUri));
            });
        
        // Create command
        CalculateRiskMetricsCommand command = new CalculateRiskMetricsCommand(batchId, s3Uri);
        
        // When: Execute the command
        RiskCalculationResult result = commandHandler.handle(command);
        
        // Then: Verify calculation results
        assertThat(result).isNotNull();
        assertThat(result.getBatchId()).isEqualTo(batchId);
        assertThat(result.getBankInfo()).isNotNull();
        assertThat(result.getBankInfo().getBankId()).isEqualTo("08081");
        assertThat(result.getBankInfo().getBankName()).isEqualTo("Community First Bank");
        
        // Verify exposures were processed
        assertThat(result.getCalculatedExposures()).isNotEmpty();
        assertThat(result.getCalculatedExposures().size()).isEqualTo(8);
        
        // Verify portfolio analysis
        assertThat(result.getPortfolioAnalysis()).isNotNull();
        assertThat(result.getPortfolioAnalysis().getTotalAmount()).isNotNull();
        assertThat(result.getPortfolioAnalysis().getTotalAmount().getValue()).isGreaterThan(0);
        
        // Verify geographic breakdown
        assertThat(result.getPortfolioAnalysis().getGeographicBreakdown()).isNotNull();
        assertThat(result.getPortfolioAnalysis().getGeographicBreakdown().getShares()).isNotEmpty();
        
        // Verify sector breakdown
        assertThat(result.getPortfolioAnalysis().getSectorBreakdown()).isNotNull();
        assertThat(result.getPortfolioAnalysis().getSectorBreakdown().getShares()).isNotEmpty();
        
        // Verify HHI indices
        assertThat(result.getPortfolioAnalysis().getGeographicHHI()).isNotNull();
        assertThat(result.getPortfolioAnalysis().getSectorHHI()).isNotNull();
        
        // Verify JSON serialization was called
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(calculationResultsStorageService).storeCalculationResults(
            jsonCaptor.capture(), 
            eq(batchId), 
            eq("08081")
        );
        
        // Verify the serialized JSON structure
        String serializedJson = jsonCaptor.getValue();
        JsonNode serializedNode = objectMapper.readTree(serializedJson);
        
        assertThat(serializedNode.get("batch_id").asText()).isEqualTo(batchId);
        assertThat(serializedNode.get("bank_id").asText()).isEqualTo("08081");
        assertThat(serializedNode.get("bank_name").asText()).isEqualTo("Community First Bank");
        assertThat(serializedNode.has("calculated_at")).isTrue();
        
        // Verify summary section
        JsonNode summaryNode = serializedNode.get("summary");
        assertThat(summaryNode).isNotNull();
        assertThat(summaryNode.get("total_exposures").asInt()).isEqualTo(8);
        assertThat(summaryNode.get("total_amount_eur").asDouble()).isGreaterThan(0);
        assertThat(summaryNode.has("geographic_breakdown")).isTrue();
        assertThat(summaryNode.has("sector_breakdown")).isTrue();
        assertThat(summaryNode.has("concentration_indices")).isTrue();
        
        // Verify calculated exposures section
        JsonNode exposuresNode = serializedNode.get("calculated_exposures");
        assertThat(exposuresNode).isNotNull();
        assertThat(exposuresNode.isArray()).isTrue();
        assertThat(exposuresNode.size()).isEqualTo(8);
        
        // Verify first exposure has required fields
        JsonNode firstExposure = exposuresNode.get(0);
        assertThat(firstExposure.has("instrument_id")).isTrue();
        assertThat(firstExposure.has("counterparty_ref")).isTrue();
        assertThat(firstExposure.has("original_amount")).isTrue();
        assertThat(firstExposure.has("original_currency")).isTrue();
        assertThat(firstExposure.has("eur_amount")).isTrue();
        assertThat(firstExposure.has("mitigated_amount_eur")).isTrue();
        assertThat(firstExposure.has("geographic_region")).isTrue();
        assertThat(firstExposure.has("economic_sector")).isTrue();
        
        // Verify batch summary was saved to database
        ArgumentCaptor<BatchSummary> summaryCaptor = ArgumentCaptor.forClass(BatchSummary.class);
        verify(batchSummaryRepository).save(summaryCaptor.capture());
        
        BatchSummary savedSummary = summaryCaptor.getValue();
        assertThat(savedSummary).isNotNull();
        assertThat(savedSummary.getBatchId()).isEqualTo(batchId);
        assertThat(savedSummary.getBankId()).isEqualTo("08081");
        assertThat(savedSummary.getTotalExposures()).isEqualTo(8);
        assertThat(savedSummary.getTotalAmountEur().getValue()).isGreaterThan(0);
        
        // Verify dual file references
        assertThat(savedSummary.getInputFileUri()).isNotNull();
        assertThat(savedSummary.getInputFileUri().uri()).isEqualTo(s3Uri);
        assertThat(savedSummary.getOutputFileUri()).isNotNull();
        assertThat(savedSummary.getOutputFileUri().uri()).isEqualTo(outputFileUri);
        
        // Verify geographic breakdown in summary
        assertThat(savedSummary.getGeographicBreakdown()).isNotNull();
        assertThat(savedSummary.getGeographicBreakdown().getShares()).isNotEmpty();
        
        // Verify sector breakdown in summary
        assertThat(savedSummary.getSectorBreakdown()).isNotNull();
        assertThat(savedSummary.getSectorBreakdown().getShares()).isNotEmpty();
        
        // Verify HHI indices in summary
        assertThat(savedSummary.getHerfindahlGeographic()).isNotNull();
        assertThat(savedSummary.getHerfindahlSector()).isNotNull();
        
        // Verify status
        assertThat(savedSummary.getStatus().isCompleted()).isTrue();
        
        // Verify event was published
        verify(eventPublisher).publishBatchCalculationCompleted(any());
        
        // Verify the output file was created in temp directory
        Path outputFile = tempDir.resolve("calculated/calc_" + batchId + ".json");
        assertThat(outputFile).exists();
        
        // Verify output file content
        String outputContent = Files.readString(outputFile);
        JsonNode outputNode = objectMapper.readTree(outputContent);
        assertThat(outputNode.get("batch_id").asText()).isEqualTo(batchId);
        assertThat(outputNode.get("calculated_exposures").size()).isEqualTo(8);
    }

    @Test
    void shouldHandleGeographicClassificationCorrectly() throws Exception {
        // Given: Real data file with mixed geographic locations
        String inputFilePath = "data/raw/batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json";
        String batchId = "batch_test_geographic";
        String s3Uri = "file:///" + inputFilePath;
        
        // Mock storage
        when(calculationResultsStorageService.storeCalculationResults(any(), any(), any()))
            .thenReturn(Result.success(FileStorageUri.of("test://output.json")));
        
        // Create command
        CalculateRiskMetricsCommand command = new CalculateRiskMetricsCommand(batchId, s3Uri);
        
        // When: Execute the command
        RiskCalculationResult result = commandHandler.handle(command);
        
        // Then: Verify geographic classification
        assertThat(result.getPortfolioAnalysis().getGeographicBreakdown().getShares())
            .containsKeys("ITALY", "EU", "NON_EU");
        
        // Verify percentages sum to 100%
        double totalPercentage = result.getPortfolioAnalysis().getGeographicBreakdown().getShares().values()
            .stream()
            .mapToDouble(share -> share.getPercentage().getValue())
            .sum();
        assertThat(totalPercentage).isCloseTo(100.0, within(0.01));
    }

    @Test
    void shouldHandleSectorClassificationCorrectly() throws Exception {
        // Given: Real data file with mixed sectors
        String inputFilePath = "data/raw/batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json";
        String batchId = "batch_test_sector";
        String s3Uri = "file:///" + inputFilePath;
        
        // Mock storage
        when(calculationResultsStorageService.storeCalculationResults(any(), any(), any()))
            .thenReturn(Result.success(FileStorageUri.of("test://output.json")));
        
        // Create command
        CalculateRiskMetricsCommand command = new CalculateRiskMetricsCommand(batchId, s3Uri);
        
        // When: Execute the command
        RiskCalculationResult result = commandHandler.handle(command);
        
        // Then: Verify sector classification
        assertThat(result.getPortfolioAnalysis().getSectorBreakdown().getShares())
            .isNotEmpty();
        
        // Verify percentages sum to 100%
        double totalPercentage = result.getPortfolioAnalysis().getSectorBreakdown().getShares().values()
            .stream()
            .mapToDouble(share -> share.getPercentage().getValue())
            .sum();
        assertThat(totalPercentage).isCloseTo(100.0, within(0.01));
    }

    @Test
    void shouldCalculateHHIIndicesCorrectly() throws Exception {
        // Given: Real data file
        String inputFilePath = "data/raw/batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json";
        String batchId = "batch_test_hhi";
        String s3Uri = "file:///" + inputFilePath;
        
        // Mock storage
        when(calculationResultsStorageService.storeCalculationResults(any(), any(), any()))
            .thenReturn(Result.success(FileStorageUri.of("test://output.json")));
        
        // Create command
        CalculateRiskMetricsCommand command = new CalculateRiskMetricsCommand(batchId, s3Uri);
        
        // When: Execute the command
        RiskCalculationResult result = commandHandler.handle(command);
        
        // Then: Verify HHI indices are within valid range [0, 1]
        assertThat(result.getPortfolioAnalysis().getGeographicHHI().getValue())
            .isBetween(0.0, 1.0);
        assertThat(result.getPortfolioAnalysis().getSectorHHI().getValue())
            .isBetween(0.0, 1.0);
    }
}
