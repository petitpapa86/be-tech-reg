package com.bcbs239.regtech.dataquality.application.validation.consistency;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for executing consistency checks across exposure records.
 * These checks validate cross-field relationships and data uniformity.
 * 
 * DIMENSIONE: COERENZA (CONSISTENCY)
 * Definizione: I dati sono uniformi tra diverse fonti e nel tempo
 */
@Component
public class ConsistencyValidator {

    /**
     * Validates consistency across all exposure records in the batch.
     * 
     * @param exposures List of all exposure records in the batch
     * @param declaredCount Optional declared count of exposures (from batch metadata)
     * @param crmReferences Optional list of CRM reference IDs
     * @return ConsistencyValidationResult with all check results
     */
    public ConsistencyValidationResult validate(
        List<ExposureRecord> exposures,
        Integer declaredCount,
        List<String> crmReferences
    ) {
        ConsistencyValidationResult.Builder resultBuilder = ConsistencyValidationResult.builder();

        // Check 1: Conteggio Esposizioni
        if (declaredCount != null) {
            ConsistencyCheckResult countCheck = checkExposureCount(exposures, declaredCount);
            resultBuilder.addCheck(countCheck);
        }

        // Check 2: Mappatura CRM → Esposizioni
        if (crmReferences != null && !crmReferences.isEmpty()) {
            ConsistencyCheckResult crmMappingCheck = checkCrmMapping(exposures, crmReferences);
            resultBuilder.addCheck(crmMappingCheck);
        }

        // Check 3: Relazione LEI ↔ counterpartyId (1:1)
        ConsistencyCheckResult leiConsistencyCheck = checkLeiCounterpartyConsistency(exposures);
        resultBuilder.addCheck(leiConsistencyCheck);

        // Check 4: Coerenza Valutaria
        ConsistencyCheckResult currencyCheck = checkCurrencyConsistency(exposures);
        resultBuilder.addCheck(currencyCheck);

        return resultBuilder.build();
    }

    /**
     * Check 1: Conteggio Esposizioni
     * Verifies that declared total_exposures matches actual array length.
     */
    private ConsistencyCheckResult checkExposureCount(
        List<ExposureRecord> exposures,
        Integer declaredCount
    ) {
        int actualCount = exposures.size();
        boolean passed = declaredCount.equals(actualCount);
        double score = passed ? 100.0 : 0.0;

        ConsistencyCheckResult.Builder builder = ConsistencyCheckResult.builder()
            .checkType(ConsistencyCheckType.EXPOSURE_COUNT_MATCH)
            .passed(passed)
            .score(score);

        if (passed) {
            builder.summary(String.format(
                "Dichiarato: %d, Effettivo: %d - Match: ✓ Coerente",
                declaredCount, actualCount
            ));
        } else {
            builder.summary(String.format(
                "Dichiarato: %d, Effettivo: %d - Match: ✗ INCOERENTE",
                declaredCount, actualCount
            ));
            builder.addViolation(ConsistencyViolation.builder()
                .violationType("COUNT_MISMATCH")
                .affectedEntity("Batch")
                .expectedValue(String.valueOf(declaredCount))
                .actualValue(String.valueOf(actualCount))
                .description("Il conteggio dichiarato non corrisponde al numero effettivo di esposizioni")
                .build());
        }

        return builder.build();
    }

    /**
     * Check 2: Mappatura CRM → Esposizioni
     * Verifies that every CRM reference corresponds to an existing exposure.
     */
    private ConsistencyCheckResult checkCrmMapping(
        List<ExposureRecord> exposures,
        List<String> crmReferences
    ) {
        Set<String> exposureIds = exposures.stream()
            .map(ExposureRecord::exposureId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<ConsistencyViolation> violations = new ArrayList<>();
        int matchedCount = 0;

        for (int i = 0; i < crmReferences.size(); i++) {
            String crmRef = crmReferences.get(i);
            if (exposureIds.contains(crmRef)) {
                matchedCount++;
            } else {
                violations.add(ConsistencyViolation.builder()
                    .violationType("ORPHAN_CRM_REFERENCE")
                    .affectedEntity("CRM #" + (i + 1))
                    .expectedValue("Esposizione esistente")
                    .actualValue(crmRef)
                    .description(String.format(
                        "CRM #%d (exposure_id: '%s') non trovato in array esposizioni",
                        i + 1, crmRef
                    ))
                    .build());
            }
        }

        double score = crmReferences.isEmpty() ? 100.0 : 
            (matchedCount * 100.0) / crmReferences.size();
        boolean passed = score >= 90.0; // 90% threshold

        String summary = String.format(Locale.US,
            "CRM dichiarati: %d, CRM abbinati: %d/%d (%.0f%%) - %s",
            crmReferences.size(),
            matchedCount,
            crmReferences.size(),
            score,
            passed ? "✓" : "✗ FAIL"
        );

        return ConsistencyCheckResult.builder()
            .checkType(ConsistencyCheckType.CRM_EXPOSURE_MAPPING)
            .passed(passed)
            .score(score)
            .summary(summary)
            .violations(violations)
            .build();
    }

    /**
     * Check 3: Relazione LEI ↔ counterpartyId (1:1)
     * Verifies that same LEI always maps to same counterpartyId.
     * 
     * Principio: Stesso LEI = Stesso counterpartyId
     * Un LEI identifica univocamente un'entità legale.
     */
    private ConsistencyCheckResult checkLeiCounterpartyConsistency(
        List<ExposureRecord> exposures
    ) {
        // Group exposures by LEI
        Map<String, Set<String>> leiToCounterpartyIds = new HashMap<>();
        
        for (ExposureRecord exposure : exposures) {
            if (exposure.counterpartyLei() != null && !exposure.counterpartyLei().isBlank()) {
                leiToCounterpartyIds
                    .computeIfAbsent(exposure.counterpartyLei(), k -> new HashSet<>())
                    .add(exposure.counterpartyId());
            }
        }

        List<ConsistencyViolation> violations = new ArrayList<>();
        int totalLeis = leiToCounterpartyIds.size();
        int consistentLeis = 0;

        for (Map.Entry<String, Set<String>> entry : leiToCounterpartyIds.entrySet()) {
            String lei = entry.getKey();
            Set<String> counterpartyIds = entry.getValue();

            if (counterpartyIds.size() == 1) {
                consistentLeis++;
            } else {
                // Multiple counterpartyIds for same LEI - violation!
                violations.add(ConsistencyViolation.builder()
                    .violationType("LEI_COUNTERPARTY_MISMATCH")
                    .affectedEntity("LEI: " + lei)
                    .expectedValue("Stesso counterpartyId per stesso LEI")
                    .actualValue("Multiple IDs: " + String.join(", ", counterpartyIds))
                    .description(String.format(
                        "LEI '%s' appare con counterpartyId diversi: %s. " +
                        "Stesso LEI deve sempre avere stesso counterpartyId.",
                        lei,
                        String.join(", ", counterpartyIds)
                    ))
                    .build());
            }
        }

        double score = totalLeis == 0 ? 100.0 : 
            (consistentLeis * 100.0) / totalLeis;
        boolean passed = violations.isEmpty();

        String summary = passed ?
            String.format("Tutti %d LEI coerenti (✓ 1:1 mapping)", totalLeis) :
            String.format(
                "%d/%d LEI coerenti - %d violazioni trovate (✗ FAIL)",
                consistentLeis, totalLeis, violations.size()
            );

        return ConsistencyCheckResult.builder()
            .checkType(ConsistencyCheckType.LEI_COUNTERPARTY_CONSISTENCY)
            .passed(passed)
            .score(score)
            .summary(summary)
            .violations(violations)
            .build();
    }

    /**
     * Check 4: Coerenza Valutaria
     * Verifies currency consistency across exposures.
     */
    private ConsistencyCheckResult checkCurrencyConsistency(
        List<ExposureRecord> exposures
    ) {
        Map<String, Long> currencyDistribution = exposures.stream()
            .filter(e -> e.currency() != null)
            .collect(Collectors.groupingBy(
                ExposureRecord::currency,
                Collectors.counting()
            ));

        List<ConsistencyViolation> violations = new ArrayList<>();
        
        // Check if there's a dominant currency (>90% of exposures)
        long totalWithCurrency = currencyDistribution.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        
        Optional<Map.Entry<String, Long>> dominantCurrency = currencyDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue());

        double score = 100.0;
        boolean passed = true;

        if (dominantCurrency.isPresent() && totalWithCurrency > 0) {
            long dominantCount = dominantCurrency.get().getValue();
            double dominantPercentage = (dominantCount * 100.0) / totalWithCurrency;

            if (dominantPercentage < 90.0 && currencyDistribution.size() > 1) {
                // Multiple currencies without clear dominance
                score = dominantPercentage;
                passed = false;

                for (Map.Entry<String, Long> entry : currencyDistribution.entrySet()) {
                    if (!entry.getKey().equals(dominantCurrency.get().getKey())) {
                        violations.add(ConsistencyViolation.builder()
                            .violationType("CURRENCY_INCONSISTENCY")
                            .affectedEntity("Currency: " + entry.getKey())
                            .expectedValue(dominantCurrency.get().getKey())
                            .actualValue(entry.getKey())
                            .description(String.format(Locale.US,
                                "%d esposizioni in %s (%.1f%%) invece di %s",
                                entry.getValue(),
                                entry.getKey(),
                                (entry.getValue() * 100.0) / totalWithCurrency,
                                dominantCurrency.get().getKey()
                            ))
                            .build());
                    }
                }
            }
        }

        String summary = currencyDistribution.isEmpty() ?
            "Nessuna valuta specificata" :
            String.format(
                "Valute: %s - %s",
                currencyDistribution.entrySet().stream()
                    .map(e -> String.format(Locale.US, "%s: %d (%.1f%%)", 
                        e.getKey(), 
                        e.getValue(),
                        (e.getValue() * 100.0) / totalWithCurrency))
                    .collect(Collectors.joining(", ")),
                passed ? "✓ Coerente" : "✗ FAIL"
            );

        return ConsistencyCheckResult.builder()
            .checkType(ConsistencyCheckType.CURRENCY_CONSISTENCY)
            .passed(passed)
            .score(score)
            .summary(summary)
            .violations(violations)
            .build();
    }
}
