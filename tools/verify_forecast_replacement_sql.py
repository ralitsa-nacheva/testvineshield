#!/usr/bin/env python3
"""SQLite replacement transaction check, not execution of Room-generated code."""
from pathlib import Path
import json, re, sqlite3
ROOT=Path(__file__).resolve().parents[1]
def run():
    schema=json.loads((ROOT/'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json').read_text())['database']
    fixture=(ROOT/'shared/src/androidDeviceTest/kotlin/com/rncoding/testvineshield/core/data/local/database/migrations/SchemaV1Fixture.kt').read_text()
    body=fixture.split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    statements=[json.loads(s.strip().rstrip(',')) for s in body.splitlines() if s.strip()]
    source=(ROOT/'shared/src/commonMain/kotlin/com/rncoding/testvineshield/core/data/local/database/daos/WeatherForecastDao.kt').read_text()
    delete=re.search(r'@Query\(\s*"""\s*(DELETE FROM weather_forecast[\s\S]+?)"""\s*\)\s*suspend fun deleteForecastByVineyard',source).group(1)
    assert '@Transaction\n    suspend fun replaceForecast' in source
    fields=[f['columnName'] for e in schema['entities'] if e['tableName']=='weather_forecast' for f in e['fields'] if f['columnName']!='forecast_id']
    insert='INSERT INTO weather_forecast('+','.join(fields)+') VALUES('+','.join('?' for _ in fields)+')'
    def row(timestamp,temp):return tuple({'vineyard_id':10,'timestamp':timestamp,'temperature_2m':temp,'forecast_created_at':1000,'is_day':1}.get(f,0) for f in fields)
    with sqlite3.connect(':memory:') as db:
        db.execute('PRAGMA foreign_keys=ON')
        for s in statements:db.execute(s)
        db.execute(insert,row(2000,18));db.commit()
        before=db.execute('SELECT * FROM weather_forecast').fetchall()
        db.execute("CREATE TRIGGER test_fail BEFORE INSERT ON weather_forecast WHEN NEW.temperature_2m = 999 BEGIN SELECT RAISE(ABORT,'Injected failure'); END")
        db.commit()
        try:
            db.execute('BEGIN');db.execute(delete,{'vineyardId':10});db.execute(insert,row(3000,19));db.execute(insert,row(4000,999));db.commit()
            raise AssertionError('Expected failure')
        except sqlite3.IntegrityError:db.rollback()
        assert before==db.execute('SELECT * FROM weather_forecast').fetchall()
        db.execute('BEGIN');db.execute(delete,{'vineyardId':10});db.execute(insert,row(3000,19));db.commit()
        assert db.execute('SELECT timestamp,temperature_2m FROM weather_forecast').fetchall()==[(3000,19.0)]
        assert not db.execute('PRAGMA foreign_key_check').fetchall()
        assert db.execute('PRAGMA integrity_check').fetchone()==('ok',)
    return {'status':'passed','scope':'SQLite transaction using production DELETE and schema-derived INSERT, not Room execution','failed_replacement_preserves_old_cache':True,'successful_replacement_replaces_old_cache':True,'sqlite_version':sqlite3.sqlite_version,'not_run':['Kotlin DTO decoding/validation','Room-generated transaction/Flow','HTTP/Ktor/native tests']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
