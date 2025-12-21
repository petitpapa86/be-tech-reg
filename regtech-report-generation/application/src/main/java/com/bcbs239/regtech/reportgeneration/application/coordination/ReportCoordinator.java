package com.bcbs239.regtech.reportgeneration.application.coordination;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Application service that coordinates the arrival of dual events (calculation and quality)
 * and triggers comprehensive report generation when both are present.
 * 
 * This service implements the coordination logic required by Requirements 1.4, 1.5, 5.1.
 * It acts as the orchestration point that ensures reports are only generated when
 * both upstream modules have completed their processing.
 * 
 * Design Decision: This is an application service (not domain service) because it deals
 * with cross-module coordination and integration concerns, which are application-level responsibilities.
 */
@Service
@Slf4j
public class ReportCoordinator {
    
    private final BatchEventTracker eventTracker;
    private final IComprehensiveReportOrchestrator reportOrchestrator;
    
    public ReportCoordinator(
            BatchEventTracker eventTracker,
            IComprehensiveReportOrchestrator reportOrchestrator) {
        this.eventTracker = eventTracker;
        this.reportOrchestrator = reportOrchestrator;
        
        log.info("ReportCoordinator initialized");
    }
    
    /**
     * Handles the arrival of a calculation completed event.
     * Marks the event as received and checks if both events are now present.
     * If both events are present, triggers comprehensive report generation.
     *
     * @param eventData the calculation event data
     */
    public void handleCalculationCompleted(CalculationEventData eventData) {
        String batchId = eventData.getBatchId();
        
        log.info("Handling calculation completed event for batch: {}", batchId);
        
        // Mark risk calculation as complete
        eventTracker.markRiskComplete(batchId, eventData);
        
        // Check if both events are now present
        if (eventTracker.areBothComplete(batchId)) {

            BatchEventTracker.BatchEvents events = eventTracker.getBothEvents(batchId);
            
            // Trigger asynchronous report generation
            reportOrchestrator.generateComprehensiveReport(
                events.getRiskEventData(),
                events.getQualityEventData()
            );
        }
    }
    
    /**
     * Handles the arrival of a quality completed event.
     * Marks the event as received and checks if both events are now present.
     * If both events are present, triggers comprehensive report generation.
     *
     * @param eventData the quality event data
     */
    public void handleQualityCompleted(QualityEventData eventData) {
        String batchId = eventData.getBatchId();
        
        log.info("Handling quality completed event for batch: {}", batchId);
        
        // Mark quality validation as complete
        eventTracker.markQualityComplete(batchId, eventData);
        
        // Check if both events are now present
        if (eventTracker.areBothComplete(batchId)) {
            log.info("Both events present for batch: {}. Triggering comprehensive report generation", batchId);
            
            BatchEventTracker.BatchEvents events = eventTracker.getBothEvents(batchId);
            
            // Trigger asynchronous report generation
            return reportOrchestrator.generateComprehensiveReport(
                events.getRiskEventData(),
                events.getQualityEventData()
            );
        } else {
           return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Checks if a batch is currently being tracked (has at least one event).
     * Useful for monitoring and debugging.
     * 
     * @param batchId the batch identifier
     * @return true if batch is being tracked, false otherwise
     */
    public boolean isTracking(String batchId) {
        return eventTracker.isTracking(batchId);
    }
    
    /**
     * Returns the current number of batches being coordinated.
     * Useful for monitoring and health checks.
     * 
     * @return number of batches being tracked
     */
    public int getTrackedBatchCount() {
        return eventTracker.getTrackedBatchCount();
    }
}
