#!/usr/bin/env python3
"""Production 8->9 SQL on populated historical data; not Kotlin/Room execution."""
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
    migration=sql('Migration8To9.kt');assert len(migration)==4
    fixture=(BASE/'androidDeviceTest'/PKG/'SprayRateMigrationTest.kt').read_text().split('private fun seed(',1)[1].split('private fun snapshot',1)[0]
    extras=re.findall(r'c.execSQL\("([^"\n]+)"\)',fixture)
    columns={e['tableName']:[f['columnName'] for f in e['fields']] for e in entities}
    columns['vineyard']+=['size_unit'];columns['block']+=['area_unit','row_spacing_unit'];columns['security_settings']+=['inactivity_lock_enabled','inactivity_timeout_millis','device_credential_enabled']
    columns['spray_application']=['activity_id','application_date','local_time','time_zone','treated_area','area_unit','water_quantity','water_unit','notes','revision','generation']
    columns['spray_product']=['product_line_id','activity_id','line_position','product_name','quantity','quantity_unit','label_reference']
    def snapshot(c):return {t:sorted(c.execute('SELECT '+','.join('quote(`'+x+'`)' for x in fields)+' FROM `'+t+'`').fetchall()) for t,fields in columns.items()}
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for statement in seed+sum([sql('Migration%dTo%d.kt'%(i,i+1)) for i in range(1,8)],[])+extras:c.execute(statement)
        c.commit();before=snapshot(c);info=list(c.execute('PRAGMA table_info(spray_product)'))
        sequences=list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'));indices=list(c.execute("SELECT name,sql FROM sqlite_master WHERE type='index' ORDER BY name"))
        c.execute('BEGIN')
        for statement in migration:c.execute(statement)
        c.rollback();assert before==snapshot(c) and info==list(c.execute('PRAGMA table_info(spray_product)'))
        c.execute('BEGIN')
        for statement in migration:c.execute(statement)
        assert before==snapshot(c) and sequences==list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'))
        assert indices==list(c.execute("SELECT name,sql FROM sqlite_master WHERE type='index' ORDER BY name"))
        assert c.execute('SELECT rate_quantity,rate_product_unit,rate_basis_quantity,rate_basis_unit FROM spray_product').fetchall()==[(None,None,None,None),(None,None,None,None)]
        c.execute("UPDATE spray_product SET rate_quantity=100,rate_product_unit='ml',rate_basis_quantity=100,rate_basis_unit='water_l' WHERE product_line_id=50")
        assert c.execute('SELECT quantity,quantity_unit,rate_quantity,rate_basis_quantity FROM spray_product WHERE product_line_id=50').fetchone()==(0.5,'kg',100.0,100.0)
        c.execute("INSERT INTO spray_product(activity_id,line_position,product_name) VALUES(30,2,'New')")
        assert c.execute('SELECT last_insert_rowid()').fetchone()[0]>900
        assert not c.execute('PRAGMA foreign_key_check').fetchall();assert c.execute('PRAGMA integrity_check').fetchone()==('ok',)
        c.rollback()
    return {'status':'passed','scope':'actual production 8->9 SQL only','old_tables_compared':len(columns),'new_nullable_columns':4,'sqlite_version':sqlite3.sqlite_version,'old_values_positions_ids_revisions_generations_indices_sequences_preserved':True,'old_rates_unknown_no_legacy_inference':True,'rollback_restores_original_columns_and_rows':True,'not_run':['Kotlin compilation','Room target9 validation','repository compound-rate mapping','host/device/native tests','installed upgrade']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
