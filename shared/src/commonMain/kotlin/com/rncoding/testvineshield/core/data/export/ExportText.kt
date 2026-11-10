package com.rncoding.testvineshield.core.data.export

/** Spreadsheet-view prefixing is not a universal re-save/import safety guarantee. */
internal fun spreadsheetTextValue(value: String): String {
    val significant = value.dropWhile { it.isWhitespace() || it == '\uFEFF' || it == '\u200B' }
    val dangerous = significant.firstOrNull() in listOf('=', '+', '-', '@', '＝', '＋', '－', '＠') || value.firstOrNull() in listOf('\t', '\r', '\n')
    return if (dangerous) "'" + value else value
}
internal fun csvQuoted(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
