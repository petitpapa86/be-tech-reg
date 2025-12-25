# Modification Examples — Color Rules

All examples assume you modify `color-rules-config-COMPLETE.yaml`, then re-generate the snippet HTML with the generator.

## Example 1 — Change “Excellent” threshold from 90% to 95%

**Goal:** Make green start at 95%.

1. Edit `color-rules-config-COMPLETE.yaml`:
   - Set `dimension_scores.thresholds.excellent.percentage` to `95.0`
   - Set `dimension_scores.thresholds.acceptable.min_percentage` to `75.0` (unchanged) OR adjust if you want amber to start at a different boundary.

   If you want the boundaries to remain adjacent (no gap / no overlap):
   - Set `dimension_scores.thresholds.excellent.percentage = 95.0`
   - Keep `dimension_scores.thresholds.acceptable.min_percentage = 75.0`

2. Regenerate snippets:
   - `python tools/color_rules/code-generator.py generate --config color-rules-config-COMPLETE.yaml --out tools/color_rules/generated`

3. Update the actual template:
   - Replace the Dimension Scores blocks in the Thymeleaf template with the generated snippet.

4. Run tests:
   - `./mvnw -pl regtech-report-generation -am test -Dtest=HtmlReportGeneratorImplTest`

## Example 2 — Change error distribution “critical” threshold from >100 to >150

1. Edit `color-rules-config-COMPLETE.yaml`:
   - Set `error_distribution.thresholds.critical.min_count` to `151`
   - Set `error_distribution.thresholds.high.max_count` to `150`

2. Regenerate snippets:
   - `python tools/color_rules/code-generator.py generate --config color-rules-config-COMPLETE.yaml --out tools/color_rules/generated`

3. Update the template’s `th:with="severity=..."` thresholds accordingly.

4. Run tests:
   - `./mvnw -pl regtech-report-generation -am test -Dtest=ErrorDistributionColorConsistencyTemplateTest`

