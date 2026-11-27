"""Check source or a supplied TEXT merged manifest; does not simulate Android backup.

Run from the project root. --manifest accepts an Android Studio/AGP merged XML
manifest to check merger output too. Binary APK XML must first be decoded with
Android tooling; it is deliberately not accepted as source XML.
"""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ANDROID = '{http://schemas.android.com/apk/res/android}'
DOMAINS = frozenset({
    'root', 'file', 'database', 'sharedpref', 'external',
    'device_root', 'device_file', 'device_database', 'device_sharedpref',
})


def excluded_domains(section):
    """Only an entire-domain exclusion satisfies this policy; a filename cannot."""
    return {e.get('domain') for e in section.findall('exclude') if e.get('path') in ('.', './')}


def audit(root, manifest=None):
    root = Path(root)
    manifest = Path(manifest) if manifest else root / 'androidApp/src/main/AndroidManifest.xml'
    application = ET.parse(manifest).getroot().find('application')
    errors = []
    if application is None:
        return {'passed': False, 'errors': ['Manifest has no application element.']}
    expected = {
        'allowBackup': 'false',
        'fullBackupContent': '@xml/backup_rules',
        'dataExtractionRules': '@xml/data_extraction_rules',
    }
    for name, value in expected.items():
        if application.get(ANDROID + name) != value:
            errors.append(f'Application {name} must be {value}.')
    if application.get(ANDROID + 'backupAgent'):
        errors.append('A custom backupAgent requires a separate policy review.')
    resource_root = root / 'androidApp/src/main/res/xml'
    legacy = ET.parse(resource_root / 'backup_rules.xml').getroot()
    modern = ET.parse(resource_root / 'data_extraction_rules.xml').getroot()
    if legacy.tag != 'full-backup-content':
        errors.append('Legacy rules have the wrong root.')
    if modern.tag != 'data-extraction-rules':
        errors.append('Modern rules have the wrong root.')
    sections = [('legacy', legacy)]
    for tag in ('cloud-backup', 'device-transfer'):
        matches = modern.findall(tag)
        if len(matches) != 1:
            errors.append(f'Expected exactly one {tag} section.')
        else:
            sections.append((tag, matches[0]))
    # Do not silently enable a new transfer channel without reviewing its policy.
    if any(child.tag not in {'cloud-backup', 'device-transfer'} for child in modern):
        errors.append('Unreviewed modern transfer section.')
    for name, section in sections:
        missing = sorted(DOMAINS - excluded_domains(section))
        if missing:
            errors.append(f'{name}: missing full-domain exclusions: {", ".join(missing)}.')
        if section.findall('include'):
            errors.append(f'{name}: includes require a policy review.')
    return {
        'passed': not errors, 'errors': errors,
        'manifest': str(manifest), 'domains_per_section': len(DOMAINS),
        'limits': 'XML policy check only. Build, merged resources and real transport behavior require Android verification.',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--manifest', type=Path)
    args = parser.parse_args()
    try:
        result = audit(args.root, args.manifest)
    except (OSError, ET.ParseError) as error:
        result = {'passed': False, 'errors': [str(error)]}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
