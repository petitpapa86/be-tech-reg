package com.bcbs239.regtech.dataquality.application.validation.timeliness;

import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Application service for calculating timeliness dimension score.
 * 
 * DIMENSIONE: TEMPESTIVITÀ (TIMELINESS)
 * Definizione: I dati sono aggiornati e disponibili quando necessari
 * 
 * Calcolo:
 * - Data Riferimento Dati: reportingDate dall'exposure record
 * - Data Caricamento File: uploadDate dal batch
 * - Ritardo = uploadDate - reportingDate (giorni calendario)
 * - Soglia BCBS 239: da DB (default ≤ 7 giorni)
 * - Score: decresce con l'aumentare del ritardo
 */
@Component
public class TimelinessValidator {

    private final QualityThresholdRepository thresholdRepository;

    public TimelinessValidator(QualityThresholdRepository thresholdRepository) {
        this.thresholdRepository = thresholdRepository;
    }

    /**
     * Calculates timeliness score for a batch of exposures.
     * 
     * @param exposures List of exposure records
     * @param uploadDate Date when the file was uploaded
     * @param reportDate Official reporting date from bank metadata (preferred) or null to calculate from exposures
     * @param bankId Bank identifier to load thresholds
     * @return TimelinessResult with score, delay, and threshold details
     */
    public TimelinessResult calculateTimeliness(
        List<ExposureRecord> exposures,
        LocalDate uploadDate,
        LocalDate reportDate,
        String bankId
    ) {
        if (exposures == null || exposures.isEmpty()) {
            return TimelinessResult.empty();
        }

        // Use provided reportDate from metadata, or calculate from exposures as fallback
        LocalDate reportingDate = reportDate;
        if (reportingDate == null) {
            reportingDate = exposures.stream()
                .map(ExposureRecord::reportingDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(null);
        }

        if (reportingDate == null) {
            return TimelinessResult.noReportingDate();
        }

        // Calculate delay (giorni calendario)
        long delayDays = ChronoUnit.DAYS.between(reportingDate, uploadDate);

        // Load threshold from DB
        int thresholdDays = thresholdRepository.findByBankId(bankId)
            .map(QualityThreshold::timelinessDays)
            .orElse(7); // Default BCBS 239: ≤ 7 giorni

        // Calculate score
        double score = calculateScore(delayDays, thresholdDays);
        boolean passed = delayDays <= thresholdDays;

        return new TimelinessResult(
            reportingDate,
            uploadDate,
            delayDays,
            thresholdDays,
            score,
            passed
        );
    }

    /**
     * Calculates timeliness score based on delay.
     * 
     * Score calculation:
     * - 0 giorni ritardo = 100% (ECCELLENTE)
     * - <= soglia giorni = 100% - (ritardo / soglia * 20%) (PASS)
     * - > soglia giorni = decremento progressivo fino a 0% (FAIL)
     */
    private double calculateScore(long delayDays, int thresholdDays) {
        if (delayDays <= 0) {
            // Dati caricati in anticipo o stesso giorno
            return 100.0;
        }

        if (delayDays <= thresholdDays) {
            // Entro la soglia: score alto ma decresce leggermente
            // Es: soglia 7 giorni, ritardo 3 giorni = 100 - (3/7 * 20) = 91.4%
            double penalty = (delayDays / (double) thresholdDays) * 20.0;
            return Math.max(80.0, 100.0 - penalty);
        }

        // Oltre la soglia: decremento più severo
        // Es: soglia 7 giorni, ritardo 14 giorni = 80 - ((14-7)/7 * 40) = 40%
        double excessDays = delayDays - thresholdDays;
        double penalty = (excessDays / (double) thresholdDays) * 40.0;
        return Math.max(0.0, 80.0 - penalty);
    }

    /**
     * Result of timeliness validation.
     */
    public record TimelinessResult(
        LocalDate reportingDate,
        LocalDate uploadDate,
        long delayDays,
        int thresholdDays,
        double score,
        boolean passed
    ) {
        public static TimelinessResult empty() {
            return new TimelinessResult(
                null,
                null,
                0,
                7,
                0.0,
                false
            );
        }

        public static TimelinessResult noReportingDate() {
            return new TimelinessResult(
                null,
                null,
                0,
                7,
                0.0,
                false
            );
        }
    }
}
