#!/usr/bin/env python3
"""Bounded source checks for lazy identity and help placement; not a Kotlin parser or UI test."""
from pathlib import Path
from collections import Counter
import json
import re

ROOT = Path(__file__).resolve().parents[1]
PRESENTATION = Path("shared/src/commonMain/kotlin/com/rncoding/testvineshield/core/presentation")
HELP_KEYS = {
    "account_logout_help", "language_partial_help", "auth_unlock_help", "unit_change_note", "location_change_note",
    "weather_monitoring_intro", "risk_not_assessed", "cached_weather_note", "block_save_help", "block_unknown_help",
    "activity_basic_help", "activity_dates_help", "harvest_save_help", "harvest_unit_change_help", "harvest_units_help",
    "spray_purpose", "spray_legacy_help", "spray_save_help", "spray_unit_help", "spray_zone_help", "spray_rate_help",
    "harvest_export_contents", "harvest_export_plaintext", "harvest_export_csv_help", "spray_export_contents",
    "spray_export_plaintext", "spray_export_csv_help", "map_privacy", "map_position_help", "map_address_help",
    "map_zone_privacy", "observation_resolution_help", "observation_active_help", "symptom_date_help",
    "catalogue_signs_help", "alerts_scope", "alerts_provenance", "alerts_review_help", "weather_location_cache",
    "weather_offline_help", "weather_refresh_failure_help", "weather_unfinished_help", "weather_refresh_scope",
    "weather_history_coverage_help", "weather_forecast_help", "monitor_coverage_help", "monitor_enabled_help",
    "monitor_risk_help", "monitor_draft_help", "security_lock_help", "security_device_help", "security_disable_help"
}

def call_arguments(source, name):
    for match in re.finditer(r"\b" + name + r"\s*\(", source):
        start = match.end(); depth = 1; at = start
        while at < len(source) and depth:
            if source.startswith('//', at):
                end = source.find('\n', at); at = len(source) if end < 0 else end + 1; continue
            if source[at] == '"':
                at += 1
                while at < len(source):
                    if source[at] == "\\": at += 2; continue
                    if source[at] == '"': at += 1; break
                    at += 1
                continue
            if source[at] == '(': depth += 1
            if source[at] == ')': depth -= 1
            at += 1
        if depth: raise ValueError(f"Unbalanced {name} call")
        yield source[start:at-1]

def check_source(source, label):
    if re.search(r"\bitem\s*\{", source): raise ValueError(f"Unkeyed fixed item in {label}")
    for name in ("items", "itemsIndexed", "item"):
        for arguments in call_arguments(source, name):
            if not re.search(r"\bkey\s*=", arguments): raise ValueError(f"Unkeyed {name} in {label}")
    literals = re.findall(r'\bitem\(key\s*=\s*"([^"$]+)"', source)
    duplicates = [key for key, count in Counter(literals).items() if count > 1]
    if duplicates: raise ValueError(f"Repeated literal row keys in {label}: {duplicates}")
    for key in HELP_KEYS:
        if re.search(r"Text\(\s*stringResource\(Res.string." + key + r"\)", source):
            raise ValueError(f"Inline explanatory help remains in {label}: {key}")

def audit(root=ROOT):
    rows = []; combined = ""
    for path in sorted((root/PRESENTATION).rglob("*.kt")):
        source = path.read_text(encoding="utf-8")
        check_source(source, str(path.relative_to(root)))
        combined += source + "\n"
        lazy_count = len(re.findall(r"\bLazy(?:Column|Row)\s*\(", source))
        if lazy_count:
            rows.append({"file":str(path.relative_to(root)), "lazy_lists":lazy_count,
                         "record_declarations":len(list(call_arguments(source,"items")))+len(list(call_arguments(source,"itemsIndexed")))})
    for key in HELP_KEYS:
        if f"Res.string.{key}" not in combined: raise ValueError(f"Help content removed rather than relocated: {key}")
    details = (root/PRESENTATION/"vineyard_details/VineyardDetailsScreen.kt").read_text(encoding="utf-8")
    for field, kind in (("blockId","block"),("occurrenceId","occurrence"),("alertId","alert")):
        if f"VineyardDetailsItemKeys.{kind}(it.{field})" not in details: raise ValueError(f"Missing details namespace: {kind}")
    drag = (root/PRESENTATION/"vineyard_list/VineyardListScreen.kt").read_text(encoding="utf-8")
    if "vineyard.vineyardId" not in drag or "it.vineyardId == key" not in drag: raise ValueError("Review vineyard drag identity")
    return {"status":"passed", "reviewed_lazy_screen_files":rows, "help_resource_keys_preserved":len(HELP_KEYS),
            "limits":["Source inventory only; not a full Kotlin/Compose parser", "Record-ID uniqueness still depends on repository contracts",
                      "Literal-key duplicate check is file-wide (stricter than per-list)", "No native Compose, screen reader or scroll execution"]}

if __name__ == "__main__": print(json.dumps(audit(),ensure_ascii=False,indent=2))
