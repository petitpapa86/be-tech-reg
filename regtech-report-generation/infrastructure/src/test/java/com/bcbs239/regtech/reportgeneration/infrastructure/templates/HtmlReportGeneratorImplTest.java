package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import com.bcbs239.regtech.reportgeneration.domain.generation.*;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults.ExposureResult;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults.ValidationError;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlReportGeneratorImplTest {

    private final HtmlReportGeneratorImpl generator = new HtmlReportGeneratorImpl(templateEngine());

    @Test
    void generateComprehensive_rendersTierOneCapitalAndCapitalSection() {
        CalculationResults calc = Fixtures.calculationResults(AmountEur.of(1000), List.of());
        QualityResults quality = Fixtures.qualityResults(
            5,
            4,
            1,
            Fixtures.dimensionScores(Map.of(
                QualityDimension.COMPLETENESS, new BigDecimal("95"),
                QualityDimension.ACCURACY, new BigDecimal("95"),
                QualityDimension.CONSISTENCY, new BigDecimal("95"),
                QualityDimension.TIMELINESS, new BigDecimal("95"),
                QualityDimension.UNIQUENESS, new BigDecimal("95"),
                QualityDimension.VALIDITY, new BigDecimal("95")
            )),
            List.of()
        );

        String html = generator.generateComprehensive(calc, quality, List.of(), Fixtures.metadata());

        assertThat(html).contains("PATRIMONIO DI VIGILANZA");
        assertThat(html).contains("Capitale Tier 1");
        // AmountEur#toFormattedString is locale-stable (Locale.US)
        assertThat(html).contains("€1,000.00");
        assertThat(html).contains("Fondi Propri Totali");
    }

    @Test
    void generateComprehensive_rendersBcbs239ComplianceBadgesBasedOnScores() {
        CalculationResults calc = Fixtures.calculationResults(AmountEur.of(1000), List.of());

        Map<QualityDimension, BigDecimal> scores = Fixtures.dimensionScores(Map.of(
            QualityDimension.ACCURACY, new BigDecimal("95"),      // compliant for Principle 3
            QualityDimension.COMPLETENESS, new BigDecimal("50"),  // non-compliant for Principle 4
            QualityDimension.TIMELINESS, new BigDecimal("92"),    // compliant for Principle 5
            QualityDimension.CONSISTENCY, new BigDecimal("60")     // non-compliant for Principle 6 (>=70 needed)
        ));

        QualityResults quality = Fixtures.qualityResults(10, 9, 2, scores, List.of());

        String html = generator.generateComprehensive(calc, quality, List.of(), Fixtures.metadata());

        assertThat(html).contains("Conformità BCBS 239");
        assertThat(html).contains("Principle 3 - Accuracy and Integrity");
        assertThat(html).contains("Principle 4 - Completeness");
        assertThat(html).contains("Principle 5 - Timeliness");
        assertThat(html).contains("Principle 6 - Adaptability");

        // At least one compliant and one non-compliant badge should render.
        assertThat(html).contains("bg-green-100 text-green-800");
        assertThat(html).contains("bg-red-100 text-red-800");
        assertThat(html).contains("CONFORME");
        assertThat(html).contains("NON CONFORME");
    }

    @Test
    void generateComprehensive_dimensionScoreColorsMatchThresholds() {
        CalculationResults calc = Fixtures.calculationResults(AmountEur.of(1000), List.of());

        Map<QualityDimension, BigDecimal> scores = Fixtures.dimensionScores(Map.of(
            QualityDimension.COMPLETENESS, new BigDecimal("95"), // green
            QualityDimension.ACCURACY, new BigDecimal("80"),     // amber
            QualityDimension.CONSISTENCY, new BigDecimal("60"),  // red
            QualityDimension.TIMELINESS, new BigDecimal("95"),
            QualityDimension.UNIQUENESS, new BigDecimal("95"),
            QualityDimension.VALIDITY, new BigDecimal("95")
        ));

        List<ExposureResult> exposureResults = new ArrayList<>();
        exposureResults.add(new ExposureResult("exp-1", false, List.of(
            new ValidationError("COMPLETENESS", "R1", "Missing required", "fieldA", "HIGH"),
            new ValidationError("ACCURACY", "R2", "Invalid value", "fieldB", "MEDIUM"),
            new ValidationError("CONSISTENCY", "R3", "Mismatch", "fieldC", "LOW")
        )));

        QualityResults quality = Fixtures.qualityResults(1, 0, 3, scores, exposureResults);

        String html = generator.generateComprehensive(calc, quality, List.of(), Fixtures.metadata());

        // Use broad assertions: ensure each block includes the expected color class.
        assertThat(html).containsPattern("Completezza \\(Completeness\\)[\\s\\S]*text-green-600");
        assertThat(html).containsPattern("Accuratezza \\(Accuracy\\)[\\s\\S]*text-amber-600");
        assertThat(html).containsPattern("Coerenza \\(Consistency\\)[\\s\\S]*text-red-600");

        // And ensure bars use the same thresholds.
        assertThat(html).containsPattern("Completezza[\\s\\S]*bg-green-600");
        assertThat(html).containsPattern("Accuratezza[\\s\\S]*bg-amber-500");
        assertThat(html).containsPattern("Coerenza[\\s\\S]*bg-red-600");
    }

    @Test
    void generateComprehensive_handlesZeroTotalsWithoutDivisionErrors() {
        CalculationResults calc = Fixtures.calculationResults(AmountEur.of(0), List.of());
        QualityResults quality = Fixtures.qualityResults(0, 0, 0, Fixtures.dimensionScores(Map.of()), List.of());

        String html = generator.generateComprehensive(calc, quality, List.of(), Fixtures.metadata());

        assertThat(html).contains("Punteggio Complessivo");
        assertThat(html).contains("Esposizioni Totali");
        assertThat(html).contains("Errori Totali");
    }

    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/reports/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static final class Fixtures {
        private static final BatchId BATCH_ID = BatchId.of("batch_test_001");
        private static final BankId BANK_ID = BankId.of("08081");
        private static final ReportingDate REPORTING_DATE = ReportingDate.of(LocalDate.of(2024, 9, 30));

        private static ReportMetadata metadata() {
            return new ReportMetadata(
                BATCH_ID,
                BANK_ID,
                "Banca Test",
                REPORTING_DATE,
                Instant.parse("2024-10-01T10:15:30Z")
            );
        }

        private static CalculationResults calculationResults(AmountEur tierOneCapital, List<CalculatedExposure> exposures) {
            GeographicBreakdown geo = new GeographicBreakdown(
                AmountEur.zero(), BigDecimal.ZERO, 0,
                AmountEur.zero(), BigDecimal.ZERO, 0,
                AmountEur.zero(), BigDecimal.ZERO, 0
            );

            SectorBreakdown sector = new SectorBreakdown(
                AmountEur.zero(), BigDecimal.ZERO, 0,
                AmountEur.zero(), BigDecimal.ZERO, 0,
                AmountEur.zero(), BigDecimal.ZERO, 0,
                AmountEur.zero(), BigDecimal.ZERO, 0,
                AmountEur.zero(), BigDecimal.ZERO, 0
            );

            ConcentrationIndices indices = new ConcentrationIndices(BigDecimal.ZERO, BigDecimal.ZERO);
            ProcessingTimestamps timestamps = new ProcessingTimestamps(Instant.parse("2024-10-01T10:15:30Z"), null, null, null, null);

            AmountEur totalAmount = AmountEur.zero();
            return new CalculationResults(
                BATCH_ID,
                BANK_ID,
                "Banca Test",
                REPORTING_DATE,
                tierOneCapital,
                exposures.size(),
                totalAmount,
                0,
                exposures,
                geo,
                sector,
                indices,
                timestamps
            );
        }

        private static QualityResults qualityResults(
            int totalExposures,
            int validExposures,
            int totalErrors,
            Map<QualityDimension, BigDecimal> dimensionScores,
            List<ExposureResult> exposureResults
        ) {
            return new QualityResults(
                BATCH_ID,
                BANK_ID,
                Instant.parse("2024-10-01T10:15:30Z"),
                totalExposures,
                validExposures,
                totalErrors,
                dimensionScores,
                List.of(),
                exposureResults
            );
        }

        private static Map<QualityDimension, BigDecimal> dimensionScores(Map<QualityDimension, BigDecimal> overrides) {
            Map<QualityDimension, BigDecimal> map = new EnumMap<>(QualityDimension.class);
            for (QualityDimension d : QualityDimension.values()) {
                map.put(d, new BigDecimal("95"));
            }
            map.putAll(overrides);
            return map;
        }
    }
}
