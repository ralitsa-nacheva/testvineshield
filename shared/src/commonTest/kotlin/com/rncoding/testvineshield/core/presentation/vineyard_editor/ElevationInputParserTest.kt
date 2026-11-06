package com.rncoding.testvineshield.core.presentation.vineyard_editor

import kotlin.test.*
import com.rncoding.testvineshield.core.presentation.formatting.formatPlainDecimal

class ElevationInputParserTest {
    @Test fun acceptsDecimalDotAndComma() {
        assertEquals(ElevationParseResult.Valid(123.45), ElevationInputParser.parse("123.45"))
        assertEquals(ElevationParseResult.Valid(123.45), ElevationInputParser.parse("123,45"))
    }
    @Test fun preservesNegativeMetresAndTrimsWhitespace() {
        assertEquals(ElevationParseResult.Valid(-12.5), ElevationInputParser.parse(" -12,5 "))
    }
    @Test fun blankIsUnknownWhileZeroIsMeasuredZero() {
        assertEquals(ElevationParseResult.Valid(null), ElevationInputParser.parse("  "))
        assertEquals(ElevationParseResult.Valid(0.0), ElevationInputParser.parse("0"))
    }
    @Test fun rejectsGroupingMixedSeparatorsAndExponentNotation() {
        for (text in listOf("1 234.5", "1,234.5", "1.234,5", "1e3", "--2", "+", "height")) {
            assertEquals(ElevationParseResult.Invalid, ElevationInputParser.parse(text))
        }
    }
    @Test fun rejectsNonFiniteNumbersAndOverflow() {
        for (text in listOf("NaN", "Infinity", "-Infinity", "9".repeat(400), "0." + "0".repeat(400) + "1")) {
            assertEquals(ElevationParseResult.Invalid, ElevationInputParser.parse(text))
        }
    }
    @Test fun permitsLeadingDecimalFraction() {
        assertEquals(ElevationParseResult.Valid(0.5), ElevationInputParser.parse(".5"))
        assertEquals(ElevationParseResult.Valid(-0.5), ElevationInputParser.parse("-,5"))
    }
    @Test fun signActionIsReversibleAndDoesNotRound() {
        assertEquals("-123,45", toggleElevationSign("123,45"))
        assertEquals("123,45", toggleElevationSign("-123,45"))
        assertEquals("-12.75", toggleElevationSign("+12.75"))
    }

    @Test fun savedSmallFractionsRemainEditableWithoutExponentNotation() {
        for (value in listOf(0.0000001, -0.0000001, 1.0E-100, 123.456, 0.0)) {
            val text = formatPlainDecimal(value)
            assertFalse(text.contains('E', ignoreCase = true))
            assertEquals(ElevationParseResult.Valid(value), ElevationInputParser.parse(text))
        }
    }

}
