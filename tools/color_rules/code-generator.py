#!/usr/bin/env python
"""BCBS 239 color rules generator.

This script is intentionally small and dependency-light.

It uses a single source of truth YAML:
- `color-rules-config-COMPLETE.yaml`

For backward compatibility, it can still read the legacy schema if you provide a legacy file path,
but the repository is intended to keep only the COMPLETE YAML.

Internally, COMPLETE YAML is normalized to the legacy structure so the
snippet generation stays stable.

Usage:
    python tools/color_rules/code-generator.py validate --config color-rules-config-COMPLETE.yaml
    python tools/color_rules/code-generator.py generate --config color-rules-config-COMPLETE.yaml --out tools/color_rules/generated

Notes:
- Requires PyYAML (see tools/color_rules/requirements.txt)
- This does not automatically patch the main template; it generates
    copy/paste-ready snippets.
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any, Dict, Tuple


DEFAULT_TEMPLATES: Dict[str, str] = {
    "dimension_score_html": """<!-- @@DIMENSION_NAME@@ -->
<div>
    <div class=\"flex justify-between items-center mb-2\">
        <div>
            <h4 class=\"text-lg font-bold text-gray-900\">@@LABEL_IT@@ (@@LABEL_EN@@)</h4>
            <p class=\"text-sm text-gray-600\">@@DESCRIPTION_IT@@</p>
        </div>
        <div class=\"text-right\">
            <span class=\"text-3xl font-bold\"
                  th:classappend=\"${@@VAR@@ != null && @@VAR@@ >= @@THRESH_EXCELLENT@@ ? 'text-green-600' :
                                  @@VAR@@ != null && @@VAR@@ >= @@THRESH_ACCEPTABLE@@ ? 'text-amber-600' :
                                  'text-red-600'}\"
                  th:text=\"${#numbers.formatDecimal((@@VAR@@ != null ? @@VAR@@ : 0), 1, 1)} + '%'\"><span>0.0%</span></span>
            <p class=\"text-xs font-semibold\"
               th:classappend=\"${@@VAR@@ != null && @@VAR@@ >= @@THRESH_EXCELLENT@@ ? 'text-green-600' :
                               @@VAR@@ != null && @@VAR@@ >= @@THRESH_ACCEPTABLE@@ ? 'text-amber-600' :
                               'text-red-600'}\"><span th:text=\"${@@ERROR_VAR@@}\">0</span> @@ERROR_LABEL_IT@@</p>
        </div>
    </div>
    <div class=\"w-full bg-gray-200 rounded-full h-4\">
        <div class=\"h-4 rounded-full\"
             th:style=\"${'width: ' + (@@VAR@@ != null ? @@VAR@@ : 0) + '%'}\"
             th:classappend=\"${@@VAR@@ != null && @@VAR@@ >= @@THRESH_EXCELLENT@@ ? 'bg-green-600' :
                             @@VAR@@ != null && @@VAR@@ >= @@THRESH_ACCEPTABLE@@ ? 'bg-amber-500' :
                             'bg-red-600'}\"></div>
    </div>
</div>
""",
    "error_distribution_card_html": """<!-- @@DIMENSION_NAME@@ Error Card -->
<div class=\"rounded-lg p-6 border-2\"
   th:with=\"severity=${@@ERROR_VAR@@ > @@THRESH_CRITICAL@@ ? 'critical' : @@ERROR_VAR@@ > @@THRESH_HIGH@@ ? 'high' : @@ERROR_VAR@@ > @@THRESH_MEDIUM@@ ? 'medium' : 'low'}\"
   th:classappend=\"${severity == 'critical' ? 'bg-red-50 border-red-200' :
           severity == 'high' ? 'bg-orange-50 border-orange-200' :
           severity == 'medium' ? 'bg-yellow-50 border-yellow-200' :
           'bg-green-50 border-green-200'}\">
  <div class=\"flex justify-between items-start mb-4\">
    <div>
      <p class=\"text-sm font-semibold\"
         th:classappend=\"${severity == 'critical' ? 'text-red-800' :
                 severity == 'high' ? 'text-orange-800' :
                 severity == 'medium' ? 'text-yellow-800' :
                 'text-green-800'}\">@@DIMENSION_LABEL@@</p>
      <p class=\"text-xs mt-1\"
         th:classappend=\"${severity == 'critical' ? 'text-red-600' :
                 severity == 'high' ? 'text-orange-600' :
                 severity == 'medium' ? 'text-yellow-600' :
                 'text-green-600'}\"
         th:text=\"${severity == 'critical' ? '@@LABEL_CRITICAL_IT@@' :
             severity == 'high' ? '@@LABEL_HIGH_IT@@' :
             severity == 'medium' ? '@@LABEL_MEDIUM_IT@@' :
             '@@LABEL_LOW_IT@@'}\">@@SUBTITLE_FALLBACK_IT@@</p>
    </div>
    <span class=\"text-3xl font-bold\"
        th:classappend=\"${severity == 'critical' ? 'text-red-600' :
                severity == 'high' ? 'text-orange-600' :
                severity == 'medium' ? 'text-yellow-600' :
                'text-green-600'}\"
        th:text=\"${@@ERROR_VAR@@}\">0</span>
  </div>
  <div class=\"w-full rounded-full h-3\"
     th:classappend=\"${severity == 'critical' ? 'bg-red-200' :
             severity == 'high' ? 'bg-orange-200' :
             severity == 'medium' ? 'bg-yellow-200' :
             'bg-green-200'}\">
    <div class=\"h-3 rounded-full\"
       th:classappend=\"${severity == 'critical' ? 'bg-red-600' :
               severity == 'high' ? 'bg-orange-600' :
               severity == 'medium' ? 'bg-yellow-600' :
               'bg-green-600'}\"
       th:style=\"${'width: ' + (maxCnt > 0 ? ((@@ERROR_VAR@@ * 100.0) / maxCnt) : 0) + '%'}\" style=\"width: 100%\"></div>
  </div>
    <p class=\"text-xs mt-2\"
     th:classappend=\"${severity == 'critical' ? 'text-red-700' :
             severity == 'high' ? 'text-orange-700' :
             severity == 'medium' ? 'text-yellow-700' :
             'text-green-700'}\"><span th:text=\"${qualityResults.totalErrors > 0 ? #numbers.formatDecimal((@@ERROR_VAR@@ * 100.0) / qualityResults.totalErrors, 1, 1) : '0.0'}\">62.5</span>% degli errori totali</p>
    </div>
""",
}


def _load_yaml(path: Path) -> Dict[str, Any]:
    try:
        import yaml  # type: ignore
    except Exception as exc:  # pragma: no cover
        raise SystemExit(
            "PyYAML is required. Install with: pip install -r tools/color_rules/requirements.txt"
        ) from exc

    with path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict):
        raise SystemExit("Invalid YAML: expected top-level mapping")
    return data


def _is_complete_schema(raw: Dict[str, Any]) -> bool:
    dim = raw.get("dimension_scores")
    err = raw.get("error_distribution")
    return isinstance(raw.get("global"), dict) and isinstance(dim, dict) and isinstance(err, dict) and "enabled" in dim


def _as_float(value: Any, *, path: str) -> float:
    try:
        return float(value)
    except Exception as exc:
        raise SystemExit(f"Invalid numeric value at {path}: {value!r}") from exc


def _as_int(value: Any, *, path: str) -> int:
    try:
        return int(value)
    except Exception as exc:
        raise SystemExit(f"Invalid integer value at {path}: {value!r}") from exc


def _normalize_complete(raw: Dict[str, Any]) -> Dict[str, Any]:
    dim_raw = raw.get("dimension_scores") or {}
    dim_thresholds_raw = dim_raw.get("thresholds") or {}

    excellent_value = _as_float((dim_thresholds_raw.get("excellent") or {}).get("value"), path="dimension_scores.thresholds.excellent.value")
    acceptable_value = _as_float((dim_thresholds_raw.get("acceptable") or {}).get("value"), path="dimension_scores.thresholds.acceptable.value")

    def _clean_label_it(value: Any) -> str:
        s = (value or "").strip()
        # COMPLETE labels are like "Completezza (Completeness)"; legacy template already appends (English).
        if "(" in s and s.endswith(")"):
            return s.split("(", 1)[0].strip()
        return s

    dimensions_out: Dict[str, Any] = {}
    for key, d in (dim_raw.get("dimensions") or {}).items():
        if not isinstance(d, dict):
            continue
        dimensions_out[key] = {
            "variable": d.get("variable_score") or d.get("variable") or d.get("id") or "",
            "error_variable": d.get("variable_error") or d.get("error_variable") or "",
            "label_it": _clean_label_it(d.get("label_it")),
            "label_en": (d.get("label_en") or ""),
            "description_it": d.get("description_it") or "",
            "error_label_it": "ERRORI",
        }

    err_raw = raw.get("error_distribution") or {}
    err_thresholds_raw = err_raw.get("thresholds") or {}

    def _mk_min_count(threshold_value: Any, *, path: str) -> int:
        # COMPLETE schema expresses rules as "> value". Legacy schema expects min_count (value+1).
        return _as_int(threshold_value, path=path) + 1

    critical_value = (err_thresholds_raw.get("critical") or {}).get("value")
    high_value = (err_thresholds_raw.get("high") or {}).get("value")
    medium_value = (err_thresholds_raw.get("medium") or {}).get("value")

    thresholds_out = {
        "critical": {
            "min_count": _mk_min_count(critical_value, path="error_distribution.thresholds.critical.value"),
            "label_it": (err_thresholds_raw.get("critical") or {}).get("label_it", "Situazione critica!"),
        },
        "high": {
            "min_count": _mk_min_count(high_value, path="error_distribution.thresholds.high.value"),
            "label_it": (err_thresholds_raw.get("high") or {}).get("label_it", "Richiede attenzione"),
        },
        "medium": {
            "min_count": _mk_min_count(medium_value, path="error_distribution.thresholds.medium.value"),
            "label_it": (err_thresholds_raw.get("medium") or {}).get("label_it", "Errori moderati"),
        },
        "low": {
            "max_count": _as_int((err_thresholds_raw.get("low") or {}).get("threshold_value", 10), path="error_distribution.thresholds.low.threshold_value"),
            "label_it": (err_thresholds_raw.get("low") or {}).get("label_it", "Errori minimi"),
        },
    }

    # Error cards in the report are the top-3 dimensions (historically: completeness, accuracy, consistency).
    dim_defs = dim_raw.get("dimensions") or {}
    error_cards: Dict[str, Any] = {}
    ordered = [
        ("completeness", "COMPLETENESS", "Dimensione più problematica"),
        ("accuracy", "ACCURACY", "Seconda dimensione critica"),
        ("consistency", "CONSISTENCY", "Terza dimensione critica"),
    ]
    for key, label, subtitle in ordered:
        d = dim_defs.get(key) or {}
        if not isinstance(d, dict):
            continue
        var_cnt = d.get("variable_count")
        if not var_cnt:
            continue
        error_cards[key] = {
            "variable": var_cnt,
            "label": label,
            "subtitle_fallback_it": subtitle,
        }

    return {
        "dimension_scores": {
            "thresholds": {
                "excellent": {"percentage": excellent_value},
                "acceptable": {"min_percentage": acceptable_value},
            },
            "dimensions": dimensions_out,
        },
        "error_distribution": {
            "thresholds": thresholds_out,
            "dimensions": error_cards,
        },
        # COMPLETE YAML does not carry snippet templates in the same way; use defaults.
        "templates": DEFAULT_TEMPLATES.copy(),
    }


def _normalize_config(raw: Dict[str, Any]) -> Tuple[Dict[str, Any], str]:
    if _is_complete_schema(raw):
        return _normalize_complete(raw), "complete"

    # Legacy schema: keep as-is, but ensure templates exist.
    legacy = dict(raw)
    templates = legacy.get("templates") or {}
    if not isinstance(templates, dict):
        templates = {}
    if "dimension_score_html" not in templates or "error_distribution_card_html" not in templates:
        merged = DEFAULT_TEMPLATES.copy()
        merged.update({k: v for k, v in templates.items() if isinstance(v, str)})
        legacy["templates"] = merged
    return legacy, "legacy"


def validate_rules(cfg: Dict[str, Any]) -> None:
    # Dimension score thresholds sanity
    dim = cfg.get("dimension_scores") or {}
    thresholds = (dim.get("thresholds") or {})

    excellent = thresholds.get("excellent") or {}
    acceptable = thresholds.get("acceptable") or {}

    excellent_pct = float(excellent.get("percentage"))
    acceptable_min = float(acceptable.get("min_percentage"))

    if excellent_pct <= acceptable_min:
        raise SystemExit(
            f"Invalid dimension score thresholds: excellent ({excellent_pct}) must be > acceptable.min ({acceptable_min})."
        )

    # Error distribution threshold ordering
    err = cfg.get("error_distribution") or {}
    t = err.get("thresholds") or {}

    critical_min = int((t.get("critical") or {}).get("min_count"))
    high_min = int((t.get("high") or {}).get("min_count"))
    medium_min = int((t.get("medium") or {}).get("min_count"))

    if not (critical_min > high_min > medium_min > 0):
        raise SystemExit(
            f"Invalid error count ordering: critical.min ({critical_min}) > high.min ({high_min}) > medium.min ({medium_min}) expected."
        )

    print("OK: config passes basic validation")


def _render_dimension_block(template: str, *, dim_cfg: Dict[str, Any], thresholds: Dict[str, Any]) -> str:
    exc = thresholds["excellent"]["percentage"]
    acceptable_min = thresholds["acceptable"]["min_percentage"]

    replacements = {
        "@@DIMENSION_NAME@@": dim_cfg.get("label_en", ""),
        "@@LABEL_IT@@": dim_cfg.get("label_it", ""),
        "@@LABEL_EN@@": dim_cfg.get("label_en", ""),
        "@@DESCRIPTION_IT@@": dim_cfg.get("description_it", ""),
        "@@ERROR_LABEL_IT@@": dim_cfg.get("error_label_it", "ERRORI"),
        "@@VAR@@": dim_cfg["variable"],
        "@@ERROR_VAR@@": dim_cfg["error_variable"],
        "@@THRESH_EXCELLENT@@": str(exc),
        "@@THRESH_ACCEPTABLE@@": str(acceptable_min),
    }

    out = template
    for token, value in replacements.items():
        out = out.replace(token, str(value))
    return out


def _render_error_card(template: str, *, card_cfg: Dict[str, Any], thresholds: Dict[str, Any]) -> str:
    # Template expects "threshold_critical" etc as the comparison constants in the ternary.
    # Thymeleaf uses strict ">" comparisons in the current template.
    critical_cmp = int(thresholds["critical"]["min_count"]) - 1
    high_cmp = int(thresholds["high"]["min_count"]) - 1
    medium_cmp = int(thresholds["medium"]["min_count"]) - 1

    labels = cfg["error_distribution"]["thresholds"]
    replacements = {
        "@@DIMENSION_NAME@@": card_cfg.get("label", ""),
        "@@DIMENSION_LABEL@@": card_cfg.get("label", ""),
        "@@SUBTITLE_FALLBACK_IT@@": card_cfg.get("subtitle_fallback_it", ""),
        "@@ERROR_VAR@@": card_cfg["variable"],
        "@@THRESH_CRITICAL@@": str(critical_cmp),
        "@@THRESH_HIGH@@": str(high_cmp),
        "@@THRESH_MEDIUM@@": str(medium_cmp),
        "@@LABEL_CRITICAL_IT@@": str((labels.get("critical") or {}).get("label_it", "Situazione critica!")),
        "@@LABEL_HIGH_IT@@": str((labels.get("high") or {}).get("label_it", "Richiede attenzione")),
        "@@LABEL_MEDIUM_IT@@": str((labels.get("medium") or {}).get("label_it", "Errori moderati")),
        "@@LABEL_LOW_IT@@": str((labels.get("low") or {}).get("label_it", "Errori minimi")),
    }

    out = template
    for token, value in replacements.items():
        out = out.replace(token, str(value))
    return out


def _render_thymeleaf_fragments(*, dimension_blocks: list[str], error_cards: list[str]) -> str:
    # Keep this file minimal and includeable.
    return (
        "<!DOCTYPE html>\n"
        "<html xmlns:th=\"http://www.thymeleaf.org\">\n"
        "  <body>\n"
        "    <th:block th:fragment=\"dimensionScores\">\n"
        f"{('\n\n'.join(dimension_blocks)).strip()}\n"
        "    </th:block>\n\n"
        "    <th:block th:fragment=\"errorDistributionCards\">\n"
        f"{('\n\n'.join(error_cards)).strip()}\n"
        "    </th:block>\n"
        "  </body>\n"
        "</html>\n"
    )


def generate_snippets(cfg: Dict[str, Any], out_dir: Path, *, fragments_path: Path | None = None) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)

    templates = cfg.get("templates") or {}
    dim_tpl = templates.get("dimension_score_html")
    err_tpl = templates.get("error_distribution_card_html")

    if not dim_tpl or not err_tpl:
        raise SystemExit("Missing templates.dimension_score_html or templates.error_distribution_card_html")

    dim = cfg["dimension_scores"]
    thresholds = dim["thresholds"]

    # Dimension score blocks
    blocks: list[str] = []
    for _, dim_cfg in (dim.get("dimensions") or {}).items():
        blocks.append(_render_dimension_block(dim_tpl, dim_cfg=dim_cfg, thresholds=thresholds))

    (out_dir / "dimension-scores-section.snippet.html").write_text(
        "\n\n".join(blocks).strip() + "\n", encoding="utf-8"
    )

    # Error distribution cards
    err = cfg["error_distribution"]
    err_thresholds = err["thresholds"]

    cards: list[str] = []
    for _, card_cfg in (err.get("dimensions") or {}).items():
        cards.append(_render_error_card(err_tpl, card_cfg=card_cfg, thresholds=err_thresholds))

    (out_dir / "error-distribution-section.snippet.html").write_text(
        "\n\n".join(cards).strip() + "\n", encoding="utf-8"
    )

    if fragments_path is not None:
        fragments_path.parent.mkdir(parents=True, exist_ok=True)
        fragments_path.write_text(
            _render_thymeleaf_fragments(dimension_blocks=blocks, error_cards=cards),
            encoding="utf-8",
        )
        print(f"Wrote Thymeleaf fragments to: {fragments_path}")

    print(f"Wrote snippets to: {out_dir}")


def main() -> None:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_val = sub.add_parser("validate")
    p_val.add_argument("--config", required=True)

    p_gen = sub.add_parser("generate")
    p_gen.add_argument("--config", required=True)
    p_gen.add_argument("--out", required=True)
    p_gen.add_argument(
        "--write-fragments",
        required=False,
        help="Optional path to write a Thymeleaf fragments file (e.g. .../templates/reports/fragments/color-rules-generated.html)",
    )

    args = parser.parse_args()

    config_path = Path(args.config)
    if not config_path.exists():
        raise SystemExit(f"Config not found: {config_path}")

    global cfg
    raw = _load_yaml(config_path)
    cfg, schema = _normalize_config(raw)

    print(f"Loaded config schema: {schema}")

    if args.cmd == "validate":
        validate_rules(cfg)
        return

    if args.cmd == "generate":
        validate_rules(cfg)
        fragments_path = Path(args.write_fragments) if getattr(args, "write_fragments", None) else None
        generate_snippets(cfg, Path(args.out), fragments_path=fragments_path)
        return


if __name__ == "__main__":
    main()
