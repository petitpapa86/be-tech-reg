package com.bcbs239.regtech.modules.dataquality.presentation.controllers;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.dataquality.application.dto.QualityReportDto;
import com.bcbs239.regtech.modules.dataquality.application.dto.QualityTrendsDto;
import com.bcbs239.regtech.modules.dataquality.application.queries.QualityReportQueryHandler;
import com.bcbs239.regtech.modules.dataquality.application.queries.BatchQualityTrendsQueryHandler;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BankId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QualityReportController.
 * Tests the functional endpoint mapping and basic controller behavior.
 */
@ExtendWith(MockitoExtension.class)
class QualityReportControllerTest {
    
    @Mock
    private QualityReportQueryHandler qualityReportQueryHandler;
    
    @Mock
    private BatchQualityTrendsQueryHandler trendsQueryHandler;
    
    private QualityReportController controller;
    
    @BeforeEach
    void setUp() {
        controller = new QualityReportController(qualityReportQueryHandler, trendsQueryHandler);
    }
    
    @Test
    void shouldMapEndpoints() {
        // When
        RouterFunction<ServerResponse> routes = controller.mapEndpoints();
        
        // Then
        assertThat(routes).isNotNull();
    }
    
    @Test
    void shouldCreateControllerWithDependencies() {
        // Then
        assertThat(controller).isNotNull();
    }
}