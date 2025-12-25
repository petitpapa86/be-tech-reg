# Validation Report — Color Rules

Date: 2025-12-23

This report summarizes consistency checks for the conditional color rules defined in `color-rules-config.yaml`.

## Extraction completeness

- ✅ Dimension score rules found for all 6 dimensions (cpl/acc/con/tim/uni/val)
- ✅ Error distribution rules found for all 3 cards (cplCnt/accCnt/conCnt)
- ✅ Overall grade gradient mapping extracted (A/B/C/D/else)
- ✅ BCBS 239 compliance badge rule extracted (assessment.compliant)
- ✅ Additional conditional mappings extracted (risk themes, limit badge, attention level, compliance status)

## Threshold consistency

- ✅ Dimension score thresholds are consistent across score text, error count text, and bar fill:
  - Green: score ≥ 90
  - Amber: score ≥ 75 and < 90
  - Red: score < 75

- ✅ Error distribution thresholds are consistent across all elements per card:
  - Critical: count > 100
  - High: count > 50
  - Medium: count > 10
  - Low: count ≤ 10

## Color mapping consistency

- ✅ `text-green-600` pairs with `bg-green-600` for the "excellent" tier
- ✅ `text-amber-600` pairs with `bg-amber-500` for the "acceptable" tier (intentional)
- ✅ `text-red-600` pairs with `bg-red-600` for the "poor" tier

## Known non-Thymeleaf color logic in template

- The inline JS contains additional conditional color logic/palettes for the “Quality insights” cards and risk charts.
  These are captured in `color-rules-config.yaml` under `additional_rules.quality_insights_palette_js`.

## Suggested automated checks (optional)

- Parse the template and assert that:
  - the 6 dimension score blocks contain the configured thresholds (90/75) for both text and bar.
  - each error distribution card uses the same `severity` variable for all color-bearing elements.
