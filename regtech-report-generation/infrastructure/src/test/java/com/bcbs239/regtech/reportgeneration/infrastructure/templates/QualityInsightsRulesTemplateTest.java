package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class QualityInsightsRulesTemplateTest {

    @Test
    void comprehensiveReportTemplateShouldEmbedDynamicQualityInsightRules() throws IOException {
        String template = loadResource("templates/reports/comprehensive-report.html");

        // Core rule functions (rules-as-code)
        assertThat(template).contains("const getQualityGrade");
        assertThat(template).contains("const getAttentionLevel");
        assertThat(template).contains("const getComplianceStatus");
        assertThat(template).contains("const rankDimensionsByPriority");
        assertThat(template).contains("const generateQualityInsights");

        // Grade thresholds
        assertThat(template).contains("if (s >= 95)");
        assertThat(template).contains("if (s >= 85)");
        assertThat(template).contains("if (s >= 75)");
        assertThat(template).contains("if (s >= 65)");
        assertThat(template).contains("if (s >= 50)");

        // Attention thresholds (validation rate / error rate)
        assertThat(template).contains("severityThresholds");
        assertThat(template).contains("overallScore: 65.0");
        assertThat(template).contains("validationRate: 20.0");
        assertThat(template).contains("errorRate: 50.0");
        assertThat(template).contains("overallScore: 75.0");
        assertThat(template).contains("validationRate: 50.0");
        assertThat(template).contains("errorRate: 20.0");
        assertThat(template).contains("overallScore: 85.0");
        assertThat(template).contains("validationRate: 80.0");
        assertThat(template).contains("errorRate: 10.0");
        assertThat(template).contains("validationRate < qualityRules.severityThresholds.critical.validationRate");
        assertThat(template).contains("errorRate > qualityRules.severityThresholds.critical.errorRate");

        // Insight palette classes
        assertThat(template).contains("bg-red-50");
        assertThat(template).contains("border-red-200");
        assertThat(template).contains("bg-orange-50");
        assertThat(template).contains("border-orange-200");
        assertThat(template).contains("bg-yellow-50");
        assertThat(template).contains("border-yellow-200");
        assertThat(template).contains("bg-green-50");
        assertThat(template).contains("border-green-200");
        assertThat(template).contains("bg-blue-50");
        assertThat(template).contains("border-blue-200");

        // Rendering hook
        assertThat(template).contains("qualityInsightsContainer");
        assertThat(template).contains("hydrateQualityInsights");
        assertThat(template).contains("qualityInsightsData");
    }

    private static String loadResource(String classpathLocation) throws IOException {
        try (var in = QualityInsightsRulesTemplateTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            assertThat(in)
                .as("resource %s should exist on classpath", classpathLocation)
                .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
