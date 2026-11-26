import unittest
from verify_presentation_contracts import check_source

class PresentationContractTest(unittest.TestCase):
    def test_unkeyed_rows_are_rejected(self):
        for text in ('item { Text("help") }', 'items(records) { row -> Text(row.name) }'):
            with self.assertRaises(ValueError): check_source(text, "fixture")

    def test_duplicate_fixed_key_is_rejected(self):
        with self.assertRaises(ValueError):
            check_source('item(key = "same") {} item(key = "same") {}', "fixture")

    def test_nested_named_key_and_field_specific_type_are_accepted(self):
        check_source('items(records.filter { valid(it) }, key = { "record:${it.id}" }, contentType = { "card" }) {}', "fixture")

    def test_explanatory_help_must_not_be_inline(self):
        with self.assertRaises(ValueError):
            check_source('Text(stringResource(Res.string.block_save_help))', "fixture")
        check_source('InfoHelp(title = label, text = stringResource(Res.string.block_save_help))', "fixture")

if __name__ == "__main__": unittest.main()
