package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorDistributionColorConsistencyTemplateTest {

    @Test
    void comprehensiveReportTemplateShouldUseSeverityBasedColorsForErrorDistribution() throws IOException {
        String template = loadResource("templates/reports/comprehensive-report.html");

        // Must not be position-based/hardcoded colors for the three cards.
        assertThat(template).doesNotContain("<div class=\"bg-red-50 rounded-lg p-6 border-2 border-red-200\">");
        assertThat(template).doesNotContain("<div class=\"bg-orange-50 rounded-lg p-6 border-2 border-orange-200\">");
        assertThat(template).doesNotContain("<div class=\"bg-yellow-50 rounded-lg p-6 border-2 border-yellow-200\">");

        // Must compute per-card severity from error counts.
        assertThat(template).contains("severity=${cplCnt > 100 ? 'critical' : cplCnt > 50 ? 'high' : cplCnt > 10 ? 'medium' : 'low'}");
        assertThat(template).contains("severity=${accCnt > 100 ? 'critical' : accCnt > 50 ? 'high' : accCnt > 10 ? 'medium' : 'low'}");
        assertThat(template).contains("severity=${conCnt > 100 ? 'critical' : conCnt > 50 ? 'high' : conCnt > 10 ? 'medium' : 'low'}");

        // Severity -> background/border mapping (critical/high/medium/low).
        assertThat(template).contains("severity == 'critical' ? 'bg-red-50 border-red-200'");
        assertThat(template).contains("severity == 'high' ? 'bg-orange-50 border-orange-200'");
        assertThat(template).contains("severity == 'medium' ? 'bg-yellow-50 border-yellow-200'");
        assertThat(template).contains("'bg-green-50 border-green-200'");

        // Severity -> number/text mapping.
        assertThat(template).contains("severity == 'critical' ? 'text-red-600'");
        assertThat(template).contains("severity == 'high' ? 'text-orange-600'");
        assertThat(template).contains("severity == 'medium' ? 'text-yellow-600'");
        assertThat(template).contains("'text-green-600'");

        // Severity -> progress bar mapping.
        assertThat(template).contains("severity == 'critical' ? 'bg-red-200'");
        assertThat(template).contains("severity == 'high' ? 'bg-orange-200'");
        assertThat(template).contains("severity == 'medium' ? 'bg-yellow-200'");
        assertThat(template).contains("'bg-green-200'");

        assertThat(template).contains("severity == 'critical' ? 'bg-red-600'");
        assertThat(template).contains("severity == 'high' ? 'bg-orange-600'");
        assertThat(template).contains("severity == 'medium' ? 'bg-yellow-600'");
        assertThat(template).contains("'bg-green-600'");
    }

    private static String loadResource(String classpathLocation) throws IOException {
        try (var in = ErrorDistributionColorConsistencyTemplateTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            assertThat(in)
                .as("resource %s should exist on classpath", classpathLocation)
                .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
