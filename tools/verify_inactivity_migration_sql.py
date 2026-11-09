#!/usr/bin/env python3
"""Execute actual 3->4 SQL on isolated populated fixtures. Does not execute Room or Kotlin."""
from pathlib import Path
import json,re,sqlite3
ROOT=Path(__file__).resolve().parents[1]
PKG=Path('kotlin/com/rncoding/testvineshield/core/data/local/database/migrations')
BASE=ROOT/'shared/src'
def statements(name):
    return re.findall(r'connection.execSQL\("([^"\n]+)"\)',(BASE/'commonMain'/PKG/name).read_text())
def run():
    fixture=(BASE/'androidDeviceTest'/PKG/'SchemaV1Fixture.kt').read_text().split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    v1=[json.loads(s.strip().rstrip(',')) for s in fixture.splitlines() if s.strip()]
    schema=json.loads((ROOT/'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json').read_text())['database']
    cols={e['tableName']:[f['columnName'] for f in e['fields']] for e in schema['entities']}
    cols['vineyard']+=['size_unit'];cols['block']+=['area_unit','row_spacing_unit']
    def snapshot(c):
        return {t:sorted(c.execute('SELECT '+','.join('quote(`'+x+'`)' for x in names)+' FROM `'+t+'`').fetchall()) for t,names in cols.items()}
    target=statements('Migration3To4.kt');assert len(target)==4
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for s in v1+statements('Migration1To2.kt')+statements('Migration2To3.kt'):c.execute(s)
        c.execute('INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled) VALUES(1,1,1)')
        c.execute("INSERT INTO user(user_id,user_email,pass_hash,pass_salt) VALUES(2,'other@example.com','preserve-hash','preserve-salt')")
        c.execute('INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled) VALUES(2,0,0)')
        c.execute('PRAGMA user_version=3');c.commit()
        before=snapshot(c);schema_before=list(c.execute('PRAGMA table_info(security_settings)'))
        c.execute('BEGIN')
        for s in target:c.execute(s)
        c.rollback()
        assert snapshot(c)==before and list(c.execute('PRAGMA table_info(security_settings)'))==schema_before
        assert c.execute('PRAGMA user_version').fetchone()==(3,)
        c.execute('BEGIN')
        for s in target:c.execute(s)
        after=snapshot(c)
        for table in cols:
            if table!='security_settings':assert before[table]==after[table],table
        assert after['security_settings']==[('1','0','0'),('2','0','0')]
        rows=c.execute('SELECT user_id,inactivity_lock_enabled,inactivity_timeout_millis,device_credential_enabled FROM security_settings ORDER BY user_id').fetchall()
        assert rows==[(1,0,900000,0),(2,0,900000,0)]
        info={x[1]:x for x in c.execute('PRAGMA table_info(security_settings)')}
        for name,default in [('inactivity_lock_enabled','0'),('inactivity_timeout_millis','900000'),('device_credential_enabled','0')]:
            assert info[name][2:5]==('INTEGER',1,default)
        c.execute('UPDATE security_settings SET inactivity_lock_enabled=1,inactivity_timeout_millis=120000,device_credential_enabled=1 WHERE user_id=1')
        assert c.execute('SELECT inactivity_lock_enabled FROM security_settings WHERE user_id=2').fetchone()==(0,)
        assert not c.execute('PRAGMA foreign_key_check').fetchall()
        assert c.execute('PRAGMA integrity_check').fetchone()==('ok',)
        c.execute('PRAGMA user_version=4');c.commit()
    return dict(status='passed',scope='actual migration SQL only; isolated populated fixtures',sqlite_version=sqlite3.sqlite_version,
        tables_compared=len(cols),accounts_checked=2,other_records_preserved=True,old_flags_deliberately_reset=True,
        default_lock_off=True,timeout_default_millis=900000,per_account_updates=True,rollback_restores_original_columns_and_flags=True,
        not_run=['Kotlin/Compose compilation','Room compiler target validation','host/device tests','native authentication prompts','Android/iOS lifecycle runtime'])
if __name__=='__main__':print(json.dumps(run(),indent=2))
