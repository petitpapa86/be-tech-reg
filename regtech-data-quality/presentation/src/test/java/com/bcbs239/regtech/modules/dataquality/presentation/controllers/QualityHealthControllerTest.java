package com.bcbs239.regtech.modules.dataquality.presentation.controllers;

import com.bcbs239.regtech.modules.dataquality.presentation.handlers.QualityHealthResponseHandler;
import com.bcbs239.regtech.modules.dataquality.presentation.health.QualityHealthChecker;
import com.bcbs239.regtech.modules.dataquality.presentation.metrics.QualityMetricsCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for QualityHealthController.
 * Tests the refactored controller with separated concerns.
 */
@ExtendWith(MockitoExtension.class)
class QualityHealthControllerTest {
    
    @Mock
    private QualityHealthChecker healthChecker;
    
    @Mock
    private QualityMetricsCollector metricsCollector;
    
    @Mock
    private QualityHealthResponseHandler responseHandler;
    
    private QualityHealthController controller;
    
    @BeforeEach
    void setUp() {
        controller = new QualityHealthController(
            healthChecker,
            metricsCollector,
            responseHandler
        );
    }
    
    @Test
    void shouldCreateControllerWithDependencies() {
        // Then
        assertThat(controller).isNotNull();
    }
    
    @Test
    void shouldThrowExceptionWhenMappingEndpoints() {
        // When/Then - mapEndpoints is not supported in refactored version
        assertThatThrownBy(() -> controller.mapEndpoints())
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("QualityHealthRoutes");
    }
}