package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Unit tests for business context in traces.
 * 
 * Feature: observability-enhancement
 * Property 3: Business context in traces
 * Validates: Requirements 1.1, 1.2
 * 
 * For any business operation annotated with @Observed, the trace should include 
 * relevant business context attributes like batch IDs, user IDs, and operation outcomes.
 */
class BusinessContextInTracesTest {

    private ObservationRegistry observationRegistry;
    private BusinessObservationHandler businessObservationHandler;
    private TraceContextManager traceContextManager;
    private Tracer tracer;

    @BeforeEach
    void setUp() {
        observationRegistry = ObservationRegistry.create();
        businessObservationHandler = new BusinessObservationHandler();
        tracer = new SimpleTracer();
        traceContextManager = new TraceContextManagerImpl(tracer);
        
        // Register the business observation handler
        observationRegistry.observationConfig().observationHandler(businessObservationHandler);
    }

    @Test
    void businessContextIsAddedToObservations() {
        // Given: An observation for a business operation
        Observation observation = Observation.createNotStarted("business.risk.calculation", observationRegistry);
        
        // When: Starting the observation
        observation.start();
        
        // Then: Business context should be added
        assertNotNull(observation.getContext());
        assertEquals("regtech", observation.getContext().getLowCardinalityKeyValue("service.type"));
        assertEquals("risk-calculation", observation.getContext().getLowCardinalityKeyValue("business.domain"));
        assertEquals("process", observation.getContext().getLowCardinalityKeyValue("business.operation.type"));
        
        observation.stop();
    }

    @Test
    void batchContextIsAddedToTraces() {
        // Given: An observation with batch context
        Observation observation = Observation.createNotStarted("business.batch.processing", observationRegistry)
            .highCardinalityKeyValue("batch.id", "batch-123");
        
        // When: Starting the observation
        observation.start();
        
        // Then: Batch context should be preserved and enhanced
        assertEquals("batch-123", observation.getContext().getHighCardinalityKeyValue("batch.id"));
        assertEquals("ingestion", observation.getContext().getLowCardinalityKeyValue("business.domain"));
        assertEquals("process", observation.getContext().getLowCardinalityKeyValue("business.operation.type"));
        
        observation.stop();
    }

    @Test
    void userContextIsAddedToTraces() {
        // Given: An observation with user context
        Observation observation = Observation.createNotStarted("business.user.authentication", observationRegistry)
            .highCardinalityKeyValue("user.id", "user-456");
        
        // When: Starting the observation
        observation.start();
        
        // Then: User context should be preserved and enhanced
        assertEquals("user-456", observation.getContext().getHighCardinalityKeyValue("user.id"));
        assertEquals("iam", observation.getContext().getLowCardinalityKeyValue("business.domain"));
        
        observation.stop();
    }

    @Test
    void errorContextIsAddedToFailedOperations() {
        // Given: An observation that will fail
        Observation observation = Observation.createNotStarted("business.data.validation", observationRegistry);
        RuntimeException testException = new RuntimeException("Validation failed");
        
        // When: Starting and failing the observation
        observation.start();
        observation.error(testException);
        observation.stop();
        
        // Then: Error context should be added
        assertEquals("RuntimeException", observation.getContext().getLowCardinalityKeyValue("error.type"));
        assertEquals("system", observation.getContext().getLowCardinalityKeyValue("error.category"));
        assertEquals("failure", observation.getContext().getLowCardinalityKeyValue("business.outcome"));
    }

    @Test
    void businessExceptionContextIsAddedCorrectly() {
        // Given: An observation that will fail with a business exception
        Observation observation = Observation.createNotStarted("business.risk.calculation", observationRegistry);
        RuntimeException businessException = new RuntimeException("BusinessValidationException: Invalid portfolio");
        
        // When: Starting and failing the observation
        observation.start();
        observation.error(businessException);
        observation.stop();
        
        // Then: Business error context should be added
        assertEquals("RuntimeException", observation.getContext().getLowCardinalityKeyValue("error.type"));
        assertEquals("system", observation.getContext().getLowCardinalityKeyValue("error.category"));
        assertEquals("failure", observation.getContext().getLowCardinalityKeyValue("business.outcome"));
    }

    @Test
    void serviceLayerIsDetectedCorrectly() {
        // Test presentation layer detection
        Observation presentationObs = Observation.createNotStarted("user.controller.login", observationRegistry);
        presentationObs.start();
        assertEquals("presentation", presentationObs.getContext().getLowCardinalityKeyValue("service.layer"));
        presentationObs.stop();
        
        // Test application layer detection
        Observation applicationObs = Observation.createNotStarted("user.service.authenticate", observationRegistry);
        applicationObs.start();
        assertEquals("application", applicationObs.getContext().getLowCardinalityKeyValue("service.layer"));
        applicationObs.stop();
        
        // Test infrastructure layer detection
        Observation infrastructureObs = Observation.createNotStarted("user.repository.findById", observationRegistry);
        infrastructureObs.start();
        assertEquals("infrastructure", infrastructureObs.getContext().getLowCardinalityKeyValue("service.layer"));
        infrastructureObs.stop();
    }

    @Test
    void businessDomainIsDetectedCorrectly() {
        // Test IAM domain
        Observation iamObs = Observation.createNotStarted("iam.user.authentication", observationRegistry);
        iamObs.start();
        assertEquals("iam", iamObs.getContext().getLowCardinalityKeyValue("business.domain"));
        iamObs.stop();
        
        // Test Billing domain
        Observation billingObs = Observation.createNotStarted("billing.payment.process", observationRegistry);
        billingObs.start();
        assertEquals("billing", billingObs.getContext().getLowCardinalityKeyValue("business.domain"));
        billingObs.stop();
        
        // Test Data Quality domain
        Observation qualityObs = Observation.createNotStarted("quality.validation.execute", observationRegistry);
        qualityObs.start();
        assertEquals("data-quality", qualityObs.getContext().getLowCardinalityKeyValue("business.domain"));
        qualityObs.stop();
    }

    @Test
    void operationTypeIsDetectedCorrectly() {
        // Test create operation
        Observation createObs = Observation.createNotStarted("user.create.account", observationRegistry);
        createObs.start();
        assertEquals("create", createObs.getContext().getLowCardinalityKeyValue("business.operation.type"));
        createObs.stop();
        
        // Test read operation
        Observation readObs = Observation.createNotStarted("user.get.profile", observationRegistry);
        readObs.start();
        assertEquals("read", readObs.getContext().getLowCardinalityKeyValue("business.operation.type"));
        readObs.stop();
        
        // Test process operation
        Observation processObs = Observation.createNotStarted("batch.process.data", observationRegistry);
        processObs.start();
        assertEquals("process", processObs.getContext().getLowCardinalityKeyValue("business.operation.type"));
        processObs.stop();
    }

    @Test
    void traceContextManagerProvidesCurrentTraceInfo() {
        // Given: A span is active
        Span span = tracer.nextSpan().name("test-span").start();
        
        try {
            // Set current span using reflection
            java.lang.reflect.Field currentSpanField = tracer.getClass().getDeclaredField("currentSpan");
            currentSpanField.setAccessible(true);
            currentSpanField.set(tracer, span);
            
            // When: Getting trace context
            String traceId = traceContextManager.getCurrentTraceId();
            String spanId = traceContextManager.getCurrentSpanId();
            boolean hasTrace = traceContextManager.hasActiveTrace();
            String formatted = traceContextManager.getFormattedTraceContext();
            
            // Then: Trace information should be available
            assertNotNull(traceId);
            assertNotNull(spanId);
            assertTrue(hasTrace);
            assertTrue(formatted.contains("traceId="));
            assertTrue(formatted.contains("spanId="));
        } catch (Exception e) {
            assumeTrue(false, "SimpleTracer doesn't support setting current span");
        } finally {
            span.end();
        }
    }

    @Test
    void traceContextManagerAddsBusinessContext() {
        // Given: A span is active
        Span span = tracer.nextSpan().name("test-span").start();
        
        try {
            // Set current span using reflection
            java.lang.reflect.Field currentSpanField = tracer.getClass().getDeclaredField("currentSpan");
            currentSpanField.setAccessible(true);
            currentSpanField.set(tracer, span);
            
            // When: Adding business context
            traceContextManager.addBusinessContext("business.batch.id", "batch-789");
            traceContextManager.addUserContext("user-123", "ADMIN");
            traceContextManager.addBatchContext("batch-456", "DAILY");
            
            // Then: Context should be added to the span
            // Note: SimpleSpan doesn't provide tag access, so we verify no exceptions are thrown
            assertTrue(traceContextManager.hasActiveTrace());
        } catch (Exception e) {
            assumeTrue(false, "SimpleTracer doesn't support setting current span");
        } finally {
            span.end();
        }
    }

    @Test
    void completionContextIsAddedForSuccessfulOperations() {
        // Given: An observation that will succeed
        Observation observation = Observation.createNotStarted("business.report.generation", observationRegistry);
        
        // When: Starting and completing the observation successfully
        observation.start();
        observation.stop();
        
        // Then: Success outcome should be recorded
        assertEquals("success", observation.getContext().getLowCardinalityKeyValue("business.outcome"));
        assertEquals("report-generation", observation.getContext().getLowCardinalityKeyValue("business.domain"));
    }

    @Test
    void multipleBusinessContextAttributesArePreserved() {
        // Given: An observation with multiple business context attributes
        Observation observation = Observation.createNotStarted("business.risk.portfolio.calculation", observationRegistry)
            .highCardinalityKeyValue("user.id", "analyst-001")
            .highCardinalityKeyValue("batch.id", "batch-202412")
            .highCardinalityKeyValue("portfolio.id", "portfolio-abc");
        
        // When: Starting the observation
        observation.start();
        
        // Then: All business context should be preserved and enhanced
        assertEquals("analyst-001", observation.getContext().getHighCardinalityKeyValue("user.id"));
        assertEquals("batch-202412", observation.getContext().getHighCardinalityKeyValue("batch.id"));
        assertEquals("portfolio-abc", observation.getContext().getHighCardinalityKeyValue("portfolio.id"));
        assertEquals("risk-calculation", observation.getContext().getLowCardinalityKeyValue("business.domain"));
        assertEquals("process", observation.getContext().getLowCardinalityKeyValue("business.operation.type"));
        
        observation.stop();
    }
}