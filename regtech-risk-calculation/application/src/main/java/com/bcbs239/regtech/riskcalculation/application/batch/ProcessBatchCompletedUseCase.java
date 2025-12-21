package com.bcbs239.regtech.riskcalculation.application.batch;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculateRiskMetricsCommand;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculateRiskMetricsCommandHandler;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("riskCalculationProcessBatchCompletedUseCase")
@RequiredArgsConstructor
@Slf4j
public class ProcessBatchCompletedUseCase {
    private final CalculateRiskMetricsCommandHandler handler;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;


    public void process(BatchCompletedInboundEvent event) {
        if (event == null) {
            log.warn("RiskCalculation: received null BatchCompletedInboundEvent");
            return;
        }
        if (!event.isValid()) {
            log.warn("RiskCalculation: invalid BatchCompletedInboundEvent, skipping. batchId={}, bankId={}, totalExposures={}",
                    event.getBatchId(), event.getBankId(), event.getTotalExposures());
            return;
        }

        // Do not gate processing on event.getTotalExposures().
        // The risk-calculation handler parses the batch file and uses the actual number of exposures.

        BatchId batchId = BatchId.of(event.getBatchId());
        if (portfolioAnalysisRepository.findByBatchId(batchId.value()).isPresent()) {
            log.info("RiskCalculation: batch {} already processed (portfolio analysis exists), skipping", event.getBatchId());
            return;
        }
        handle(event);
    }

    @Async("riskCalculationTaskExecutor")
    private void handle(BatchCompletedInboundEvent event) {
        Result<CalculateRiskMetricsCommand> commandResult = createValidationCommand(event);
        if (commandResult.isFailure()) {
            log.warn("RiskCalculation: failed to create CalculateRiskMetricsCommand for batch {}: {}",
                    event.getBatchId(),
                    commandResult.getError().map(e -> e.getCode() + ": " + e.getMessage()).orElse("unknown error"));
            return;
        }

        Result<Void> result = handler.handle(commandResult.getValueOrThrow());
        if (result.isFailure()) {
            log.error("RiskCalculation: handler failed for batch {}: {}",
                    event.getBatchId(),
                    result.getError().map(e -> e.getCode() + ": " + e.getMessage()).orElse("unknown error"));
        }
    }

    private Result<CalculateRiskMetricsCommand> createValidationCommand(BatchCompletedInboundEvent event) {
        String correlationId = event.getCorrelationId();

        return CalculateRiskMetricsCommand.create(
                event.getBatchId(),
                event.getBankId(),
                event.getS3Uri(),
                event.getTotalExposures(),
                correlationId
        );
    }
}
