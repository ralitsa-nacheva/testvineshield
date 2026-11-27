"""Mutation checks for accidentally opening a backup/transfer channel."""
from pathlib import Path
import shutil
import tempfile
import unittest
from verify_android_backup_policy import audit


class AndroidBackupPolicyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        source = Path(__file__).resolve().parents[1] / 'androidApp/src/main'
        target = self.root / 'androidApp/src/main'
        shutil.copytree(source / 'res/xml', target / 'res/xml')
        shutil.copyfile(source / 'AndroidManifest.xml', target / 'AndroidManifest.xml')

    def change(self, relative, before, after):
        path = self.root / 'androidApp/src/main' / relative
        text = path.read_text(encoding='utf-8')
        self.assertIn(before, text)
        path.write_text(text.replace(before, after), encoding='utf-8')

    def test_current_policy_passes(self):
        self.assertTrue(audit(self.root)['passed'])

    def test_enabled_manifest_backup_is_rejected(self):
        self.change('AndroidManifest.xml', 'android:allowBackup="false"', 'android:allowBackup="true"')
        self.assertFalse(audit(self.root)['passed'])

    def test_filename_exclusion_does_not_cover_all_databases(self):
        self.change('res/xml/backup_rules.xml', 'domain="database" path="."', 'domain="database" path="one.db"')
        self.assertFalse(audit(self.root)['passed'])

    def test_device_transfer_cannot_be_omitted(self):
        self.change('res/xml/data_extraction_rules.xml', '<device-transfer>', '<unreviewed-transfer>')
        self.change('res/xml/data_extraction_rules.xml', '</device-transfer>', '</unreviewed-transfer>')
        self.assertFalse(audit(self.root)['passed'])

    def test_device_protected_preferences_must_be_excluded(self):
        self.change('res/xml/data_extraction_rules.xml', '<exclude domain="device_sharedpref" path="." />', '')
        self.assertFalse(audit(self.root)['passed'])

    def test_supplied_merged_manifest_is_checked(self):
        manifest = self.root / 'merged.xml'
        source = self.root / 'androidApp/src/main/AndroidManifest.xml'
        manifest.write_text(source.read_text(encoding='utf-8').replace('android:allowBackup="false"', 'android:allowBackup="true"'), encoding='utf-8')
        self.assertTrue(audit(self.root)['passed'])
        self.assertFalse(audit(self.root, manifest)['passed'])

    def test_custom_agent_requires_review(self):
        self.change('AndroidManifest.xml', 'android:allowBackup="false"', 'android:allowBackup="false" android:backupAgent=".UnexpectedAgent"')
        self.assertFalse(audit(self.root)['passed'])


if __name__ == '__main__':
    unittest.main()
