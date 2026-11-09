package com.rncoding.testvineshield.core.presentation.formatting

/** Blank means unknown; one dot/comma decimal separator; no grouping/exponent notation. */
internal object OptionalDecimalInput {
    private val decimal = Regex("[+-]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)")
    fun parse(text: String): DecimalInputResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DecimalInputResult.Valid(null)
        if (!decimal.matches(trimmed)) return DecimalInputResult.Invalid
        val value = trimmed.replace(',', '.').toDoubleOrNull()
        if (value == 0.0 && trimmed.any { it in '1'..'9' }) return DecimalInputResult.Invalid
        return if (value != null && value.isFinite()) DecimalInputResult.Valid(value) else DecimalInputResult.Invalid
    }
}
internal sealed interface DecimalInputResult {
    data class Valid(val value: Double?) : DecimalInputResult
    data object Invalid : DecimalInputResult
}
