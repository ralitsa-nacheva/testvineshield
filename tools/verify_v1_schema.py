#!/usr/bin/env python3
"""Verify the frozen v1 export and its SQL using only Python's standard library.

This does not compile Room, inspect a user's database or test a version upgrade.
Run from any directory; all paths are relative to this script's project root.
"""
from pathlib import Path
import hashlib
import json
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json"
EXPECTED_SHA256 = "015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2"

def check(condition, message):
    if not condition:
        raise SystemExit(message)

def main():
    raw = SCHEMA.read_bytes()
    check(hashlib.sha256(raw).hexdigest() == EXPECTED_SHA256,
          "The frozen v1 export changed. Inspect schema drift; do not refresh its checksum to hide a migration.")
    schema = json.loads(raw)["database"]
    check(schema["version"] == 1, "Expected the archived v1 schema.")
    names = [entity["tableName"] for entity in schema["entities"]]
    check(len(names) == len(set(names)) == 20, "Unexpected or duplicate v1 tables.")
    with sqlite3.connect(":memory:") as connection:
        connection.execute("PRAGMA foreign_keys = ON")
        for entity in schema["entities"]:
            name = entity["tableName"]
            connection.execute(entity["createSql"].replace("${TABLE_NAME}", name))
            for index in entity.get("indices", []):
                connection.execute(index["createSql"].replace("${TABLE_NAME}", name))
        for statement in schema["setupQueries"]:
            connection.execute(statement)
        tables = {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        check(set(names) <= tables, "Not all exported tables were created.")
        check(connection.execute("PRAGMA integrity_check").fetchone() == ("ok",), "Invalid exported SQL schema.")
        check(not connection.execute("PRAGMA foreign_key_check").fetchall(), "Exported schema foreign-key failure.")
        identity = connection.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()
        check(identity == (schema["identityHash"],), "Room setup identity differs from the exported identity.")
    print(json.dumps({"status": "passed", "scope": "frozen v1 export bytes and SQL only",
                      "version": 1, "table_count": len(names), "sha256": EXPECTED_SHA256,
                      "identity_hash": schema["identityHash"],
                      "not_tested": ["Room generation", "Kotlin execution", "version migration", "backup or restore", "encryption"]}, indent=2))

if __name__ == "__main__":
    main()
