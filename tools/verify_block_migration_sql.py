#!/usr/bin/env python3
"""Execute production block migration SQL on populated historical fixtures; no user DB access."""
from pathlib import Path
import json,re,sqlite3,hashlib
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'shared/src'
PKG=Path('kotlin/com/rncoding/testvineshield/core/data/local/database/migrations')
def run():
    raw=(ROOT/'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json').read_bytes()
    assert hashlib.sha256(raw).hexdigest()=='015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2'
    entities=json.loads(raw)['database']['entities']
    fixture=(BASE/'androidDeviceTest'/PKG/'SchemaV1Fixture.kt').read_text().split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    seed=[json.loads(s.strip().rstrip(',')) for s in fixture.splitlines() if s.strip()]
    def sql(name):return re.findall(r'connection.execSQL\("([^"\n]+)"\)',(BASE/'commonMain'/PKG/name).read_text())
    target=sql('Migration4To5.kt');assert len(target)==13
    preflight=re.findall(r'connection.prepare\("([^"\n]+)"\)',(BASE/'commonMain'/PKG/'Migration4To5.kt').read_text());assert len(preflight)==1
    cols={e['tableName']:[f['columnName'] for f in e['fields']] for e in entities}
    cols['vineyard']+=['size_unit'];cols['block']+=['area_unit','row_spacing_unit'];cols['security_settings']+=['inactivity_lock_enabled','inactivity_timeout_millis','device_credential_enabled']
    def snapshot(c):return {t:sorted(c.execute('SELECT '+','.join('quote(CAST(`'+x+'` AS REAL))' if t=='block' and x in ['area','row_spacing'] else 'quote(`'+x+'`)' for x in names)+' FROM `'+t+'`').fetchall()) for t,names in cols.items()}
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for statement in seed+sql('Migration1To2.kt')+sql('Migration2To3.kt')+sql('Migration3To4.kt'):c.execute(statement)
        c.execute("UPDATE block SET area_unit='ha',row_spacing_unit='cm' WHERE block_id=20")
        c.execute('INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled,inactivity_lock_enabled,inactivity_timeout_millis,device_credential_enabled) VALUES(1,1,0,1,120000,1)')
        c.execute("INSERT INTO block SELECT 21,vineyard_id,'Legacy zero',0,vine_variety,color,root_stock,0,0,'1970-01-01',phenological_stage,area_unit,row_spacing_unit FROM block WHERE block_id=20")
        c.execute("INSERT INTO block SELECT 22,vineyard_id,'Legacy negative',-1,vine_variety,color,root_stock,-1,-1,planted_at,phenological_stage,area_unit,row_spacing_unit FROM block WHERE block_id=20")
        c.execute('INSERT INTO block SELECT 900,vineyard_id,name,area,vine_variety,color,root_stock,row_count,row_spacing,planted_at,phenological_stage,area_unit,row_spacing_unit FROM block WHERE block_id=20')
        c.execute('DELETE FROM block WHERE block_id=900')
        c.execute('PRAGMA user_version=4');c.commit()
        before=snapshot(c);schema_before=list(c.execute('PRAGMA table_info(block)'));sequence=c.execute("SELECT seq FROM sqlite_sequence WHERE name='block'").fetchone()
        assert not c.execute(preflight[0]).fetchall()
        c.execute("UPDATE block SET row_spacing='bad value' WHERE block_id=20");assert c.execute(preflight[0]).fetchall();c.rollback()
        c.execute('BEGIN')
        for statement in target:c.execute(statement)
        c.rollback();assert before==snapshot(c) and list(c.execute('PRAGMA table_info(block)'))==schema_before
        c.execute('BEGIN')
        for statement in target:c.execute(statement)
        assert before==snapshot(c)
        assert c.execute('SELECT area,row_spacing,row_count,planted_at FROM block WHERE block_id=21').fetchone()==(0.0,0.0,0,'1970-01-01')
        assert c.execute('SELECT area,row_spacing,row_count FROM block WHERE block_id=22').fetchone()==(-1.0,-1.0,-1)
        assert c.execute("SELECT seq FROM sqlite_sequence WHERE name='block'").fetchone()==sequence
        info={row[1]:row for row in c.execute('PRAGMA table_info(block)')}
        for name,typ in [('area','REAL'),('row_spacing','REAL'),('row_count','INTEGER'),('planted_at','TEXT')]:assert info[name][2:4]==(typ,0),(name,info[name])
        assert not any(name.endswith('_value') for name in info)
        fields=[x for x in cols['block'] if x!='block_id']
        c.execute('INSERT INTO block('+','.join('`'+x+'`' for x in fields)+') SELECT '+','.join('`'+x+'`' for x in fields)+' FROM block WHERE block_id=20')
        new=c.execute('SELECT last_insert_rowid()').fetchone()[0];assert new>900
        c.execute('UPDATE block SET area=1.25,row_spacing=2.45,row_count=NULL,planted_at=NULL WHERE block_id=?',(new,))
        assert c.execute('SELECT area,row_spacing,row_count,planted_at FROM block WHERE block_id=?',(new,)).fetchone()==(1.25,2.45,None,None)
        c.execute('UPDATE block SET area=NULL,row_spacing=NULL WHERE block_id=?',(new,))
        assert c.execute('SELECT area,row_spacing,area_unit,row_spacing_unit FROM block WHERE block_id=?',(new,)).fetchone()==(None,None,'ha','cm')
        assert c.execute('PRAGMA integrity_check').fetchone()==('ok',);assert not c.execute('PRAGMA foreign_key_check').fetchall()
        # Exercise the original FK cascade after column replacement as well as preservation.
        c.execute('DELETE FROM block WHERE block_id=20');assert c.execute('SELECT count(*) FROM activity WHERE block_id=20').fetchone()==(0,)
        assert c.execute('SELECT count(*) FROM harvest WHERE block_id=20').fetchone()==(0,)
        c.rollback()
    return {'status':'passed','scope':'actual 4->5 SQL on populated v4-shaped fixture','sqlite_version':sqlite3.sqlite_version,'tables_compared':len(cols),'existing_values_units_children_and_lock_policy_preserved':True,'rollback_restores_columns_and_rows':True,'invalid_legacy_storage_detected':True,'fraction_null_and_independent_unit_storage':True,'sequence_high_water_mark_preserved':True,'cascade_still_works':True,'legacy_zero_negative_and_epoch_date_not_guessed_unknown':True,'not_run':['Kotlin/Compose compilation','Room generated target validation','common/host/device tests','Android/iOS installed upgrade']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
