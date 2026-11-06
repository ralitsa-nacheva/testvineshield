#!/usr/bin/env python3
"""Run actual migration SQL on the populated v2 fixture; no user database is opened.
This checks SQLite behaviour, not Kotlin execution or Room's generated target validation.
"""
from pathlib import Path
import hashlib,json,re,sqlite3
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'shared/src'
PKG=Path('kotlin/com/rncoding/testvineshield/core/data/local/database/migrations')

def run():
    schema_path=ROOT/'shared/schemas/com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase/1.json'
    raw=schema_path.read_bytes()
    if hashlib.sha256(raw).hexdigest()!='015606d5ab4e5da51ad27d97a337c8f5a5a43ef15e4382f5044e2c8f21cafff2':
        raise SystemExit('Frozen v1 export changed; investigate drift.')
    schema=json.loads(raw)['database']
    v1=(BASE/'androidDeviceTest'/PKG/'SchemaV1Fixture.kt').read_text()
    body=v1.split('val statements = listOf(',1)[1].split('\n    )',1)[0]
    fixture=[json.loads(line.strip().rstrip(',')) for line in body.splitlines() if line.strip()]
    v2=(BASE/'androidDeviceTest'/PKG/'SchemaV2Fixture.kt').read_text()
    identity=re.search(r'ROOM_IDENTITY = "([a-f0-9]{32})"',v2).group(1)
    extras=re.findall(r'connection.execSQL\("([^"\n]+)"\)',v2)
    def sql(name):
        source=(BASE/'commonMain'/PKG/name).read_text()
        return re.findall(r'connection.execSQL\("([^"\n]+)"\)',source)
    first=sql('Migration1To2.kt');second=sql('Migration2To3.kt')
    preflight_source=(BASE/'commonMain'/PKG/'Migration2To3.kt').read_text()
    preflight=re.findall(r'connection.prepare\("([^"\n]+)"\)',preflight_source)
    if len(preflight)!=1:raise SystemExit('Review migration preflight query.')
    if len(first)!=3 or len(second)!=4:raise SystemExit('Migration statements changed; review the checker.')
    columns={entity['tableName']:[f['columnName'] for f in entity['fields']] for entity in schema['entities']}
    columns['vineyard']+=['size_unit'];columns['block']+=['area_unit','row_spacing_unit']
    def snapshot(c):
        return {table:sorted(c.execute('SELECT '+','.join('quote(CAST(elevation AS REAL))' if table=='vineyard' and col=='elevation' else 'quote(`'+col+'`)' for col in cols)+' FROM `'+table+'`').fetchall()) for table,cols in columns.items()}
    with sqlite3.connect(':memory:') as c:
        c.execute('PRAGMA foreign_keys=ON')
        for statement in fixture+first:c.execute(statement)
        for statement in extras:c.execute(statement.replace('$ROOM_IDENTITY',identity))
        c.commit();before=snapshot(c)
        if c.execute(preflight[0]).fetchone() is not None:raise SystemExit('Valid legacy fixture rejected.')
        c.execute("UPDATE vineyard SET elevation='invalid text' WHERE vineyard_id=10")
        if c.execute(preflight[0]).fetchone() is None:raise SystemExit('Invalid legacy storage was not detected.')
        c.rollback()
        sequence=c.execute("SELECT seq FROM sqlite_sequence WHERE name='vineyard'").fetchone()[0]
        c.execute('BEGIN')
        for statement in second:c.execute(statement)
        c.rollback()
        if before!=snapshot(c):raise SystemExit('Rollback changed old data.')
        info={row[1]:row for row in c.execute('PRAGMA table_info(vineyard)')}
        if info['elevation'][2:4]!=('INTEGER',1):raise SystemExit('Rollback did not restore original elevation schema.')
        c.execute('BEGIN')
        for statement in second:c.execute(statement)
        if before!=snapshot(c):raise SystemExit('Migration changed an old value or relationship.')
        info={row[1]:row for row in c.execute('PRAGMA table_info(vineyard)')}
        if info['elevation'][2:4]!=('REAL',0) or 'elevation_decimal_m' in info:raise SystemExit('Unexpected target column schema.')
        if c.execute("SELECT seq FROM sqlite_sequence WHERE name='vineyard'").fetchone()[0]!=sequence:raise SystemExit('Sequence was reset.')
        oldcols=[col for col in columns['vineyard'] if col!='vineyard_id']
        c.execute('INSERT INTO vineyard('+','.join('`'+col+'`' for col in oldcols)+') SELECT '+','.join('`'+col+'`' for col in oldcols)+' FROM vineyard WHERE vineyard_id=10')
        new_id=c.execute('SELECT last_insert_rowid()').fetchone()[0]
        if new_id<=900:raise SystemExit('Deleted ID high-water mark was reused.')
        c.execute('UPDATE vineyard SET elevation=? WHERE vineyard_id=?',(-12.75,new_id))
        if c.execute('SELECT elevation FROM vineyard WHERE vineyard_id=?',(new_id,)).fetchone()!=(-12.75,):raise SystemExit('Fraction did not persist.')
        c.execute('UPDATE vineyard SET elevation=NULL WHERE vineyard_id=?',(new_id,))
        if c.execute('SELECT elevation FROM vineyard WHERE vineyard_id=?',(new_id,)).fetchone()!=(None,):raise SystemExit('Unknown did not remain NULL.')
        if c.execute('PRAGMA foreign_key_check').fetchall():raise SystemExit('Foreign-key violation.')
        if c.execute('PRAGMA integrity_check').fetchone()!=('ok',):raise SystemExit('SQLite integrity failure.')
        c.commit()
    return {'status':'passed','sqlite_version':sqlite3.sqlite_version,'scope':'actual SQL on populated v2 fixture',
            'v2_room_identity_from_supplied_apk':identity,'tables_compared':len(before),
            'seeded_rows':sum(map(len,before.values())),'preserves_existing_values_and_units':True,
            'rollback_restores_integer_schema':True,'sequence_high_water_mark_preserved':True,
            'fraction_and_null_storage':True,'invalid_legacy_storage_detected':True,'not_run':['Kotlin/Compose compilation','Room target validation','Android/iOS runtime tests']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
