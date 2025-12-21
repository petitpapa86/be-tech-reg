package com.bcbs239.regtech.reportgeneration.application.coordination;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for the comprehensive report orchestrator.
 * This interface allows the ReportCoordinator to depend on an abstraction
 * rather than a concrete implementation, following the Dependency Inversion Principle.
 * 
 * The actual implementation will be created in task 10.
 */
public interface IComprehensiveReportOrchestrator {
    
    /**
     * Generates a comprehensive report combining risk calculation and quality validation results.
     * This method is asynchronous and returns a CompletableFuture.
     * 
     * @param riskEventData the risk calculation event data
     * @param qualityEventData the quality validation event data
     */
    void generateComprehensiveReport(
        CalculationEventData riskEventData,
        QualityEventData qualityEventData
    );
}
