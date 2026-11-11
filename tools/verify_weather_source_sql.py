#!/usr/bin/env python3
"""Exact production 9->10 SQL, triggers and read guards on real SQLite, not Room/Kotlin."""
from pathlib import Path
import json,re,sqlite3
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'shared/src';DB=SRC/'commonMain/kotlin/com/rncoding/testvineshield/core/data/local/database'
def run():
    body=(SRC/'androidDeviceTest/kotlin/com/rncoding/testvineshield/core/data/local/database/migrations/SchemaV1Fixture.kt').read_text().split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    seed=[json.loads(s.strip().rstrip(',')) for s in body.splitlines() if s.strip()]
    def statements(i):return re.findall(r'connection.execSQL\("([^"\n]+)"\)',(DB/f'migrations/Migration{i}To{i+1}.kt').read_text())
    migration=statements(9);assert len(migration)==8
    dao=(DB/'daos/WeatherCacheDao.kt').read_text()
    query=lambda name: re.search(r'@Query\("([^"\n]+)"\)\s*(?:suspend )?fun '+name+r'\(',dao).group(1)
    with sqlite3.connect(':memory:') as db:
        db.execute('PRAGMA foreign_keys=ON')
        for s in seed+sum([statements(i) for i in range(1,9)],[]):db.execute(s)
        tables=[x[0] for x in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")]
        before={t:db.execute('SELECT * FROM '+t).fetchall() for t in tables};db.commit()
        db.execute('BEGIN')
        for s in migration:db.execute(s)
        db.rollback();assert not db.execute("SELECT name FROM sqlite_master WHERE name='weather_source'").fetchall()
        for s in migration:db.execute(s)
        assert before=={t:db.execute('SELECT * FROM '+t).fetchall() for t in tables}
        generation=lambda:db.execute('SELECT generation FROM weather_source WHERE vineyard_id=10').fetchone()[0]
        a=generation();assert len(a)==32
        assert db.execute(query('riskWindow'),dict(id=10,start=0,end=9223372036854775807)).fetchall()==[]
        wid=db.execute('SELECT weather_id FROM weather_history WHERE vineyard_id=10').fetchone()[0]
        db.execute('INSERT INTO weather_history_source VALUES(?,?,?,?,?,?)',(wid,a,1000,42.1,24.7,'Europe/Sofia'))
        assert len(db.execute(query('riskWindow'),dict(id=10,start=0,end=9223372036854775807)).fetchall())==1
        db.execute("UPDATE vineyard SET name='Rename only' WHERE vineyard_id=10");assert generation()==a
        db.execute('UPDATE vineyard SET latitude=43.1 WHERE vineyard_id=10');b=generation();assert b!=a
        assert db.execute(query('riskWindow'),dict(id=10,start=0,end=9223372036854775807)).fetchall()==[]
        db.execute('UPDATE vineyard SET latitude=42.1 WHERE vineyard_id=10');assert generation() not in [a,b]
        assert before['weather_history']==db.execute('SELECT * FROM weather_history').fetchall()
        db.execute("UPDATE vineyard SET time_zone='UTC' WHERE vineyard_id=10");d=generation();assert d!=a
        db.execute('BEGIN') if not db.in_transaction else None
        assert not db.execute('PRAGMA foreign_key_check').fetchall()
        assert db.execute('PRAGMA integrity_check').fetchone()==('ok',)
    return dict(status='passed',scope='production migration/trigger/current-source SELECT SQL only',old_tables_preserved=len(tables),legacy_weather_not_relabelled=True,rename_retains_generation=True,coordinate_timezone_ABA_rotate_generation=True,old_source_excluded_from_risk=True,migration_rollback=True,sqlite_version=sqlite3.sqlite_version,not_run=['Room target10 validation','Kotlin transaction/request token code','KMP compilation/tests','MapLibre native runtime and provider traffic'])
if __name__=='__main__':print(json.dumps(run(),indent=2))
