package com.bcbs239.regtech.riskcalculation.application.batch;

import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculateRiskMetricsCommand;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculateRiskMetricsCommandHandler;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component("riskCalculationProcessBatchCompletedUseCase")
@RequiredArgsConstructor
public class ProcessBatchCompletedUseCase {
    private final CalculateRiskMetricsCommandHandler handler;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;


    public void process(BatchCompletedInboundEvent event) {
        if (!event.isValid()) return;
        BatchId batchId = BatchId.of(event.getBatchId());
        if (portfolioAnalysisRepository.findByBatchId(batchId.value()).isPresent()) {
            return;
        }
        handle(event);
    }

    @Async("riskCalculationTaskExecutor")
    private void handle(BatchCompletedInboundEvent event) {
        Result<CalculateRiskMetricsCommand> commandResult = createValidationCommand(event);
        if (commandResult.isSuccess()){
            handler.handle(commandResult.getValueOrThrow());
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
