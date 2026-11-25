import tempfile
import unittest
from pathlib import Path
from verify_bilingual_resources import check_pair, read_strings

class BilingualContractTest(unittest.TestCase):
    def test_reordered_numbered_arguments_are_allowed(self):
        check_pair({"a": "%1$s %2$d"}, {"a": "%2$d %1$s"})

    def test_missing_translation_is_rejected(self):
        with self.assertRaises(ValueError):
            check_pair({"a": "Text"}, {})

    def test_changed_argument_type_and_omission_are_rejected(self):
        for text in ("%1$s", "Text"):
            with self.assertRaises(ValueError):
                check_pair({"a": "%1$d"}, {"a": text})

    def test_unnumbered_or_unsupported_formats_are_rejected(self):
        for text in ("%s", "%1$f"):
            with self.assertRaises(ValueError):
                check_pair({"a": text}, {"a": text})

    def test_duplicate_names_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "strings.xml"
            path.write_text('<resources><string name="a">A</string><string name="a">B</string></resources>')
            with self.assertRaises(ValueError):
                read_strings(path)

if __name__ == "__main__":
    unittest.main()
