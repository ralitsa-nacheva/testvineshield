#!/usr/bin/env python3
"""Execute the new production read query on real SQLite; not Room/Kotlin execution."""
from pathlib import Path
import json, re, sqlite3
ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'shared/src/commonMain/kotlin/com/rncoding/testvineshield/core/data/local/database'
def run():
    fixture=ROOT/'shared/src/androidDeviceTest/kotlin/com/rncoding/testvineshield/core/data/local/database/migrations/SchemaV1Fixture.kt'
    body=fixture.read_text().split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    statements=[json.loads(line.strip().rstrip(',')) for line in body.splitlines() if line.strip()]
    query=re.search(r'@Query\("([^"\n]+)"\)\s+fun observeConfigurationsForVineyard', (PKG/'daos/DiseaseRiskMonitoringConfigurationDao.kt').read_text()).group(1)
    with sqlite3.connect(':memory:') as db:
        db.execute('PRAGMA foreign_keys=ON')
        for statement in statements:db.execute(statement)
        for i in range(1,9):
            for statement in re.findall(r'connection.execSQL\("([^"\n]+)"\)',(PKG/f'migrations/Migration{i}To{i+1}.kt').read_text()):db.execute(statement)
        fields=[row[1] for row in db.execute('PRAGMA table_info(vineyard)')]
        row=dict(zip(fields,db.execute('SELECT * FROM vineyard WHERE vineyard_id=10').fetchone()));row.update(vineyard_id=11,name='Other vineyard')
        db.execute('INSERT INTO vineyard('+','.join(fields)+') VALUES('+','.join('?' for _ in fields)+')',tuple(row.values()))
        fields=[row[1] for row in db.execute('PRAGMA table_info(block)')]
        row=dict(zip(fields,db.execute('SELECT * FROM block WHERE block_id=20').fetchone()));row.update(block_id=21,vineyard_id=11,name='Other block')
        db.execute('INSERT INTO block('+','.join(fields)+') VALUES('+','.join('?' for _ in fields)+')',tuple(row.values()))
        db.execute("INSERT INTO disease(disease_id,code,name,description,vectors,factors,treatment_suggestion) VALUES(70,'test','Test','','','','')")
        records=[(10,20,70,'POWDERY_MILDEW_UC_DAVIS','2026-11-01',0),(10,20,70,'BOTRYTIS','2026-10-01',1),(11,21,70,'BOTRYTIS','2026-10-01',1)]
        db.executemany('INSERT INTO disease_risk_monitoring_configuration VALUES(?,?,?,?,?,?)',records)
        changes=db.total_changes
        assert db.execute(query,{'vineyardId':10}).fetchall()==[records[1],records[0]]
        assert db.execute(query,{'vineyardId':11}).fetchall()==[records[2]]
        assert db.execute(query,{'vineyardId':999}).fetchall()==[]
        assert db.total_changes==changes
        assert not db.execute('PRAGMA foreign_key_check').fetchall()
        assert db.execute('PRAGMA integrity_check').fetchone()==('ok',)
    return {'status':'passed','scope':'new production monitoring SELECT on real SQLite only','includes_disabled':True,'stable_order':True,'vineyard_scope':True,'read_only':True,'sqlite_version':sqlite3.sqlite_version,'not_run':['Room Flow invalidation','Kotlin repository mapping','authorization/ViewModel/native tests']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
