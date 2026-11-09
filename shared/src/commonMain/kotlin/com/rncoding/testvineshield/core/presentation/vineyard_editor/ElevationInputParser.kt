package com.rncoding.testvineshield.core.presentation.vineyard_editor

/** No grouping separators or exponent notation. A blank field explicitly means unknown. */
internal object ElevationInputParser {
    fun parse(text: String): ElevationParseResult = when (val result =
        com.rncoding.testvineshield.core.presentation.formatting.OptionalDecimalInput.parse(text)) {
        is com.rncoding.testvineshield.core.presentation.formatting.DecimalInputResult.Valid -> ElevationParseResult.Valid(result.value)
        com.rncoding.testvineshield.core.presentation.formatting.DecimalInputResult.Invalid -> ElevationParseResult.Invalid
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
