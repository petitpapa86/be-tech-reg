#!/usr/bin/env python
"""BCBS 239 color rules generator.

This script is intentionally small and dependency-light.
It reads `color-rules-config.yaml` and emits HTML snippets that match
current Thymeleaf patterns in the report template.

Usage:
  python tools/color_rules/code-generator.py validate --config color-rules-config.yaml
  python tools/color_rules/code-generator.py generate --config color-rules-config.yaml --out tools/color_rules/generated

Notes:
- Requires PyYAML (see tools/color_rules/requirements.txt)
- This does not automatically patch the main template; it generates
  copy/paste-ready snippets.
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any, Dict


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
    cfg = _load_yaml(config_path)

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
