package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RiskSemanticRulesTemplateTest {

    @Test
    void comprehensiveReportTemplateShouldEmbedSemanticRiskRules() throws IOException {
        String template = loadResource("templates/reports/comprehensive-report.html");

        // Sector consistency rules (never change these)
        assertThat(template).contains("CORPORATE: '#3B82F6'");
        assertThat(template).contains("BANK: '#10B981'");
        assertThat(template).contains("INSURANCE: '#8B5CF6'");

        // Sector border consistency
        assertThat(template).contains("CORPORATE: '#2563EB'");
        assertThat(template).contains("BANK: '#059669'");
        assertThat(template).contains("INSURANCE: '#7C3AED'");

        // Risk rank colors
        assertThat(template).contains("1: '#EF4444'");
        assertThat(template).contains("2: '#F97316'");
        assertThat(template).contains("3: '#F59E0B'");

        // Purple is reserved for insurance only: do not allow it in fallback palette.
        assertThat(template).doesNotContain("fallbackPalette: ['#3B82F6', '#10B981', '#8B5CF6'");
    }

    private static String loadResource(String classpathLocation) throws IOException {
        try (var in = RiskSemanticRulesTemplateTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            assertThat(in)
                .as("resource %s should exist on classpath", classpathLocation)
                .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
