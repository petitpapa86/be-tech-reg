# Color Consistency Rules — Technical Documentation

This document describes the conditional Tailwind color rules used by the BCBS 239 comprehensive report template.

Source template: regtech-report-generation/infrastructure/src/main/resources/templates/reports/comprehensive-report.html

## Quick reference

### Dimension scores (Punteggi per Dimensione di Qualità)

| Score range | Meaning | Text class | Bar fill class |
|---|---|---|---|
| ≥ 90% | Excellent | text-green-600 | bg-green-600 |
| 75–89% | Acceptable | text-amber-600 | bg-amber-500 |
| < 75% | Poor | text-red-600 | bg-red-600 |

Applies to:
- score percentage text
- error count text
- progress bar fill

### Error distribution (Distribuzione Errori per Dimensione)

| Error count | Severity | Background / border | Text primary | Bar bg / fill |
|---:|---|---|---|---|
| > 100 | CRITICAL | bg-red-50 / border-red-200 | text-red-800 | bg-red-200 / bg-red-600 |
| 51–100 | HIGH | bg-orange-50 / border-orange-200 | text-orange-800 | bg-orange-200 / bg-orange-600 |
| 11–50 | MEDIUM | bg-yellow-50 / border-yellow-200 | text-yellow-800 | bg-yellow-200 / bg-yellow-600 |
| ≤ 10 | LOW | bg-green-50 / border-green-200 | text-green-800 | bg-green-200 / bg-green-600 |

### Overall quality grade card

This is a **grade-letter mapping** (A/B/C/D/else) for a gradient background.

| Grade | Gradient classes |
|---|---|
| A | from-green-500 to-green-600 |
| B | from-amber-500 to-amber-600 |
| C | from-yellow-500 to-yellow-600 |
| D | from-orange-500 to-orange-600 |
| else (E/F) | from-red-500 to-red-600 |

### BCBS 239 compliance badges

| Condition | Badge classes | Label |
|---|---|---|
| assessment.compliant == true | bg-green-100 text-green-800 | CONFORME |
| assessment.compliant == false | bg-red-100 text-red-800 | NON CONFORME |

Per-principle thresholds are computed server-side in `HtmlReportGeneratorImpl#prepareBCBS239Compliance`:
- Principle 3 (Accuracy): 90
- Principle 4 (Completeness): 90
- Principle 5 (Timeliness): 90
- Principle 6 (Adaptability / Consistency): 70

## Single source of truth

All rules are centralized in `color-rules-config.yaml`.

