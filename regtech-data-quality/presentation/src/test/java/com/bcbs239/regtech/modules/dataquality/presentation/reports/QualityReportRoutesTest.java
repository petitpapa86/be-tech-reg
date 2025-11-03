package com.bcbs239.regtech.modules.dataquality.presentation.reports;



import com.bcbs239.regtech.dataquality.presentation.reports.QualityReportController;
import com.bcbs239.regtech.dataquality.presentation.reports.QualityReportRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityReportRoutes.
 */
@ExtendWith(MockitoExtension.class)
class QualityReportRoutesTest {
    
    @Mock
    private QualityReportController controller;
    
    private QualityReportRoutes routes;
    
    @BeforeEach
    void setUp() {
        routes = new QualityReportRoutes(controller);
    }
    
    @Test
    void shouldCreateRoutes() {
        // When
        RouterFunction<ServerResponse> routerFunction = routes.createRoutes();
        
        // Then
        assertThat(routerFunction).isNotNull();
    }
    
    @Test
    void shouldCreateRoutesWithController() {
        // Then
        assertThat(routes).isNotNull();
    }
}