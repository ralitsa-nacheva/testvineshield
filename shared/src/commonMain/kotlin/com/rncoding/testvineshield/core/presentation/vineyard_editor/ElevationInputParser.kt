package com.rncoding.testvineshield.core.presentation.vineyard_editor

/** No grouping separators or exponent notation. A blank field explicitly means unknown. */
internal object ElevationInputParser {
    private val decimal = Regex("[+-]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)")
    fun parse(text: String): ElevationParseResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ElevationParseResult.Valid(null)
        if (!decimal.matches(trimmed)) return ElevationParseResult.Invalid
        val value = trimmed.replace(',', '.').toDoubleOrNull()
        if (value == 0.0 && trimmed.any { it in '1'..'9' }) return ElevationParseResult.Invalid
        return if (value != null && value.isFinite()) ElevationParseResult.Valid(value)
        else ElevationParseResult.Invalid
    }
}

internal sealed interface ElevationParseResult {
    data class Valid(val metres: Double?) : ElevationParseResult
    data object Invalid : ElevationParseResult
}

/** Decimal keyboards often omit minus; an explicit UI action keeps signed values enterable. */
internal fun toggleElevationSign(text: String): String {
    val trimmed = text.trim()
    return if (trimmed.startsWith("-")) trimmed.removePrefix("-")
    else "-" + trimmed.removePrefix("+")
}
