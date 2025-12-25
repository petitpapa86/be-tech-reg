# Rule Extraction Report (Color Consistency)

Source template: regtech-report-generation/infrastructure/src/main/resources/templates/reports/comprehensive-report.html

This report lists every location in the Thymeleaf HTML template where **colors are applied via conditional logic** (mostly `th:classappend` and `th:with`).

## Step 1 — Rule Locations Table

| Section Name | Line Number | Variable Used | Rule Type | Current Thresholds |
|---|---:|---|---|---|
| Sector Distribution — Sector badge | 199 | `row.theme` | Theme-based mapping | n/a |
| Top 10 Exposures — Card theme | 229 | `card.theme` | Theme-based mapping | n/a |
| Top 10 Exposures — Limit badge | 239 | `card.limitExceeded` | Boolean mapping | n/a |
| Stato Validazione — Compliance badge | 357 | `qualityResults.complianceStatus.name()` | Enum mapping | n/a |
| Stato Validazione — Attention container | 364 | `qualityResults.attentionLevel.name()` | Enum mapping | LOW / MEDIUM / else |
| Stato Validazione — Attention label | 367 | `qualityResults.attentionLevel.name()` | Enum mapping | LOW / MEDIUM / else |
| Stato Validazione — Attention badge | 369 | `qualityResults.attentionLevel.name()` | Enum mapping | LOW / MEDIUM / else |
| Punteggio Complessivo — Grade gradient | 395 | `g` | Grade mapping | n/a (grade already computed) |
| Dimension Scores — Completeness score text | 454 | `cpl` | Score-based | 90, 75 |
| Dimension Scores — Completeness error count text | 459 | `cpl` / `cplErr` | Score-based | 90, 75 |
| Dimension Scores — Completeness bar fill | 467 | `cpl` | Score-based | 90, 75 |
| Dimension Scores — Accuracy score text | 482 | `acc` | Score-based | 90, 75 |
| Dimension Scores — Accuracy error count text | 487 | `acc` / `accErr` | Score-based | 90, 75 |
| Dimension Scores — Accuracy bar fill | 495 | `acc` | Score-based | 90, 75 |
| Dimension Scores — Consistency score text | 510 | `con` | Score-based | 90, 75 |
| Dimension Scores — Consistency error count text | 515 | `con` / `conErr` | Score-based | 90, 75 |
| Dimension Scores — Consistency bar fill | 523 | `con` | Score-based | 90, 75 |
| Dimension Scores — Timeliness score text | 538 | `tim` | Score-based | 90, 75 |
| Dimension Scores — Timeliness error count text | 543 | `tim` / `timErr` | Score-based | 90, 75 |
| Dimension Scores — Timeliness bar fill | 551 | `tim` | Score-based | 90, 75 |
| Dimension Scores — Uniqueness score text | 566 | `uni` | Score-based | 90, 75 |
| Dimension Scores — Uniqueness error count text | 571 | `uni` / `uniErr` | Score-based | 90, 75 |
| Dimension Scores — Uniqueness bar fill | 579 | `uni` | Score-based | 90, 75 |
| Dimension Scores — Validity score text | 594 | `val` | Score-based | 90, 75 |
| Dimension Scores — Validity error count text | 599 | `val` / `valErr` | Score-based | 90, 75 |
| Dimension Scores — Validity bar fill | 607 | `val` | Score-based | 90, 75 |
| Error Distribution — Completeness severity | 627 | `cplCnt` | Count-based severity | 100, 50, 10 |
| Error Distribution — Completeness colors (all elements) | 628–669 | `severity` | Severity mapping | 100, 50, 10 |
| Error Distribution — Accuracy severity | 676 | `accCnt` | Count-based severity | 100, 50, 10 |
| Error Distribution — Accuracy colors (all elements) | 677–718 | `severity` | Severity mapping | 100, 50, 10 |
| Error Distribution — Consistency severity | 725 | `conCnt` | Count-based severity | 100, 50, 10 |
| Error Distribution — Consistency colors (all elements) | 726–767 | `severity` | Severity mapping | 100, 50, 10 |
| BCBS 239 Compliance — Compliance badge | 826 | `assessment.compliant` | Boolean mapping | thresholds in server-side generator: 90/70 |

## Notes

- The BCBS 239 per-principle thresholds are computed server-side in `HtmlReportGeneratorImpl#prepareBCBS239Compliance` (90% for Principles 3–5, 70% for Principle 6).
- The template also contains a **client-side “quality insights” palette** inside the inline `<script>` which maps severities to Tailwind classes (not Thymeleaf).
