package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import com.rncoding.testvineshield.core.data.local.mappers.BlockMapper
import com.rncoding.testvineshield.core.data.local.database.converters.LocalDateRoomConverters
import com.rncoding.testvineshield.core.presentation.formatting.formatBlockArea
import kotlinx.datetime.LocalDate
import kotlin.test.*

class BlockOptionalValuesTest {
    private fun block() = BlockDomainModel(0, 10, "North", null, "Merlot", "red", "", null, null, null, null)
    private val validator = BlockValidator()
    @Test fun unknownMeasurementsAndDateAreAccepted() { assertNull(validator.validate(block())) }
    @Test fun fractionsAreNotRoundedOrConvertedWhenUnitsChange() {
        val original = block().copy(area = 1.25, rowSpacing = 2.45, areaUnit = AreaUnit.HECTARE, rowSpacingUnit = LengthUnit.METRE)
        assertNull(validator.validate(original))
        val mapper = BlockMapper()
        assertEquals(original, mapper.toDomain(mapper.toEntity(original)))
        assertEquals(1.25, original.copy(areaUnit = null).area)
    }
    @Test fun unknownsSurviveEntityMapping() {
        val original = block().copy(areaUnit = AreaUnit.HECTARE, rowSpacingUnit = LengthUnit.METRE)
        val mapper = BlockMapper(); assertEquals(original, mapper.toDomain(mapper.toEntity(original)))
    }
    @Test fun knownMeasurementsMustBePositiveAndFinite() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertNotNull(validator.validate(block().copy(area = it)))
            assertNotNull(validator.validate(block().copy(rowSpacing = it)))
        }
    }
    @Test fun rowCountUnknownIsDifferentFromZero() {
        assertNull(validator.validate(block().copy(rowCount = null)))
        assertNull(validator.validate(block().copy(rowCount = 1)))
        assertNotNull(validator.validate(block().copy(rowCount = 0)))
        assertNotNull(validator.validate(block().copy(rowCount = -1)))
    }
    @Test fun dateAndStageAreIndependent() {
        val date = LocalDate(1990, 4, 15)
        val original = block().copy(plantedAt = date)
        assertNull(original.phenologicalStage)
        assertEquals(date, BlockMapper().toDomain(BlockMapper().toEntity(original)).plantedAt)
    }
    @Test fun nullableDateConverterDoesNotInventTodayOrAnEpoch() {
        assertNull(LocalDateRoomConverters.fromLocalDate(null))
        assertNull(LocalDateRoomConverters.toLocalDate(null))
        val date = LocalDate(2020, 4, 15)
        assertEquals(date, LocalDateRoomConverters.toLocalDate(LocalDateRoomConverters.fromLocalDate(date)))
        assertFails { LocalDateRoomConverters.toLocalDate("invalid") }
    }
    @Test fun areaDisplayDistinguishesUnknownNumberFromUnknownUnit() {
        assertEquals("Unknown", formatBlockArea(null, AreaUnit.HECTARE))
        assertEquals("1.25 ha", formatBlockArea(1.25, AreaUnit.HECTARE))
        assertEquals("1.25 (unit unknown)", formatBlockArea(1.25, null))
        assertEquals("0.0 (unit unknown)", formatBlockArea(0.0, null))
    }
}
