#!/usr/bin/env python3
"""Static resource contract checks; not native localization/accessibility certification."""
from pathlib import Path
from collections import Counter
import argparse
import json
import plistlib
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def read_strings(path):
    nodes = list(ET.parse(path).getroot())
    names = [node.attrib["name"] for node in nodes]
    duplicates = [name for name, count in Counter(names).items() if count > 1]
    if duplicates:
        raise ValueError(f"Duplicate resource names in {path}: {duplicates}")
    if any(node.tag != "string" for node in nodes):
        raise ValueError("Review new non-string resource kinds explicitly")
    return {node.attrib["name"]: "".join(node.itertext()) for node in nodes}

def signature(text):
    # All current runtime placeholders use numbered string/integer substitutions.
    return sorted(re.findall(r"%(\d+)\$([sd])", text))

def check_pair(en, bg):
    if en.keys() != bg.keys():
        raise ValueError(f"Locale key mismatch: EN-only={sorted(en.keys()-bg.keys())}; BG-only={sorted(bg.keys()-en.keys())}")
    for name, text in en.items():
        if not text.strip() or not bg[name].strip():
            raise ValueError(f"Empty resource: {name}")
        if signature(text) != signature(bg[name]):
            raise ValueError(f"Placeholder mismatch: {name}")
        for value in (text, bg[name]):
            for number, kind in re.findall(r"(?<!%)%(?:(\d+)\$)?([A-Za-z])", value):
                if not number or kind not in {"s", "d"}:
                    raise ValueError(f"Unsupported or unnumbered placeholder: {name}")

def audit(root=ROOT):
    resources = root / "shared/src/commonMain/composeResources"
    en = read_strings(resources / "values/strings.xml")
    bg = read_strings(resources / "values-bg/strings.xml")
    check_pair(en, bg)
    # Explicit UTF-8 is required on Windows, regardless of the active code page.
    refs = set()
    for source in (root / "shared/src").rglob("*.kt"):
        found = set(re.findall(r"Res\.string\.(\w+)", source.read_text(encoding="utf-8")))
        missing = found - en.keys()
        if missing:
            raise ValueError(f"Missing resource reference in {source}: {sorted(missing)}")
        refs.update(found)
    android_ns = "{http://schemas.android.com/apk/res/android}"
    locales = ET.parse(root / "androidApp/src/main/res/xml/locale_config.xml").getroot()
    if {node.attrib[android_ns + "name"] for node in locales} != {"en", "bg"}:
        raise ValueError("Android locale declaration differs from the agreed language scope")
    with (root / "iosApp/iosApp/Info.plist").open("rb") as stream:
        ios = plistlib.load(stream)
    if set(ios.get("CFBundleLocalizations", [])) != {"en", "bg"}:
        raise ValueError("iOS language declaration differs from the agreed language scope")
    return dict(resource_pairs=len(en), referenced_keys=len(refs), placeholder_contracts="passed",
                platform_locale_declarations="en,bg", identical_values=sorted(k for k in en if en[k] == bg[k]),
                native_runtime_or_accessibility_verified=False)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = json.dumps(audit(), ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(result, encoding="utf-8")
    print(result, end="")
