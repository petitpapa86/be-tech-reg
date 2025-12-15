package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import com.bcbs239.regtech.reportgeneration.domain.generation.*;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.AmountEur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of HtmlReportGenerator using Thymeleaf template engine.
 * 
 * This service generates professional HTML reports with:
 * - Bank information and regulatory references
 * - Executive summary cards
 * - Interactive charts (sector distribution, top exposures)
 * - Detailed exposure tables
 * - Compliance status indicators
 * - Risk analysis sections
 * - Data quality assessment
 * - Quality recommendations
 * 
 * Requirements: 5.3, 6.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HtmlReportGeneratorImpl implements HtmlReportGenerator {
    
    private final SpringTemplateEngine templateEngine;
    
    /**
     * Generates HTML report from calculation results and metadata.
     * 
     * This method is for backward compatibility and generates a report
     * with only Large Exposures section (no quality data).
     * 
     * @param results the calculation results
     * @param metadata the report metadata
     * @return generated HTML content
     * @throws HtmlGenerationException if generation fails
     */
    @Override
    public String generate(CalculationResults results, ReportMetadata metadata) {
        try {
            log.info("Generating HTML report (calculation only) [batchId:{},bankId:{}]", 
                results.batchId().value(), results.bankId().value());
            
            // Create Thymeleaf context
            Context context = new Context();
            
            // Add calculation results
            context.setVariable("calculationResults", results);
            context.setVariable("metadata", metadata);
            
            // Add chart data
            context.setVariable("sectorChartData", prepareSectorChartData(results));
            context.setVariable("topExposuresChartData", prepareTopExposuresChartData(results));

            // Risk-report view data (for Italian layout)
            context.setVariable("riskSectorRows", prepareRiskSectorRows(results));
            context.setVariable("riskTopExposureCards", prepareRiskTopExposureCards(results));
            context.setVariable("riskConcentration", prepareRiskConcentration(results));
            
            // Process template
            String html = templateEngine.process("comprehensive-report", context);
            
            log.info("HTML report generated successfully [batchId:{},size:{}]", 
                results.batchId().value(), html.length());
            
            return html;
            
        } catch (Exception e) {
            log.error("Failed to generate HTML report [batchId:{}]", 
                results.batchId().value(), e);
            throw new HtmlGenerationException("Failed to generate HTML report", e);
        }
    }
    
    /**
     * Generates comprehensive HTML report with both calculation and quality data.
     * 
     * The method:
     * 1. Creates Thymeleaf context with all data
     * 2. Prepares chart data for Chart.js
     * 3. Processes template
     * 4. Returns generated HTML string
     * 
     * @param calculationResults the calculation results
     * @param qualityResults the quality results
     * @param recommendations the quality recommendations
     * @param metadata the report metadata
     * @return generated HTML content
     * @throws HtmlGenerationException if generation fails
     */
    @Override
    public String generateComprehensive(
            CalculationResults calculationResults,
            QualityResults qualityResults,
            List<RecommendationSection> recommendations,
            ReportMetadata metadata) {
        try {
            log.info("Generating comprehensive HTML report [batchId:{},bankId:{}]", 
                calculationResults.batchId().value(), calculationResults.bankId().value());
            
            // Create Thymeleaf context
            Context context = new Context();
            
            // Add all data
            context.setVariable("calculationResults", calculationResults);
            context.setVariable("qualityResults", qualityResults);
            context.setVariable("recommendations", recommendations);
            context.setVariable("metadata", metadata);
            
            // Add chart data
            context.setVariable("sectorChartData", prepareSectorChartData(calculationResults));
            context.setVariable("topExposuresChartData", prepareTopExposuresChartData(calculationResults));

            // Risk-report view data (for Italian layout)
            context.setVariable("riskSectorRows", prepareRiskSectorRows(calculationResults));
            context.setVariable("riskTopExposureCards", prepareRiskTopExposureCards(calculationResults));
            context.setVariable("riskConcentration", prepareRiskConcentration(calculationResults));
            
            // Process template
            String html = templateEngine.process("comprehensive-report", context);
            
            log.info("Comprehensive HTML report generated successfully [batchId:{},size:{}]", 
                calculationResults.batchId().value(), html.length());
            
            return html;
            
        } catch (Exception e) {
            log.error("Failed to generate comprehensive HTML report", e);
            throw new HtmlGenerationException("Failed to generate comprehensive HTML report", e);
        }
    }
    
    /**
     * Prepares sector distribution data for Chart.js donut chart.
     * 
     * Groups exposures by sector and calculates total amounts.
     * 
     * @param results the calculation results
     * @return map with labels and values for chart
     */
    private Map<String, Object> prepareSectorChartData(CalculationResults results) {
        Map<String, Object> chartData = new HashMap<>();

        // Fallback for risk-calculation v1 outputs where exposure sector metadata can be missing:
        // use the pre-aggregated sector breakdown when exposures don't provide meaningful sector codes.
        if (shouldUseSectorBreakdownFallback(results)) {
            SectorBreakdown breakdown = results.sectorBreakdown();

            List<String> labels = List.of(
                getSectorDisplayName("retail_mortgage"),
                getSectorDisplayName("sovereign"),
                getSectorDisplayName("corporate"),
                getSectorDisplayName("banking"),
                getSectorDisplayName("other")
            );
            List<BigDecimal> values = List.of(
                breakdown.retailMortgageAmount().value(),
                breakdown.sovereignAmount().value(),
                breakdown.corporateAmount().value(),
                breakdown.bankingAmount().value(),
                breakdown.otherAmount().value()
            );

            chartData.put("labels", labels);
            chartData.put("values", values);
            return chartData;
        }
        
        // Group exposures by sector
        Map<String, BigDecimal> sectorTotals = results.exposures().stream()
            .collect(Collectors.groupingBy(
                CalculatedExposure::sectorCode,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    CalculatedExposure::amountEur,
                    BigDecimal::add
                )
            ));
        
        // Sort by amount descending
        List<Map.Entry<String, BigDecimal>> sortedSectors = sectorTotals.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        // Extract labels and values
        List<String> labels = sortedSectors.stream()
            .map(entry -> getSectorDisplayName(entry.getKey()))
            .collect(Collectors.toList());
        
        List<BigDecimal> values = sortedSectors.stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        
        chartData.put("labels", labels);
        chartData.put("values", values);
        
        return chartData;
    }
    
    /**
     * Prepares top exposures data for Chart.js horizontal bar chart.
     * 
     * Selects top 10 exposures by amount.
     * 
     * @param results the calculation results
     * @return map with labels and values for chart
     */
    private Map<String, Object> prepareTopExposuresChartData(CalculationResults results) {
        Map<String, Object> chartData = new HashMap<>();
        
        // Get top 10 exposures by amount
        List<CalculatedExposure> topExposures = results.exposures().stream()
            .sorted(Comparator.comparing(CalculatedExposure::amountEur).reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        // Extract labels (counterparty names, truncated if too long)
        List<String> labels = topExposures.stream()
            .map(exposure -> truncateCounterpartyName(exposure.counterpartyName(), 30))
            .collect(Collectors.toList());
        
        // Extract values
        List<BigDecimal> values = topExposures.stream()
            .map(CalculatedExposure::amountEur)
            .collect(Collectors.toList());

        // Provide sectors + limit flags so the template can apply semantic color rules.
        List<String> sectors = topExposures.stream()
            .map(exposure -> getSectorDisplayName(exposure.sectorCode()))
            .collect(Collectors.toList());

        List<Boolean> limitExceeded = topExposures.stream()
            .map(CalculatedExposure::exceedsLimit)
            .collect(Collectors.toList());
        
        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("sectors", sectors);
        chartData.put("limitExceeded", limitExceeded);
        
        return chartData;
    }
    
    /**
     * Gets display name for sector code.
     * 
     * Maps ESA 2010 sector codes to readable names.
     * 
     * @param sectorCode the sector code (e.g., "S.11")
     * @return display name
     */
    private String getSectorDisplayName(String sectorCode) {
        Map<String, String> sectorNames = Map.ofEntries(
            Map.entry("S.11", "Non-Financial Corporations"),
            Map.entry("S.12", "Financial Corporations"),
            Map.entry("S.121", "Central Bank"),
            Map.entry("S.122", "Deposit-Taking Corporations"),
            Map.entry("S.123", "Money Market Funds"),
            Map.entry("S.124", "Non-MMF Investment Funds"),
            Map.entry("S.125", "Other Financial Intermediaries"),
            Map.entry("S.126", "Financial Auxiliaries"),
            Map.entry("S.127", "Captive Financial Institutions"),
            Map.entry("S.128", "Insurance Corporations"),
            Map.entry("S.129", "Pension Funds"),
            Map.entry("S.13", "General Government"),
            Map.entry("S.14", "Households"),
            Map.entry("S.15", "Non-Profit Institutions"),

            // Risk-calculation v1 sector categories
            Map.entry("RETAIL_MORTGAGE", "Mutui Retail"),
            Map.entry("SOVEREIGN", "Sovrano"),
            Map.entry("CORPORATE", "Corporate"),
            Map.entry("BANKING", "Bancario"),
            Map.entry("OTHER", "Altro"),
            Map.entry("retail_mortgage", "Mutui Retail"),
            Map.entry("sovereign", "Sovrano"),
            Map.entry("corporate", "Corporate"),
            Map.entry("banking", "Bancario"),
            Map.entry("other", "Altro")
        );
        
        return sectorNames.getOrDefault(sectorCode, sectorCode);
    }
    
    /**
     * Truncates counterparty name if too long.
     * 
     * @param name the counterparty name
     * @param maxLength maximum length
     * @return truncated name with ellipsis if needed
     */
    private String truncateCounterpartyName(String name, int maxLength) {
        if (name == null) {
            return "Unknown";
        }
        
        if (name.length() <= maxLength) {
            return name;
        }
        
        return name.substring(0, maxLength - 3) + "...";
    }

    private record RiskSectorRow(
        String label,
        int count,
        BigDecimal amountEur,
        BigDecimal percentOfTotal,
        BigDecimal percentOfCapital,
        String theme
    ) {}

    private record RiskTopExposureCard(
        int rank,
        String medal,
        String counterpartyName,
        BigDecimal amountEur,
        BigDecimal percentOfCapital,
        String sectorLabel,
        boolean limitExceeded,
        String theme
    ) {}

    private record RiskConcentration(
        BigDecimal top3AmountEur,
        BigDecimal top3PercentOfTotal,
        BigDecimal top5AmountEur,
        BigDecimal top5PercentOfTotal,
        BigDecimal avgExposureAmountEur,
        BigDecimal avgExposurePercentOfCapital,
        int sectorsInvolved
    ) {}

    private List<RiskSectorRow> prepareRiskSectorRows(CalculationResults results) {
        BigDecimal totalAmount = Optional.ofNullable(results.totalAmount()).map(AmountEur::value).orElse(BigDecimal.ZERO);
        BigDecimal capital = Optional.ofNullable(results.tierOneCapital()).map(AmountEur::value).orElse(BigDecimal.ZERO);

        if (shouldUseSectorBreakdownFallback(results)) {
            SectorBreakdown breakdown = results.sectorBreakdown();
            int totalExposures = results.totalExposures();

            return List.of(
                new RiskSectorRow(
                    getSectorDisplayName("retail_mortgage"),
                    breakdown.retailMortgageCount() > 0 ? breakdown.retailMortgageCount() : estimateCount(breakdown.retailMortgageAmount().value(), totalAmount, totalExposures),
                    breakdown.retailMortgageAmount().value(),
                    safePercent(breakdown.retailMortgageAmount().value(), totalAmount),
                    safePercent(breakdown.retailMortgageAmount().value(), capital),
                    riskThemeForSector("retail_mortgage", getSectorDisplayName("retail_mortgage"))
                ),
                new RiskSectorRow(
                    getSectorDisplayName("sovereign"),
                    breakdown.sovereignCount() > 0 ? breakdown.sovereignCount() : estimateCount(breakdown.sovereignAmount().value(), totalAmount, totalExposures),
                    breakdown.sovereignAmount().value(),
                    safePercent(breakdown.sovereignAmount().value(), totalAmount),
                    safePercent(breakdown.sovereignAmount().value(), capital),
                    riskThemeForSector("sovereign", getSectorDisplayName("sovereign"))
                ),
                new RiskSectorRow(
                    getSectorDisplayName("corporate"),
                    breakdown.corporateCount() > 0 ? breakdown.corporateCount() : estimateCount(breakdown.corporateAmount().value(), totalAmount, totalExposures),
                    breakdown.corporateAmount().value(),
                    safePercent(breakdown.corporateAmount().value(), totalAmount),
                    safePercent(breakdown.corporateAmount().value(), capital),
                    riskThemeForSector("corporate", getSectorDisplayName("corporate"))
                ),
                new RiskSectorRow(
                    getSectorDisplayName("banking"),
                    breakdown.bankingCount() > 0 ? breakdown.bankingCount() : estimateCount(breakdown.bankingAmount().value(), totalAmount, totalExposures),
                    breakdown.bankingAmount().value(),
                    safePercent(breakdown.bankingAmount().value(), totalAmount),
                    safePercent(breakdown.bankingAmount().value(), capital),
                    riskThemeForSector("banking", getSectorDisplayName("banking"))
                ),
                new RiskSectorRow(
                    getSectorDisplayName("other"),
                    breakdown.otherCount() > 0 ? breakdown.otherCount() : estimateCount(breakdown.otherAmount().value(), totalAmount, totalExposures),
                    breakdown.otherAmount().value(),
                    safePercent(breakdown.otherAmount().value(), totalAmount),
                    safePercent(breakdown.otherAmount().value(), capital),
                    riskThemeForSector("other", getSectorDisplayName("other"))
                )
            ).stream()
                .filter(row -> row.amountEur() != null && row.amountEur().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(RiskSectorRow::amountEur).reversed())
                .collect(Collectors.toList());
        }

        Map<String, List<CalculatedExposure>> grouped = results.exposures().stream()
            .collect(Collectors.groupingBy(CalculatedExposure::sectorCode));

        return grouped.entrySet().stream()
            .map(entry -> {
                String sectorCode = entry.getKey();
                List<CalculatedExposure> exposures = entry.getValue();
                int count = exposures.size();
                BigDecimal amountSum = exposures.stream()
                    .map(CalculatedExposure::amountEur)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal percentTotal = safePercent(amountSum, totalAmount);
                BigDecimal percentCapital = safePercent(amountSum, capital);

                String label = getSectorDisplayName(sectorCode);
                String theme = riskThemeForSector(sectorCode, label);

                return new RiskSectorRow(label, count, amountSum, percentTotal, percentCapital, theme);
            })
            .sorted(Comparator.comparing(RiskSectorRow::amountEur).reversed())
            .collect(Collectors.toList());
    }

    private boolean shouldUseSectorBreakdownFallback(CalculationResults results) {
        if (results == null) {
            return false;
        }

        if (results.exposures() == null || results.exposures().isEmpty()) {
            return true;
        }

        // If exposures exist but sector codes are not informative, prefer the summary breakdown.
        boolean allPlaceholder = results.exposures().stream().allMatch(e -> {
            String code = e.sectorCode();
            if (code == null) {
                return true;
            }
            String normalized = code.trim().toLowerCase();
            return normalized.isBlank() || normalized.equals("other") || normalized.equals("unknown") || normalized.equals("n/a");
        });

        if (!allPlaceholder) {
            return false;
        }

        SectorBreakdown breakdown = results.sectorBreakdown();
        if (breakdown == null) {
            return false;
        }

        BigDecimal sum = breakdown.getTotalAmount().value();
        return sum.compareTo(BigDecimal.ZERO) > 0;
    }

    private int estimateCount(BigDecimal amount, BigDecimal totalAmount, int totalExposures) {
        if (totalExposures <= 0) {
            return 0;
        }
        if (amount == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal share = amount.divide(totalAmount, 6, RoundingMode.HALF_UP);
        int estimate = share.multiply(BigDecimal.valueOf(totalExposures)).setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(totalExposures, estimate));
    }

    private List<RiskTopExposureCard> prepareRiskTopExposureCards(CalculationResults results) {
        List<CalculatedExposure> top = results.getTop10Exposures();

        return java.util.stream.IntStream.range(0, top.size())
            .mapToObj(i -> {
                CalculatedExposure exposure = top.get(i);
                int rank = i + 1;
                String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "";
                String sectorLabel = getSectorDisplayName(exposure.sectorCode());
                boolean limitExceeded = exposure.exceedsLimit();

                String theme;
                if (rank == 1) {
                    theme = "rank1";
                } else if (rank == 2) {
                    theme = "rank2";
                } else if (rank == 3) {
                    theme = "rank3";
                } else {
                    theme = riskThemeForSector(exposure.sectorCode(), sectorLabel);
                }

                return new RiskTopExposureCard(
                    rank,
                    medal,
                    exposure.counterpartyName(),
                    exposure.amountEur(),
                    exposure.percentageOfCapital(),
                    sectorLabel,
                    limitExceeded,
                    theme
                );
            })
            .collect(Collectors.toList());
    }

    private RiskConcentration prepareRiskConcentration(CalculationResults results) {
        BigDecimal totalAmount = Optional.ofNullable(results.totalAmount()).map(AmountEur::value).orElse(BigDecimal.ZERO);
        BigDecimal capital = Optional.ofNullable(results.tierOneCapital()).map(AmountEur::value).orElse(BigDecimal.ZERO);

        List<CalculatedExposure> sorted = results.exposures().stream()
            .sorted(Comparator.comparing(CalculatedExposure::amountEur).reversed())
            .collect(Collectors.toList());

        BigDecimal top3 = sorted.stream().limit(3).map(CalculatedExposure::amountEur).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal top5 = sorted.stream().limit(5).map(CalculatedExposure::amountEur).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgAmount = BigDecimal.ZERO;
        BigDecimal avgPctCapital = BigDecimal.ZERO;
        if (!sorted.isEmpty()) {
            avgAmount = totalAmount.divide(BigDecimal.valueOf(sorted.size()), 2, RoundingMode.HALF_UP);
            avgPctCapital = safePercent(avgAmount, capital);
        }

        int sectorsInvolved = (int) results.exposures().stream().map(CalculatedExposure::sectorCode).distinct().count();

        return new RiskConcentration(
            top3,
            safePercent(top3, totalAmount),
            top5,
            safePercent(top5, totalAmount),
            avgAmount,
            avgPctCapital,
            sectorsInvolved
        );
    }

    private BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) {
            return BigDecimal.ZERO;
        }
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator
            .multiply(BigDecimal.valueOf(100))
            .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String riskThemeForSector(String sectorCode, String label) {
        String text = (label == null ? "" : label).toLowerCase();
        String code = (sectorCode == null ? "" : sectorCode).toLowerCase();

        if (text.contains("insurance") || text.contains("pension") || code.startsWith("s.128") || code.startsWith("s.129")) {
            return "insurance";
        }
        if (text.contains("bank") || text.contains("deposit") || text.contains("central bank") || code.startsWith("s.12") || code.startsWith("s.121") || code.startsWith("s.122")) {
            return "bank";
        }
        if (text.contains("government") || code.startsWith("s.13")) {
            return "other";
        }
        return "corporate";
    }
}
