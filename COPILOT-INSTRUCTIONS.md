# 🤖 GitHub Copilot Instructions - BCBS 239 Report Code Generation

## 📋 Overview

Questo file contiene istruzioni per GitHub Copilot su come generare codice dal file `color-rules-config-COMPLETE.yaml`.

**CRITICAL:** Il file YAML è la **SINGLE SOURCE OF TRUTH** per TUTTO il codice dell'applicazione.

---

## 🎯 Cosa Può Fare Copilot

Con il file `color-rules-config-COMPLETE.yaml`, Copilot può generare:

1. ✅ **HTML Thymeleaf** - Sezioni del report con colori dinamici
2. ✅ **JavaScript** - Logica di generazione insights
3. ✅ **Java Tests** - Unit test con le aspettative corrette
4. ✅ **Documentazione** - Markdown con regole aggiornate

---

## 📂 File di Configurazione

### Percorso

In questo repository il file si trova qui:

```
./color-rules-config-COMPLETE.yaml
```

### Struttura

```yaml
global:
  color_palette: {...}          # Colori Tailwind

dimension_scores:
  thresholds:
    excellent: 90.0              # ← FONTE DI VERITÀ
    acceptable: 75.0             # ← FONTE DI VERITÀ
  dimensions: {...}

error_distribution:
  thresholds:
    critical: 100                # ← FONTE DI VERITÀ
    high: 50                     # ← FONTE DI VERITÀ
    medium: 10                   # ← FONTE DI VERITÀ

quality_insights:                # ← NUOVO! Per JavaScript
  severity_thresholds: {...}
  insight_rules: {...}
  analysis_config: {...}
```

---

## 🔧 Task 1: Genera JavaScript per Quality Insights

### Prompt per Copilot

```
Genera un file JavaScript quality-insights.js leggendo color-rules-config-COMPLETE.yaml.

Usa questi valori esatti dal YAML:
- dimension_scores.thresholds.excellent.value = 90.0
- dimension_scores.thresholds.acceptable.value = 75.0
- error_distribution.thresholds.critical.value = 100
- error_distribution.thresholds.high.value = 50
- error_distribution.thresholds.medium.value = 10
- quality_insights.severity_thresholds.critical.overall_score_below = 65.0
- quality_insights.analysis_config.top_dimensions_to_analyze = 3

Genera:
1. const qualityRules con tutti i thresholds
2. Funzioni helper (safeNumber, safePct, etc.)
3. Funzione generateQualityInsights() che implementa le regole da quality_insights.insight_rules
4. Commenti di validazione che mostrano il path YAML

NON usare valori hardcoded! Tutti i numeri devono venire dal YAML.
```

### Output Atteso

```javascript
// ============================================
// Quality Insights - Generated from YAML
// Source: color-rules-config-COMPLETE.yaml
// ============================================

const qualityRules = {
    // From: dimension_scores.thresholds.excellent.value
    dimensionThresholds: {
        COMPLETENESS: 90,  // YAML: 90.0
        ACCURACY: 90,       // YAML: 90.0
        CONSISTENCY: 90,
        TIMELINESS: 90,
        UNIQUENESS: 90,
        VALIDITY: 90
    },

    // From: error_distribution.thresholds
    errorThresholds: {
        critical: 100,   // YAML: 100
        high: 50,        // YAML: 50
        medium: 10       // YAML: 10
    },

    // From: quality_insights.severity_thresholds.critical
    severityThresholds: {
        critical: {
            overallScore: 65.0,        // YAML: overall_score_below
            validationRate: 20.0,      // YAML: validation_rate_below
            errorRate: 50.0            // YAML: error_rate_above
        },
        // ... etc
    }
};

// From: quality_insights.insight_rules
const generateQualityInsights = (qualityReport) => {
    const insights = [];

    // Rule 1: critical_situation
    // From: quality_insights.insight_rules[0]
    if (qualityReport.overallScore < 65.0) {  // YAML: severity_thresholds.critical.overall_score_below
        insights.push({
            severity: 'critical',
            icon: '🚨',
            title: 'Situazione Critica - Azione Immediata Richiesta',
            content: `Il punteggio complessivo di qualità del ${qualityReport.overallScore}% indica...`
        });
    }

    // Rule 2: dimension_below_threshold
    // From: quality_insights.insight_rules[1]
    const worstDimensions = rankDimensionsByPriority(qualityReport.dimensions)
        .slice(0, 3);  // YAML: quality_insights.analysis_config.top_dimensions_to_analyze

    worstDimensions.forEach(dim => {
        if (dim.score < 90) {  // YAML: dimension_scores.thresholds.excellent.value
            // Generate dimension-specific insight
        }
    });

    return insights;
};
```

---

## 🔧 Task 2: Genera HTML per Dimension Scores

### Prompt per Copilot

```
Genera HTML Thymeleaf per la sezione "Completeness" score usando color-rules-config-COMPLETE.yaml.

Usa:
- Variable: cpl (da dimension_scores.dimensions.completeness.variable_score)
- Error var: cplErr (da dimension_scores.dimensions.completeness.variable_error)
- Label IT: "Completezza (Completeness)" (da dimension_scores.dimensions.completeness.label_it)
- Description: "Tutti i dati richiesti sono presenti e popolati"
- Threshold excellent: 90.0 (da dimension_scores.thresholds.excellent.value)
- Threshold acceptable: 75.0 (da dimension_scores.thresholds.acceptable.value)

Colori da global.color_palette:
- Green: text-green-600, bg-green-600
- Amber: text-amber-600, bg-amber-500
- Red: text-red-600, bg-red-600

Usa th:classappend con logica dinamica, NON class hardcoded!
```

### Output Atteso

```html
<!-- Completeness - Generated from YAML -->
<!-- Source: dimension_scores.dimensions.completeness -->
<div>
    <div class="flex justify-between items-center mb-2">
        <div>
            <!-- From: dimension_scores.dimensions.completeness.label_it -->
            <h4 class="text-lg font-bold text-gray-900">Completezza (Completeness)</h4>
            <!-- From: dimension_scores.dimensions.completeness.description_it -->
            <p class="text-sm text-gray-600">Tutti i dati richiesti sono presenti e popolati</p>
        </div>
        <div class="text-right">
            <!-- Score percentage with dynamic color -->
            <!-- Thresholds from: dimension_scores.thresholds -->
            <!-- excellent: 90.0, acceptable: 75.0 -->
            <span class="text-3xl font-bold"
                  th:classappend="${cpl >= 90 ? 'text-green-600' :
                                  cpl >= 75 ? 'text-amber-600' :
                                  'text-red-600'}"
                  th:text="${#numbers.formatDecimal(cpl, 1, 1)} + '%'">
                0.0%
            </span>

            <!-- Error count with matching color -->
            <p class="text-xs font-semibold"
               th:classappend="${cpl >= 90 ? 'text-green-600' :
                               cpl >= 75 ? 'text-amber-600' :
                               'text-red-600'}"
               th:text="${cplErr} + ' ERRORI'">
                0 ERRORI
            </p>
        </div>
    </div>

    <!-- Progress bar -->
    <div class="w-full bg-gray-200 rounded-full h-4">
        <div class="h-4 rounded-full"
             th:style="${'width: ' + cpl + '%'}"
             th:classappend="${cpl >= 90 ? 'bg-green-600' :
                             cpl >= 75 ? 'bg-amber-500' :
                             'bg-red-600'}">
        </div>
    </div>
</div>
```

---

## 🔧 Task 3: Genera Unit Test

### Prompt per Copilot

```
Genera un test JUnit 6 per verificare i colori del dimension score usando color-rules-config-COMPLETE.yaml.

Test per verificare:
- Score 100% → GREEN text + GREEN bar
- Score 90% → GREEN text + GREEN bar (boundary)
- Score 89.9% → AMBER text + AMBER bar
- Score 75% → AMBER text + AMBER bar (boundary)
- Score 74.9% → RED text + RED bar
- Score 50% → RED text + RED bar

Usa le soglie dal YAML:
- dimension_scores.thresholds.excellent.value = 90.0
- dimension_scores.thresholds.acceptable.value = 75.0
```

### Output Atteso

```java
// Generated from: color-rules-config-COMPLETE.yaml
// dimension_scores.thresholds.excellent.value = 90.0
// dimension_scores.thresholds.acceptable.value = 75.0

@ParameterizedTest
@CsvSource({
    "100.0, text-green-600, bg-green-600, 'Perfect score'",
    "90.0,  text-green-600, bg-green-600, 'Boundary at excellent threshold'",  // YAML: 90.0
    "89.9,  text-amber-600, bg-amber-500, 'Just below excellent threshold'",
    "75.0,  text-amber-600, bg-amber-500, 'Boundary at acceptable threshold'", // YAML: 75.0
    "74.9,  text-red-600,   bg-red-600,   'Just below acceptable threshold'",
    "50.0,  text-red-600,   bg-red-600,   'Poor score'"
})
void shouldHaveConsistentColors(double score, String expectedTextColor, String expectedBarColor, String description) {
    // Given
    QualityResults results = createResults(score);

    // When
    String html = generator.generate(results);

    // Then - Text color
    assertThat(html)
        .as(description + " - Text color")
        .contains(expectedTextColor);

    // Then - Bar color
    assertThat(html)
        .as(description + " - Bar color")
        .contains(expectedBarColor);

    // CRITICAL: Text and bar MUST match (from validation rules)
    if (expectedTextColor.contains("green")) {
        assertThat(expectedBarColor).contains("green");
    }
}
```

---

## 🔧 Task 4: Genera Documentazione

### Prompt per Copilot

```
Genera documentazione Markdown che descrive le regole di colore usando color-rules-config-COMPLETE.yaml.

Includi:
- Tabella con dimension score thresholds
- Tabella con error distribution thresholds
- Esempi visivi
- Regole di validazione

Usa i valori esatti dal YAML.
```

### Output Atteso

```markdown
# Color Consistency Rules

## Dimension Score Thresholds

<!-- From: dimension_scores.thresholds -->

| Score Range | Color | Threshold Source |
|-------------|-------|------------------|
| ≥ 90.0% | 🟢 GREEN | `dimension_scores.thresholds.excellent.value` |
| ≥ 75.0% and < 90.0% | 🟡 AMBER | `dimension_scores.thresholds.acceptable.value` |
| < 75.0% | 🔴 RED | Implicit (below acceptable) |

## Error Distribution Thresholds

<!-- From: error_distribution.thresholds -->

| Error Count | Severity | Color | Threshold Source |
|-------------|----------|-------|------------------|
| > 100 | CRITICAL | 🔴 RED | `error_distribution.thresholds.critical.value` |
| > 50 and ≤ 100 | HIGH | 🟠 ORANGE | `error_distribution.thresholds.high.value` |
| > 10 and ≤ 50 | MEDIUM | 🟡 YELLOW | `error_distribution.thresholds.medium.value` |
| ≤ 10 | LOW | 🟢 GREEN | Implicit |

## Quality Insights Severity

<!-- From: quality_insights.severity_thresholds -->

| Condition | Severity | Icon | Source |
|-----------|----------|------|--------|
| Overall score < 65% | CRITICAL | 🚨 | `quality_insights.severity_thresholds.critical.overall_score_below` |
| Overall score < 75% | HIGH | ⚠️ | `quality_insights.severity_thresholds.high.overall_score_below` |
| Overall score < 85% | MEDIUM | ⚡ | `quality_insights.severity_thresholds.medium.overall_score_below` |
```

---

## ✅ Checklist di Validazione

Dopo che Copilot ha generato il codice, verifica:

### JavaScript

- [ ] Tutti i thresholds vengono dal YAML (no hardcoded)
- [ ] `dimensionThresholds.COMPLETENESS` = 90 (non 80, 85, etc.)
- [ ] `errorThresholds.critical` = 100 (non 500, 200, etc.)
- [ ] Commenti mostrano il path YAML di origine
- [ ] Funzioni usano i thresholds dalle costanti

### HTML

- [ ] Usa `th:classappend` (non `class` hardcoded)
- [ ] Thresholds sono 90 e 75 (non altri valori)
- [ ] Colori: green-600, amber-600, red-600 (dal palette)
- [ ] Text color === Bar color (stesse soglie)

### Test

- [ ] Assertions usano i valori dal YAML
- [ ] Boundary tests a 90.0 e 75.0
- [ ] Test name include "(from YAML)" o simile
- [ ] Expected values documentati con commento YAML path

---

## 🚨 Errori Comuni da Evitare

### ❌ ERRORE 1: Hardcoded Values

```javascript
// ❌ SBAGLIATO
const dimensionThresholds = {
    COMPLETENESS: 80  // Hardcoded! Non viene dal YAML!
};

// ✅ CORRETTO
// From: dimension_scores.thresholds.excellent.value = 90.0
const dimensionThresholds = {
    COMPLETENESS: 90  // YAML value
};
```

### ❌ ERRORE 2: Thresholds Inconsistenti

```javascript
// ❌ SBAGLIATO
const textThreshold = 90;
const barThreshold = 85;  // Diverso!

// ✅ CORRETTO
// Both from: dimension_scores.thresholds.excellent.value
const threshold = 90;
```

### ❌ ERRORE 3: Mancanza di Commenti

```javascript
// ❌ SBAGLIATO
const critical = 100;  // Da dove viene?

// ✅ CORRETTO
// From: error_distribution.thresholds.critical.value
const critical = 100;
```

---

## 🎯 Prompt Template Generico

Usa questo template per qualsiasi task di code generation:

```
Genera [TIPO_FILE] usando color-rules-config-COMPLETE.yaml.

Path YAML da usare:
- [PATH_1]: [VALORE]
- [PATH_2]: [VALORE]
- [PATH_3]: [VALORE]

Requisiti:
1. NON usare valori hardcoded
2. Tutti i numeri devono venire dal YAML
3. Aggiungi commenti con il path YAML di origine
4. [ALTRI REQUISITI SPECIFICI]

Output atteso:
[DESCRIZIONE OUTPUT]
```

**Esempio:**

```
Genera JavaScript per error distribution usando color-rules-config-COMPLETE.yaml.

Path YAML da usare:
- error_distribution.thresholds.critical.value: 100
- error_distribution.thresholds.high.value: 50
- error_distribution.thresholds.medium.value: 10

Requisiti:
1. NON usare valori hardcoded
2. Crea const errorThresholds
3. Aggiungi commenti con YAML path

Output atteso:
File JavaScript con const errorThresholds che usa i valori dal YAML.
```

---

## 📚 Risorse

### File da Leggere

1. **color-rules-config-COMPLETE.yaml** - Configurazione completa
2. **LLM-RULE-EXTRACTION-GUIDE.md** - Guida dettagliata
3. **QUICK-START-RULE-SYSTEM.md** - Quick start

### Sezioni YAML Principali

- `global.color_palette` - Colori Tailwind
- `dimension_scores.thresholds` - Soglie dimension score
- `error_distribution.thresholds` - Soglie error count
- `quality_insights.insight_rules` - Regole insight
- `quality_insights.severity_thresholds` - Soglie severity

### Percorsi YAML Utili

```yaml
# Dimension score thresholds
dimension_scores.thresholds.excellent.value          # 90.0
dimension_scores.thresholds.acceptable.value         # 75.0

# Error distribution thresholds
error_distribution.thresholds.critical.value         # 100
error_distribution.thresholds.high.value             # 50
error_distribution.thresholds.medium.value           # 10

# Quality insights
quality_insights.severity_thresholds.critical.overall_score_below   # 65.0
quality_insights.analysis_config.top_dimensions_to_analyze          # 3

# Colors
global.color_palette.green.text                      # "text-green-600"
global.color_palette.amber.bg                        # "bg-amber-500"
```

---

## 🤝 Come Usare con Copilot

### In VS Code

1. **Apri il file YAML**

   ```
   color-rules-config-COMPLETE.yaml
   ```

2. **Apri questo file di istruzioni**

   ```
   COPILOT-INSTRUCTIONS.md
   ```

3. **Crea un nuovo file** (es: `quality-insights.js`)

4. **Usa il prompt** dal Task corrispondente

5. **Copilot genererà il codice** basato sul YAML

### Verifica Output

Dopo la generazione:

1. Cerca valori hardcoded (es: `80`, `85`, `500`)
2. Verifica commenti con path YAML
3. Confronta con i valori nel YAML
4. Run test suite per validare

---

## 📝 Note Finali

- **SEMPRE** verifica che i valori vengano dal YAML
- **MAI** usare valori hardcoded
- **SEMPRE** aggiungi commenti con il path YAML
- **VERIFICA** consistency tra HTML, JavaScript, e Test

**Il file YAML è la SINGLE SOURCE OF TRUTH!** 🎯

---

## 🧰 Repository Helpers (opzionale)

Questo repository include anche uno script di supporto per validare/generare snippet da regole YAML:

- Script: `tools/color_rules/code-generator.py`

Se vuoi usarlo come supporto (oltre a Copilot), eseguilo dalla root del repo con Python configurato.
