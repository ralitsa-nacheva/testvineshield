#!/usr/bin/env python3
"""Execute actual production 7->8 DDL against historical populated storage.
This is not Kotlin/Room execution or a compiler-generated v8 schema export.
"""
from pathlib import Path
import hashlib, json, re, sqlite3
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'shared/src';PKG=Path('kotlin/com/rncoding/testvineshield/core/data/local/database/migrations')
def run():
    raw=(ROOT/'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json').read_bytes()
    assert hashlib.sha256(raw).hexdigest()=='015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2'
    entities=json.loads(raw)['database']['entities']
    body=(BASE/'androidDeviceTest'/PKG/'SchemaV1Fixture.kt').read_text().split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    seed=[json.loads(s.strip().rstrip(',')) for s in body.splitlines() if s.strip()]
    def sql(name):return re.findall(r'connection.execSQL\("([^"\n]+)"\)',(BASE/'commonMain'/PKG/name).read_text())
    migration=sql('Migration7To8.kt');assert len(migration)==3
    fixture=(BASE/'androidDeviceTest'/PKG/'SprayMigrationTest.kt').read_text().split('private fun seed(',1)[1].split('private fun snapshot',1)[0]
    extras=re.findall(r'c.execSQL\("([^"\n]+)"\)',fixture)
    cols={e['tableName']:[f['columnName'] for f in e['fields']] for e in entities}
    cols['vineyard']+=['size_unit'];cols['block']+=['area_unit','row_spacing_unit']
    cols['security_settings']+=['inactivity_lock_enabled','inactivity_timeout_millis','device_credential_enabled']
    def snapshot(c):return {t:sorted(c.execute('SELECT '+','.join('quote(`'+x+'`)' for x in names)+' FROM `'+t+'`').fetchall()) for t,names in cols.items()}
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for statement in seed+sum([sql('Migration%dTo%d.kt'%(i,i+1)) for i in range(1,7)],[])+extras:c.execute(statement)
        c.commit();before=snapshot(c);sequences=list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'))
        old_schema=list(c.execute("SELECT type,name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY name"))
        c.execute('BEGIN')
        for statement in migration:c.execute(statement)
        c.rollback()
        assert before==snapshot(c) and old_schema==list(c.execute("SELECT type,name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY name"))
        c.execute('BEGIN')
        for statement in migration:c.execute(statement)
        assert before==snapshot(c) and sequences==list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'))
        assert c.execute('SELECT count(*) FROM spray_application').fetchone()==(0,)
        assert c.execute('SELECT count(*) FROM spray_product').fetchone()==(0,)
        app_cols={row[1] for row in c.execute('PRAGMA table_info(spray_application)')}
        assert app_cols=={'activity_id','application_date','local_time','time_zone','treated_area','area_unit','water_quantity','water_unit','notes','revision','generation'}
        c.execute("INSERT INTO spray_application(activity_id,application_date,time_zone,revision,generation) VALUES(30,'2026-11-10','UTC',1,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')")
        c.execute("INSERT INTO spray_product(activity_id,line_position,product_name,quantity,quantity_unit) VALUES(30,0,'A',0.5,'kg')")
        c.execute("INSERT INTO spray_product(activity_id,line_position,product_name) VALUES(30,1,'B')")
        assert c.execute('SELECT quantity,quantity_unit FROM spray_product WHERE line_position=1').fetchone()==(None,None)
        try:c.execute("INSERT INTO spray_product(activity_id,line_position,product_name) VALUES(30,1,'Duplicate')")
        except sqlite3.IntegrityError:pass
        else:raise AssertionError('Position uniqueness was not enforced')
        c.execute('DELETE FROM spray_application WHERE activity_id=30')
        assert c.execute('SELECT count(*) FROM spray_product').fetchone()==(0,)
        assert before==snapshot(c)
        assert not c.execute('PRAGMA foreign_key_check').fetchall()
        assert c.execute('PRAGMA integrity_check').fetchone()==('ok',)
        c.rollback()
    return {'status':'passed','scope':'actual production 7->8 DDL only','old_tables_compared':len(cols),'new_tables':2,'sqlite_version':sqlite3.sqlite_version,'old_rows_legacy_treatments_links_security_sequences_preserved':True,'new_tables_initially_empty':True,'nullable_amounts_unique_positions_and_child_cascade_checked':True,'rollback_removes_new_schema':True,'not_run':['Kotlin compilation','Room target8 validation','DAO replace/revision/rollback execution','host/device/native tests','installed upgrade']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
