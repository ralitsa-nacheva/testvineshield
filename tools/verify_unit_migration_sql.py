#!/usr/bin/env python3
"""Execute the actual 1->2 migration SQL on the historical populated v1 fixture.
This is a SQLite SQL check, not Kotlin/Room compilation or device execution.
"""
from pathlib import Path
import hashlib
import json
import re
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "shared/src"
PKG = "kotlin/com/rncoding/testvineshield/core/data/local/database/migrations"

def main():
    schema_path = ROOT / "shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json"
    raw = schema_path.read_bytes()
    if hashlib.sha256(raw).hexdigest() != "015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2":
        raise SystemExit("Archived v1 schema changed; investigate drift.")
    schema = json.loads(raw)["database"]
    fixture = (BASE / "androidDeviceTest" / PKG / "SchemaV1Fixture.kt").read_text()
    body = fixture.split("val statements = listOf(", 1)[1].split("\n    )", 1)[0]
    statements = [json.loads(line.strip().rstrip(",")) for line in body.splitlines() if line.strip()]
    expected = []
    for entity in schema["entities"]:
        expected.append(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        expected += [index["createSql"].replace("${TABLE_NAME}", entity["tableName"]) for index in entity.get("indices", [])]
    expected += schema["setupQueries"]
    if statements[:len(expected)] != expected:
        raise SystemExit("Historical Kotlin fixture no longer matches the archived v1 export.")
    migration = (BASE / "commonMain" / PKG / "Migration1To2.kt").read_text()
    sql = re.findall(r'connection.execSQL\("([^"\n]+)"\)', migration)
    if len(sql) != 3:
        raise SystemExit("Expected three additive unit-column statements; inspect changed migration.")
    def snapshot(connection):
        return {entity["tableName"]: connection.execute(
            "SELECT " + ",".join("quote(`" + f["columnName"] + "`)" for f in entity["fields"])
            + " FROM `" + entity["tableName"] + "`").fetchall() for entity in schema["entities"]}
    with sqlite3.connect(":memory:") as connection:
        connection.execute("PRAGMA foreign_keys=ON")
        for statement in statements:
            connection.execute(statement)
        before = snapshot(connection)
        for statement in sql:
            connection.execute(statement)
        if before != snapshot(connection):
            raise SystemExit("An old column changed during migration.")
        if connection.execute("SELECT size_unit FROM vineyard").fetchone() != (None,):
            raise SystemExit("Legacy vineyard unit was invented.")
        if connection.execute("SELECT area_unit,row_spacing_unit FROM block").fetchone() != (None, None):
            raise SystemExit("Legacy block unit was invented.")
        if connection.execute("PRAGMA foreign_key_check").fetchall():
            raise SystemExit("Foreign-key check failed.")
        if connection.execute("PRAGMA integrity_check").fetchone() != ("ok",):
            raise SystemExit("SQLite integrity check failed.")
        print(json.dumps({"status": "passed", "scope": "actual migration SQL on populated archived v1 fixture",
                          "tables_checked": len(before), "seeded_rows": sum(map(len, before.values())),
                          "old_columns_preserved": True, "legacy_units_null": True,
                          "not_run": ["Kotlin compilation", "Room target validation", "device/native tests"]}, indent=2))

if __name__ == "__main__":
    main()
