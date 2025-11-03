package com.bcbs239.regtech.modules.dataquality.presentation.monitoring;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityHealthRoutes.
 */
@ExtendWith(MockitoExtension.class)
class QualityHealthRoutesTest {
    
    @Mock
    private QualityHealthController controller;
    
    private QualityHealthRoutes routes;
    
    @BeforeEach
    void setUp() {
        routes = new QualityHealthRoutes(controller);
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