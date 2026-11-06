package com.rncoding.testvineshield.core.presentation.formatting

/** Expand the canonical Double representation without rounding or JVM-only BigDecimal.
 * This keeps a small saved fraction editable by a form that deliberately rejects exponent input.
 */
internal fun formatPlainDecimal(value: Double): String {
    require(value.isFinite())
    val raw = value.toString()
    val exponentIndex = raw.indexOfFirst { it == 'E' || it == 'e' }
    if (exponentIndex < 0) return raw
    val negative = raw.startsWith('-')
    val mantissa = raw.substring(0, exponentIndex).removePrefix("-")
    val exponent = raw.substring(exponentIndex + 1).toInt()
    val decimalPosition = (mantissa.indexOf('.').takeIf { it >= 0 } ?: mantissa.length) + exponent
    val digits = mantissa.replace(".", "")
    val expanded = when {
        decimalPosition <= 0 -> "0." + "0".repeat(-decimalPosition) + digits
        decimalPosition >= digits.length -> digits + "0".repeat(decimalPosition - digits.length)
        else -> digits.take(decimalPosition) + "." + digits.drop(decimalPosition)
    }
    val plain = if ('.' in expanded) expanded.trimEnd('0').trimEnd('.') else expanded
    return (if (negative) "-" else "") + plain
}
