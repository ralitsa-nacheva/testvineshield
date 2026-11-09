#!/usr/bin/env python3
"""Actual migration SQL on isolated historical fixtures, not Kotlin/Room execution."""
from pathlib import Path
import json,re,sqlite3,hashlib
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'shared/src';PKG=Path('kotlin/com/rncoding/testvineshield/core/data/local/database/migrations')
def run():
    raw=(ROOT/'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json').read_bytes()
    assert hashlib.sha256(raw).hexdigest()=='015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2'
    entities=json.loads(raw)['database']['entities']
    body=(BASE/'androidDeviceTest'/PKG/'SchemaV1Fixture.kt').read_text().split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    seed=[json.loads(s.strip().rstrip(',')) for s in body.splitlines() if s.strip()]
    def sql(name):return re.findall(r'connection.execSQL\("([^"\n]+)"\)',(BASE/'commonMain'/PKG/name).read_text())
    migration=sql('Migration5To6.kt');assert len(migration)==4
    fixture=(BASE/'androidDeviceTest'/PKG/'ActivityStageMigrationTest.kt').read_text().split('private fun seed(',1)[1].split('private fun snapshot',1)[0]
    extras=re.findall(r'c.execSQL\("([^"\n]+)"\)',fixture)
    cols={e['tableName']:[f['columnName'] for f in e['fields']] for e in entities}
    cols['vineyard']+=['size_unit'];cols['block']+=['area_unit','row_spacing_unit'];cols['security_settings']+=['inactivity_lock_enabled','inactivity_timeout_millis','device_credential_enabled']
    def snapshot(c):return {t:sorted(c.execute('SELECT '+','.join('quote(`'+x+'`)' for x in names)+' FROM `'+t+'`').fetchall()) for t,names in cols.items()}
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for s in seed+sum([sql(name) for name in ['Migration1To2.kt','Migration2To3.kt','Migration3To4.kt','Migration4To5.kt']],[])+extras:c.execute(s)
        c.commit();before=snapshot(c);schema_before=list(c.execute('PRAGMA table_info(activity)'));sequence=c.execute("SELECT seq FROM sqlite_sequence WHERE name='activity'").fetchone()
        c.execute('BEGIN')
        for s in migration:c.execute(s)
        c.rollback();assert before==snapshot(c) and schema_before==list(c.execute('PRAGMA table_info(activity)'))
        c.execute('BEGIN')
        for s in migration:c.execute(s)
        assert before==snapshot(c)
        assert c.execute("SELECT seq FROM sqlite_sequence WHERE name='activity'").fetchone()==sequence
        info={x[1]:x for x in c.execute('PRAGMA table_info(activity)')};assert info['phenological_stage'][2:4]==('TEXT',0)
        assert 'phenological_stage_optional' not in info
        c.execute('UPDATE activity SET phenological_stage=NULL WHERE activity_id=30')
        assert c.execute('SELECT phenological_stage FROM activity WHERE activity_id=30').fetchone()==(None,)
        assert c.execute('SELECT count(*) FROM OccurrenceActivityCrossRef WHERE activity_id=30').fetchone()==(1,)
        fields=[x for x in cols['activity'] if x!='activity_id']
        c.execute('INSERT INTO activity('+','.join('`'+x+'`' for x in fields)+') SELECT '+','.join('`'+x+'`' for x in fields)+' FROM activity WHERE activity_id=30')
        assert c.execute('SELECT last_insert_rowid()').fetchone()[0]>900
        assert not c.execute('PRAGMA foreign_key_check').fetchall();assert c.execute('PRAGMA integrity_check').fetchone()==('ok',)
        c.rollback()
    return {'status':'passed','scope':'actual 5->6 SQL only on populated v5-shaped fixture','sqlite_version':sqlite3.sqlite_version,'tables_compared':20,'known_stage_rows_links_policy_and_values_preserved':True,'unknown_stage_is_null':True,'rollback_restores_original_constraint_and_rows':True,'sequence_preserved':True,'not_run':['Kotlin/Compose compilation','Room target validation','host/device/native tests','installed upgrade']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
