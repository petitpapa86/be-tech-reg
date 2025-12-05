package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.RecommendationSection;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.QualityDimension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quality Recommendations Generator Service
 * 
 * Generates contextual, actionable recommendations based on actual quality issues
 * found in the data validation results. Recommendations are specific to error patterns
 * and dimension scores, not generic advice.
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5
 */
@Service
@Slf4j
public class QualityRecommendationsGenerator {
    
    /**
     * Generate recommendations based on quality results
     * 
     * @param qualityResults Quality validation results
     * @return List of recommendation sections (max 6 for readability)
     */
    public List<RecommendationSection> generateRecommendations(QualityResults qualityResults) {
        log.debug("Generating recommendations for batch: {}", qualityResults.getBatchId());
        
        List<RecommendationSection> recommendations = new ArrayList<>();
        
        // 1. Critical situation (if score < 60%)
        if (qualityResults.getOverallScore().compareTo(new BigDecimal("60")) < 0) {
            recommendations.add(generateCriticalSituationSection(qualityResults));
        }
        
        // 2. Dimension-specific issues
        recommendations.addAll(generateDimensionSpecificSections(qualityResults));
        
        // 3. Error pattern analysis (based on actual errors)
        recommendations.addAll(generateErrorPatternSections(qualityResults));
        
        // 4. Positive aspects (if any dimension >= 95%)
        generatePositiveAspectsSection(qualityResults)
            .ifPresent(recommendations::add);
        
        // 5. Action plan (always included)
        recommendations.add(generateActionPlanSection(qualityResults));
        
        // Limit to 6 sections for readability
        List<RecommendationSection> result = recommendations.stream()
            .limit(6)
            .collect(Collectors.toList());
        
        log.info("Generated {} recommendation sections for batch: {}", 
            result.size(), qualityResults.getBatchId());
        
        return result;
    }
    
    /**
     * Generate critical situation section for scores < 60%
     * Requirement: 9.1
     */
    private RecommendationSection generateCriticalSituationSection(QualityResults qualityResults) {
        BigDecimal score = qualityResults.getOverallScore();
        int invalidCount = qualityResults.getTotalExposures() - qualityResults.getValidExposures();
        int totalCount = qualityResults.getTotalExposures();
        
        String content = String.format(
            "Il punteggio complessivo di qualità del <strong>%.1f%%</strong> indica una situazione critica. " +
            "<strong>%d esposizioni su %d</strong> presentano errori che richiedono azione immediata.",
            score, invalidCount, totalCount
        );
        
        List<String> bullets = Arrays.asList(
            "Bloccare l'invio della segnalazione fino alla risoluzione degli errori critici",
            "Convocare riunione urgente del team Data Quality",
            "Identificare e correggere immediatamente le esposizioni con errori CRITICAL",
            "Implementare controlli aggiuntivi nei processi di data entry"
        );
        
        return RecommendationSection.builder()
            .icon("🚨")
            .colorClass("red")
            .title("Situazione Critica - Azione Immediata Richiesta")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate dimension-specific sections for threshold violations
     * Requirement: 9.2
     */
    private List<RecommendationSection> generateDimensionSpecificSections(QualityResults qualityResults) {
        List<RecommendationSection> sections = new ArrayList<>();
        
        for (Map.Entry<QualityDimension, BigDecimal> entry : qualityResults.getDimensionScores().entrySet()) {
            QualityDimension dimension = entry.getKey();
            BigDecimal score = entry.getValue();
            
            if (dimension.isCritical(score)) {
                sections.add(generateDimensionSection(dimension, score, qualityResults));
            }
        }
        
        return sections;
    }
    
    /**
     * Generate section for specific dimension
     */
    private RecommendationSection generateDimensionSection(
            QualityDimension dimension, 
            BigDecimal score, 
            QualityResults qualityResults) {
        
        return switch (dimension) {
            case COMPLETENESS -> generateCompletenessSection(score, qualityResults);
            case ACCURACY -> generateAccuracySection(score, qualityResults);
            case CONSISTENCY -> generateConsistencySection(score, qualityResults);
            case TIMELINESS -> generateTimelinessSection(score, qualityResults);
            case UNIQUENESS -> generateUniquenessSection(score, qualityResults);
            case VALIDITY -> generateValiditySection(score, qualityResults);
        };
    }
    
    /**
     * Generate completeness-specific recommendations
     */
    private RecommendationSection generateCompletenessSection(BigDecimal score, QualityResults qualityResults) {
        String content = String.format(
            "Il punteggio di <strong>Completezza (%.1f%%)</strong> è sotto la soglia critica del 70%%. " +
            "Campi obbligatori mancanti compromettono l'affidabilità dei dati.",
            score
        );
        
        List<String> bullets = Arrays.asList(
            "Identificare i campi più frequentemente mancanti dall'analisi degli errori",
            "Verificare i processi di data entry per garantire la compilazione completa",
            "Implementare validazioni obbligatorie nei sistemi sorgente",
            "Formare gli utenti sull'importanza della completezza dei dati"
        );
        
        return RecommendationSection.builder()
            .icon("📋")
            .colorClass("orange")
            .title("Completezza Dati Insufficiente")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate accuracy-specific recommendations
     */
    private RecommendationSection generateAccuracySection(BigDecimal score, QualityResults qualityResults) {
        String content = String.format(
            "Il punteggio di <strong>Accuratezza (%.1f%%)</strong> è sotto la soglia critica del 70%%. " +
            "Valori errati o imprecisi possono portare a decisioni di business sbagliate.",
            score
        );
        
        List<String> bullets = Arrays.asList(
            "Confrontare i dati con fonti autorevoli per identificare discrepanze",
            "Implementare controlli di ragionevolezza sui valori inseriti",
            "Verificare i processi di trasformazione e calcolo dei dati",
            "Stabilire procedure di riconciliazione periodica con sistemi esterni"
        );
        
        return RecommendationSection.builder()
            .icon("🎯")
            .colorClass("orange")
            .title("Accuratezza Dati Insufficiente")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate consistency-specific recommendations
     */
    private RecommendationSection generateConsistencySection(BigDecimal score, QualityResults qualityResults) {
        String content = String.format(
            "Il punteggio di <strong>Coerenza (%.1f%%)</strong> è sotto la soglia critica dell'80%%. " +
            "Incoerenze tra sistemi o nel tempo compromettono l'affidabilità delle analisi.",
            score
        );
        
        List<String> bullets = Arrays.asList(
            "Identificare le regole di business violate più frequentemente",
            "Verificare l'allineamento tra sistemi sorgente diversi",
            "Implementare controlli di coerenza cross-field nei sistemi",
            "Stabilire un data dictionary condiviso tra tutti i sistemi"
        );
        
        return RecommendationSection.builder()
            .icon("🔗")
            .colorClass("orange")
            .title("Coerenza Dati Insufficiente")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate timeliness-specific recommendations
     */
    private RecommendationSection generateTimelinessSection(BigDecimal score, QualityResults qualityResults) {
        String content = String.format(
            "Il punteggio di <strong>Tempestività (%.1f%%)</strong> è sotto la soglia critica del 90%%. " +
            "Dati non aggiornati possono portare a decisioni basate su informazioni obsolete.",
            score
        );
        
        List<String> bullets = Arrays.asList(
            "Verificare i processi di aggiornamento dati per identificare ritardi",
            "Implementare notifiche automatiche per dati non aggiornati",
            "Ottimizzare i processi di caricamento e trasformazione dati",
            "Stabilire SLA chiari per l'aggiornamento dei dati critici"
        );
        
        return RecommendationSection.builder()
            .icon("⏰")
            .colorClass("orange")
            .title("Tempestività Dati Insufficiente")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate uniqueness-specific recommendations
     */
    private RecommendationSection generateUniquenessSection(BigDecimal score, QualityResults qualityResults) {
        String content = String.format(
            "Il punteggio di <strong>Unicità (%.1f%%)</strong> è sotto la soglia critica del 95%%. " +
            "Record duplicati possono causare conteggi errati e analisi distorte.",
            score
        );
        
        List<String> bullets = Arrays.asList(
            "Identificare e rimuovere i record duplicati dal sistema",
            "Implementare constraint di unicità sui campi chiave nel database",
            "Verificare i processi di caricamento per prevenire duplicazioni",
            "Stabilire procedure di de-duplicazione periodica"
        );
        
        return RecommendationSection.builder()
            .icon("🔑")
            .colorClass("orange")
            .title("Unicità Dati Insufficiente")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate validity-specific recommendations
     */
    private RecommendationSection generateValiditySection(BigDecimal score, QualityResults qualityResults) {
        String content = String.format(
            "Il punteggio di <strong>Validità (%.1f%%)</strong> è sotto la soglia critica del 90%%. " +
            "Dati non conformi ai formati e regole definiti compromettono l'integrità del sistema.",
            score
        );
        
        List<String> bullets = Arrays.asList(
            "Verificare che i dati rispettino i formati e domini definiti",
            "Implementare validazioni stringenti nei sistemi di input",
            "Aggiornare le regole di validazione per riflettere i requisiti attuali",
            "Formare gli utenti sui formati e standard richiesti"
        );
        
        return RecommendationSection.builder()
            .icon("✓")
            .colorClass("orange")
            .title("Validità Dati Insufficiente")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate error pattern sections based on top error types
     * Requirement: 9.3
     */
    private List<RecommendationSection> generateErrorPatternSections(QualityResults qualityResults) {
        List<RecommendationSection> sections = new ArrayList<>();
        
        // Get top 3 error types
        List<Map.Entry<String, Long>> topErrors = qualityResults.getTopErrorTypes(3);
        
        if (!topErrors.isEmpty()) {
            sections.add(generateTopErrorsSection(topErrors, qualityResults));
        }
        
        return sections;
    }
    
    /**
     * Generate section for top error types
     */
    private RecommendationSection generateTopErrorsSection(
            List<Map.Entry<String, Long>> topErrors,
            QualityResults qualityResults) {
        
        StringBuilder content = new StringBuilder();
        content.append("I <strong>3 errori più frequenti</strong> identificati nell'analisi sono:<br><br>");
        
        for (int i = 0; i < topErrors.size(); i++) {
            Map.Entry<String, Long> error = topErrors.get(i);
            double percentage = (error.getValue() * 100.0) / qualityResults.getTotalErrors();
            content.append(String.format(
                "%d. <strong>%s</strong>: %d occorrenze (%.1f%%)<br>",
                i + 1, error.getKey(), error.getValue(), percentage
            ));
        }
        
        List<String> bullets = Arrays.asList(
            "Analizzare le cause root degli errori più frequenti",
            "Implementare controlli preventivi per questi specifici pattern di errore",
            "Formare il team sui requisiti specifici che causano questi errori",
            "Monitorare l'andamento di questi errori nel tempo"
        );
        
        return RecommendationSection.builder()
            .icon("📊")
            .colorClass("blue")
            .title("Pattern di Errori Più Comuni")
            .content(content.toString())
            .bullets(bullets)
            .build();
    }
    
    /**
     * Generate positive aspects section for excellent dimensions (>= 95%)
     * Requirement: 9.4
     */
    private Optional<RecommendationSection> generatePositiveAspectsSection(QualityResults qualityResults) {
        List<String> excellentDimensions = qualityResults.getDimensionScores().entrySet().stream()
            .filter(entry -> entry.getKey().isExcellent(entry.getValue()))
            .map(entry -> String.format("%s (%.1f%%)", 
                entry.getKey().getDisplayName(), 
                entry.getValue()))
            .collect(Collectors.toList());
        
        if (excellentDimensions.isEmpty()) {
            return Optional.empty();
        }
        
        String content = "Le seguenti dimensioni mostrano <strong>eccellenza nella qualità</strong> (≥95%):<br>" +
            String.join(", ", excellentDimensions);
        
        List<String> bullets = Arrays.asList(
            "Documentare le best practice utilizzate per queste dimensioni",
            "Condividere i processi di successo con altre aree",
            "Mantenere gli standard elevati attraverso monitoraggio continuo",
            "Utilizzare questi processi come modello per migliorare altre dimensioni"
        );
        
        return Optional.of(RecommendationSection.builder()
            .icon("⭐")
            .colorClass("green")
            .title("Aspetti Positivi")
            .content(content)
            .bullets(bullets)
            .build());
    }
    
    /**
     * Generate action plan section with short/medium/long-term actions
     * Requirement: 9.5
     */
    private RecommendationSection generateActionPlanSection(QualityResults qualityResults) {
        BigDecimal score = qualityResults.getOverallScore();
        QualityResults.AttentionLevel attentionLevel = qualityResults.getAttentionLevel();
        
        String content = String.format(
            "Piano d'azione strutturato per migliorare la qualità dei dati dal livello attuale di <strong>%.1f%%</strong>:",
            score
        );
        
        List<String> bullets = new ArrayList<>();
        
        // Short-term actions (1-2 weeks)
        bullets.add("<strong>Breve termine (1-2 settimane):</strong> " +
            getShortTermActions(attentionLevel));
        
        // Medium-term actions (1-3 months)
        bullets.add("<strong>Medio termine (1-3 mesi):</strong> " +
            getMediumTermActions(attentionLevel));
        
        // Long-term actions (3-6 months)
        bullets.add("<strong>Lungo termine (3-6 mesi):</strong> " +
            getLongTermActions(attentionLevel));
        
        return RecommendationSection.builder()
            .icon("📅")
            .colorClass("purple")
            .title("Piano d'Azione")
            .content(content)
            .bullets(bullets)
            .build();
    }
    
    /**
     * Get short-term actions based on attention level
     */
    private String getShortTermActions(QualityResults.AttentionLevel attentionLevel) {
        return switch (attentionLevel) {
            case CRITICAL -> "Correggere immediatamente gli errori critici, bloccare segnalazioni errate";
            case HIGH -> "Identificare e correggere gli errori più frequenti, implementare controlli urgenti";
            case MEDIUM -> "Analizzare pattern di errori, implementare validazioni mancanti";
            case LOW -> "Monitorare metriche di qualità, mantenere standard elevati";
        };
    }
    
    /**
     * Get medium-term actions based on attention level
     */
    private String getMediumTermActions(QualityResults.AttentionLevel attentionLevel) {
        return switch (attentionLevel) {
            case CRITICAL -> "Ridisegnare processi di data entry, implementare controlli automatici estensivi";
            case HIGH -> "Ottimizzare processi di validazione, formare team su best practice";
            case MEDIUM -> "Automatizzare controlli di qualità, implementare dashboard di monitoraggio";
            case LOW -> "Documentare best practice, condividere processi di successo";
        };
    }
    
    /**
     * Get long-term actions based on attention level
     */
    private String getLongTermActions(QualityResults.AttentionLevel attentionLevel) {
        return switch (attentionLevel) {
            case CRITICAL -> "Implementare data governance framework, stabilire cultura della qualità";
            case HIGH -> "Implementare data quality framework completo, automatizzare monitoraggio";
            case MEDIUM -> "Ottimizzare architettura dati, implementare data lineage";
            case LOW -> "Mantenere eccellenza, innovare con AI/ML per quality assurance";
        };
    }
}
