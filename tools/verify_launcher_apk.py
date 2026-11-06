#!/usr/bin/env python3
"""Read APK DEX class definitions and reject duplicate application classes.

Usage: python tools/verify_launcher_apk.py path/to/androidApp-debug.apk
No Android SDK, decompiler or third-party Python dependency is required.
This checks packaged class ownership, not application runtime/UI behaviour.
"""
import argparse
import collections
import json
import struct
import sys
import zipfile
from pathlib import Path

PREFIX = "Lcom/rncoding/testvineshield/"
REQUIRED = {
    PREFIX + "MainActivity;": "Landroidx/fragment/app/FragmentActivity;",
    PREFIX + "AppKt;": "Ljava/lang/Object;",
    PREFIX + "TestVineshieldApplication;": "Landroid/app/Application;",
}

def class_definitions(data):
    if data[:4] != b"dex\n" or data[4:7] not in [b"035", b"037", b"038", b"039", b"040"]:
        raise ValueError("Unsupported DEX format; inspect with the Android APK Analyzer.")
    if len(data) < 112:
        raise ValueError("Truncated DEX header.")
    string_count, string_offset = struct.unpack_from("<II", data, 56)
    type_count, type_offset = struct.unpack_from("<II", data, 64)
    class_count, class_offset = struct.unpack_from("<II", data, 96)
    for count, offset, size in [(string_count, string_offset, 4), (type_count, type_offset, 4), (class_count, class_offset, 32)]:
        if offset + count * size > len(data):
            raise ValueError("DEX index extends beyond the file.")
    def string(index):
        if index >= string_count:
            raise ValueError("Invalid DEX string index.")
        position = struct.unpack_from("<I", data, string_offset + 4 * index)[0]
        # Skip the ULEB128 UTF-16 length; target class descriptors are ASCII.
        for _ in range(5):
            value = data[position]
            position += 1
            if value < 128:
                break
        else:
            raise ValueError("Invalid DEX string length.")
        return data[position:data.index(0, position)].decode("utf-8", errors="replace")
    def descriptor(index):
        if index >= type_count:
            raise ValueError("Invalid DEX type index.")
        return string(struct.unpack_from("<I", data, type_offset + 4 * index)[0])
    for index in range(class_count):
        class_index, _, superclass_index = struct.unpack_from("<III", data, class_offset + 32 * index)
        name = descriptor(class_index)
        if name.startswith(PREFIX):
            yield name, descriptor(superclass_index) if superclass_index != 0xffffffff else None

def inspect(apk):
    definitions = collections.defaultdict(list)
    with zipfile.ZipFile(apk) as archive:
        for item in archive.infolist():
            if item.filename.startswith("classes") and item.filename.endswith(".dex"):
                for name, superclass in class_definitions(archive.read(item)):
                    definitions[name].append({"dex": item.filename, "superclass": superclass})
    duplicates = {name: items for name, items in definitions.items() if len(items) != 1}
    invalid_required = {name: definitions.get(name, []) for name, superclass in REQUIRED.items()
                        if len(definitions.get(name, [])) != 1 or definitions[name][0]["superclass"] != superclass}
    return {"apk": str(apk), "status": "failed" if duplicates or invalid_required else "passed",
            "main_activity": definitions.get(PREFIX + "MainActivity;", []),
            "duplicate_application_classes": duplicates, "invalid_required_classes": invalid_required,
            "scope": "DEX class definitions only; runtime/manual checks still required"}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    try:
        report = inspect(args.apk)
    except (OSError, ValueError, IndexError, struct.error, zipfile.BadZipFile) as error:
        print("APK inspection failed: " + str(error), file=sys.stderr)
        return 2
    print(json.dumps(report, indent=2))
    return 0 if report["status"] == "passed" else 1

if __name__ == "__main__":
    raise SystemExit(main())
