package com.bcbs239.regtech.billing.infrastructure.jobs;

import com.bcbs239.regtech.billing.domain.dunning.DunningCase;
import com.bcbs239.regtech.billing.domain.dunning.DunningCaseId;
import com.bcbs239.regtech.billing.domain.dunning.DunningCaseStatus;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaDunningCaseRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Scheduled job for processing dunning cases and overdue invoice management.
 * Handles automatic dunning step execution and overdue invoice detection.
 */
@Component
public class DunningProcessScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DunningProcessScheduler.class);

    private final JpaInvoiceRepository invoiceRepository;
    private final JpaDunningCaseRepository dunningCaseRepository;
    private final DunningActionExecutor dunningActionExecutor;
    private final Executor dunningExecutor;

    public DunningProcessScheduler(
            JpaInvoiceRepository invoiceRepository,
            JpaDunningCaseRepository dunningCaseRepository,
            DunningActionExecutor dunningActionExecutor) {
        this.invoiceRepository = invoiceRepository;
        this.dunningCaseRepository = dunningCaseRepository;
        this.dunningActionExecutor = dunningActionExecutor;
        this.dunningExecutor = Executors.newFixedThreadPool(5); // Configurable thread pool
    }

    /**
     * Scheduled job that runs daily at 09:00 UTC to process dunning cases.
     * Detects overdue invoices and executes pending dunning actions.
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "UTC")
    public void processDunningCases() {
        logger.info("Starting scheduled dunning process execution");

        try {
            DunningProcessResult result = executeDunningProcess();

            logger.info("Dunning process completed: {} new cases created, {} actions executed, {} failures",
                result.newCasesCreated(), result.actionsExecuted(), result.failures());

            if (result.hasFailures()) {
                logger.warn("Dunning process had {} failures", result.failures());
            }

        } catch (Exception e) {
            logger.error("Unexpected error during scheduled dunning process: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for dunning process (useful for testing or manual runs).
     */
    public DunningProcessResult triggerDunningProcess() {
        logger.info("Manually triggering dunning process");

        try {
            return executeDunningProcess();
        } catch (Exception e) {
            logger.error("Unexpected error during manual dunning process: {}", e.getMessage(), e);
            return new DunningProcessResult(0, 0, 1);
        }
    }

    /**
     * Execute the complete dunning process: detect overdue invoices and process existing cases.
     */
    @Transactional
    private DunningProcessResult executeDunningProcess() {
        int newCasesCreated = 0;
        int actionsExecuted = 0;
        int failures = 0;

        // Step 1: Detect overdue invoices and create new dunning cases
        try {
            int newCases = createDunningCasesForOverdueInvoices();
            newCasesCreated += newCases;
            logger.info("Created {} new dunning cases for overdue invoices", newCases);
        } catch (Exception e) {
            logger.error("Failed to create dunning cases for overdue invoices: {}", e.getMessage(), e);
            failures++;
        }

        // Step 2: Process existing dunning cases that are ready for action
        try {
            int processedActions = processReadyDunningCases();
            actionsExecuted += processedActions;
            logger.info("Executed {} dunning actions for ready cases", processedActions);
        } catch (Exception e) {
            logger.error("Failed to process ready dunning cases: {}", e.getMessage(), e);
            failures++;
        }

        return new DunningProcessResult(newCasesCreated, actionsExecuted, failures);
    }

    /**
     * Detect overdue invoices and create dunning cases for them.
     */
    private int createDunningCasesForOverdueInvoices() {
        // Find overdue invoices that don't already have dunning cases
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoicesWithoutDunningCases();
        
        if (overdueInvoices.isEmpty()) {
            logger.debug("No overdue invoices found without existing dunning cases");
            return 0;
        }

        logger.info("Found {} overdue invoices requiring dunning cases", overdueInvoices.size());

        int casesCreated = 0;
        for (Invoice invoice : overdueInvoices) {
            try {
                // Check if dunning case already exists for this invoice
                Maybe<DunningCase> existingCase = dunningCaseRepository
                    .dunningCaseByInvoiceIdFinder()
                    .apply(invoice.getId());

                if (existingCase.isPresent()) {
                    logger.debug("Dunning case already exists for invoice {}, skipping", invoice.getId());
                    continue;
                }

                // Create new dunning case
                DunningCase dunningCase = DunningCase.create(
                    invoice.getId(),
                    invoice.getBillingAccountId().getValue()
                );

                Result<DunningCaseId> saveResult = dunningCaseRepository
                    .dunningCaseSaver()
                    .apply(dunningCase);

                if (saveResult.isSuccess()) {
                    casesCreated++;
                    logger.info("Created dunning case {} for overdue invoice {}", 
                        dunningCase.getId(), invoice.getId());
                } else {
                    logger.error("Failed to save dunning case for invoice {}: {}", 
                        invoice.getId(), saveResult.getError().get().getMessage());
                }

            } catch (Exception e) {
                logger.error("Error creating dunning case for invoice {}: {}", 
                    invoice.getId(), e.getMessage(), e);
            }
        }

        return casesCreated;
    }

    /**
     * Process dunning cases that are ready for their next action.
     */
    private int processReadyDunningCases() {
        // Find active dunning cases that are ready for action
        List<DunningCase> readyCases = dunningCaseRepository.findReadyForAction();
        
        if (readyCases.isEmpty()) {
            logger.debug("No dunning cases ready for action");
            return 0;
        }

        logger.info("Found {} dunning cases ready for action", readyCases.size());

        int actionsExecuted = 0;
        for (DunningCase dunningCase : readyCases) {
            // Execute actions asynchronously to avoid blocking
            CompletableFuture.runAsync(() -> {
                try {
                    processDunningCase(dunningCase);
                } catch (Exception e) {
                    logger.error("Error processing dunning case {}: {}", 
                        dunningCase.getId(), e.getMessage(), e);
                }
            }, dunningExecutor);
            
            actionsExecuted++;
        }

        return actionsExecuted;
    }

    /**
     * Process a single dunning case by executing its current step.
     */
    @Transactional
    private void processDunningCase(DunningCase dunningCase) {
        logger.info("Processing dunning case {} at step {}", 
            dunningCase.getId(), dunningCase.getCurrentStep());

        try {
            // Execute the dunning action for the current step
            DunningActionResult actionResult = dunningActionExecutor.executeAction(
                dunningCase.getCurrentStep(),
                dunningCase.getInvoiceId(),
                dunningCase.getBillingAccountId()
            );

            // Update the dunning case based on the action result
            Result<Void> stepResult = dunningCase.executeStep(
                actionResult.actionType(),
                actionResult.details(),
                actionResult.successful()
            );

            if (stepResult.isSuccess()) {
                // Save the updated dunning case
                Result<DunningCaseId> saveResult = dunningCaseRepository
                    .dunningCaseSaver()
                    .apply(dunningCase);

                if (saveResult.isSuccess()) {
                    logger.info("Successfully executed {} for dunning case {} (step: {})", 
                        actionResult.actionType(), dunningCase.getId(), dunningCase.getCurrentStep());
                } else {
                    logger.error("Failed to save dunning case {} after step execution: {}", 
                        dunningCase.getId(), saveResult.getError().get().getMessage());
                }
            } else {
                logger.error("Failed to execute step for dunning case {}: {}", 
                    dunningCase.getId(), stepResult.getError().get().getMessage());
            }

        } catch (Exception e) {
            logger.error("Error processing dunning case {}: {}", 
                dunningCase.getId(), e.getMessage(), e);
        }
    }

    /**
     * Manual method to resolve dunning cases when payments are received.
     * This would typically be called from payment processing workflows.
     */
    public void resolveDunningCasesForInvoice(String invoiceId, String resolutionReason) {
        try {
            // Convert string to InvoiceId
            Result<InvoiceId> invoiceIdResult = InvoiceId.fromString(invoiceId);
            if (invoiceIdResult.isFailure()) {
                logger.error("Invalid invoice ID format: {}", invoiceId);
                return;
            }
            
            InvoiceId invoiceIdObj = invoiceIdResult.getValue().get();
            
            Maybe<DunningCase> dunningCaseMaybe = dunningCaseRepository
                .dunningCaseByInvoiceIdFinder()
                .apply(invoiceIdObj);

            if (dunningCaseMaybe.isPresent()) {
                DunningCase dunningCase = dunningCaseMaybe.getValue();
                
                if (dunningCase.isActive()) {
                    Result<Void> resolveResult = dunningCase.resolve(resolutionReason);
                    
                    if (resolveResult.isSuccess()) {
                        Result<DunningCaseId> saveResult = dunningCaseRepository
                            .dunningCaseSaver()
                            .apply(dunningCase);

                        if (saveResult.isSuccess()) {
                            logger.info("Resolved dunning case {} for invoice {}: {}", 
                                dunningCase.getId(), invoiceId, resolutionReason);
                        } else {
                            logger.error("Failed to save resolved dunning case {}: {}", 
                                dunningCase.getId(), saveResult.getError().get().getMessage());
                        }
                    } else {
                        logger.error("Failed to resolve dunning case {}: {}", 
                            dunningCase.getId(), resolveResult.getError().get().getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error resolving dunning case for invoice {}: {}", 
                invoiceId, e.getMessage(), e);
        }
    }

    /**
     * Get statistics about current dunning cases for monitoring.
     */
    public DunningStatistics getDunningStatistics() {
        try {
            List<DunningCase> activeCases = dunningCaseRepository.findByStatus(DunningCaseStatus.IN_PROGRESS);
            List<DunningCase> readyCases = dunningCaseRepository.findReadyForAction();
            
            long step1Cases = activeCases.stream()
                .filter(c -> c.getCurrentStep().name().contains("STEP_1"))
                .count();
            long step2Cases = activeCases.stream()
                .filter(c -> c.getCurrentStep().name().contains("STEP_2"))
                .count();
            long step3Cases = activeCases.stream()
                .filter(c -> c.getCurrentStep().name().contains("STEP_3"))
                .count();
            long step4Cases = activeCases.stream()
                .filter(c -> c.getCurrentStep().name().contains("STEP_4"))
                .count();

            return new DunningStatistics(
                activeCases.size(),
                readyCases.size(),
                (int) step1Cases,
                (int) step2Cases,
                (int) step3Cases,
                (int) step4Cases
            );
        } catch (Exception e) {
            logger.error("Error getting dunning statistics: {}", e.getMessage(), e);
            return new DunningStatistics(0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Result of dunning process execution
     */
    public record DunningProcessResult(
        int newCasesCreated,
        int actionsExecuted,
        int failures
    ) {
        public boolean hasFailures() {
            return failures > 0;
        }
    }

    /**
     * Statistics about current dunning cases
     */
    public record DunningStatistics(
        int totalActiveCases,
        int casesReadyForAction,
        int step1Cases,
        int step2Cases,
        int step3Cases,
        int step4Cases
    ) {}

    /**
     * Result of executing a dunning action
     */
    public record DunningActionResult(
        String actionType,
        String details,
        boolean successful
    ) {}
}
