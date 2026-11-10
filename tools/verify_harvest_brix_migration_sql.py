#!/usr/bin/env python3
"""Execute production 6->7 SQL; not Kotlin compilation or Room validation."""
from pathlib import Path
import hashlib, json, re, sqlite3
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'shared/src'
PKG = Path('kotlin/com/rncoding/testvineshield/core/data/local/database/migrations')

def run():
    raw = (ROOT / 'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json').read_bytes()
    assert hashlib.sha256(raw).hexdigest() == '015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2'
    entities = json.loads(raw)['database']['entities']
    body = (BASE / 'androidDeviceTest' / PKG / 'SchemaV1Fixture.kt').read_text().split('val statements = listOf(', 1)[1].split('\n    )', 1)[0]
    seed = [json.loads(s.strip().rstrip(',')) for s in body.splitlines() if s.strip()]
    def sql(name):
        return re.findall(r'connection.execSQL\("([^"\n]+)"\)', (BASE / 'commonMain' / PKG / name).read_text())
    migration = sql('Migration6To7.kt'); assert len(migration) == 4
    fixture = (BASE / 'androidDeviceTest' / PKG / 'HarvestBrixMigrationTest.kt').read_text().split('private fun seed(', 1)[1].split('private fun snapshot', 1)[0]
    extras = re.findall(r'c.execSQL\("([^"\n]+)"\)', fixture)
    cols = {e['tableName']: [f['columnName'] for f in e['fields']] for e in entities}
    cols['vineyard'] += ['size_unit']; cols['block'] += ['area_unit', 'row_spacing_unit']
    cols['security_settings'] += ['inactivity_lock_enabled', 'inactivity_timeout_millis', 'device_credential_enabled']
    def snapshot(c):
        return {t: sorted(c.execute('SELECT ' + ','.join('quote(`'+x+'`)' for x in names) + ' FROM `'+t+'`').fetchall()) for t, names in cols.items()}
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for s in seed + sum([sql('Migration%dTo%d.kt' % (i, i+1)) for i in range(1, 6)], []) + extras:
            c.execute(s)
        c.commit()
        before = snapshot(c)
        schema_before = list(c.execute('PRAGMA table_info(harvest)'))
        indexes = list(c.execute("SELECT name,sql FROM sqlite_master WHERE type='index' ORDER BY name"))
        sequences = list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'))
        c.execute('BEGIN')
        for s in migration: c.execute(s)
        c.rollback()
        assert before == snapshot(c) and schema_before == list(c.execute('PRAGMA table_info(harvest)'))
        assert sequences == list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'))
        c.execute('BEGIN')
        for s in migration: c.execute(s)
        assert before == snapshot(c)
        assert indexes == list(c.execute("SELECT name,sql FROM sqlite_master WHERE type='index' ORDER BY name"))
        assert sequences == list(c.execute('SELECT * FROM sqlite_sequence ORDER BY name'))
        info = {x[1]: x for x in c.execute('PRAGMA table_info(harvest)')}
        assert info['brix_sugar_content'][2:4] == ('REAL', 0)
        assert 'brix_sugar_content_optional' not in info
        assert c.execute('SELECT brix_sugar_content,units FROM harvest WHERE harvest_id=41').fetchone() == (0.0, 'baskets')
        c.execute('UPDATE harvest SET brix_sugar_content=NULL WHERE harvest_id=41')
        assert c.execute('SELECT brix_sugar_content FROM harvest WHERE harvest_id=41').fetchone() == (None,)
        fields = [x for x in cols['harvest'] if x != 'harvest_id']
        c.execute('INSERT INTO harvest('+','.join('`'+x+'`' for x in fields)+') SELECT '+','.join('`'+x+'`' for x in fields)+' FROM harvest WHERE harvest_id=41')
        assert c.execute('SELECT last_insert_rowid()').fetchone()[0] > 900
        assert not c.execute('PRAGMA foreign_key_check').fetchall()
        assert c.execute('PRAGMA integrity_check').fetchone() == ('ok',)
        c.rollback()
    return {'status': 'passed', 'scope': 'actual production 6->7 SQL on historical v6-shaped populated fixture', 'tables_compared': len(cols), 'sqlite_version': sqlite3.sqlite_version, 'all_values_zero_brix_indexes_policy_and_sequences_preserved': True, 'unknown_brix_is_null': True, 'rollback_restores_rows_and_original_constraint': True, 'not_run': ['Kotlin compilation', 'Room target schema validation', 'device/native tests', 'installed upgrade']}

if __name__ == '__main__': print(json.dumps(run(), indent=2))
