#!/usr/bin/env python3
"""Execute actual inbox/update SQL against a minimal query fixture, not Room schema validation."""
from pathlib import Path
import json
import re
import sqlite3

ROOT = Path(__file__).resolve().parents[1]

def run():
    source = (ROOT / "shared/src/commonMain/kotlin/com/rncoding/testvineshield/core/data/local/database/daos/DiseaseAlertDao.kt").read_text(encoding="utf-8")
    queries = re.findall(r'@Query\("""(.*?)"""\)', source, re.S)
    select = next(q for q in queries if "AS vineyardName" in q)
    update = next(q for q in queries if "SET alert_status = 'acknowledged'" in q)
    with sqlite3.connect(":memory:") as db:
        db.row_factory = sqlite3.Row
        db.executescript("""
            CREATE TABLE vineyard(vineyard_id INTEGER PRIMARY KEY,user_id INTEGER,name TEXT);
            CREATE TABLE block(block_id INTEGER PRIMARY KEY,vineyard_id INTEGER,name TEXT);
            CREATE TABLE disease(disease_id INTEGER PRIMARY KEY,code TEXT,name TEXT);
            CREATE TABLE disease_alert(alert_id INTEGER PRIMARY KEY,disease_id INTEGER,vineyard_id INTEGER,
                block_id INTEGER,created_at TEXT,alert TEXT,alert_status TEXT,alert_severity TEXT,phenological_stage TEXT);
            INSERT INTO vineyard VALUES(10,1,'Owned'),(20,2,'Other');
            INSERT INTO block VALUES(5,20,'Private other block');
            INSERT INTO disease VALUES(3,'fixture','Fixture');
            INSERT INTO disease_alert VALUES(30,3,10,NULL,'2026-11-25','Saved','active','high','flowering');
            INSERT INTO disease_alert VALUES(31,3,20,5,'2026-11-25','Other','active','high','flowering');
        """)
        def rows(owner): return [dict(row) for row in db.execute(select,{"ownerId":owner})]
        assert [r["alertId"] for r in rows(1)] == [30]
        assert [r["alertId"] for r in rows(2)] == [31]
        def params(row, owner=1):
            return {"ownerId":owner,"vineyardId":row["vineyardId"],"alertId":row["alertId"],
                "diseaseId":row["diseaseId"],"blockId":row["blockId"],"createdAt":row["createdAt"],
                "message":row["message"],"severity":row["severity"],"stage":row["stage"]}
        expected = rows(1)[0]
        assert db.execute(update,params(expected,2)).rowcount == 0
        db.execute("UPDATE disease_alert SET alert_status='invalidated' WHERE alert_id=30")
        assert db.execute(update,params(expected)).rowcount == 0
        db.execute("UPDATE disease_alert SET alert_status='active',alert='Changed' WHERE alert_id=30")
        assert db.execute(update,params(expected)).rowcount == 0
        expected = rows(1)[0]
        assert db.execute(update,params(expected)).rowcount == 1
        assert db.execute(update,params(expected)).rowcount == 0
        assert rows(1)[0]["status"] == "acknowledged"
        assert rows(2)[0]["status"] == "active"
        # Malformed legacy cross-vineyard block references must not disclose the other block's name.
        db.execute("UPDATE disease_alert SET block_id=5 WHERE alert_id=30")
        assert rows(1)[0]["blockName"] is None and rows(1)[0]["blockId"] is None
    return {"status":"passed","scope":"actual owned-inbox and compare-before-review SQL over minimal fixture",
        "checks":["owner filtering","wrong-owner update rejected","invalidation/stale message rejected",
                  "review exactly once","other owner unchanged","cross-vineyard block name hidden"],
        "room_or_device_tests_run":False}

if __name__ == "__main__":
    print(json.dumps(run(),indent=2))
