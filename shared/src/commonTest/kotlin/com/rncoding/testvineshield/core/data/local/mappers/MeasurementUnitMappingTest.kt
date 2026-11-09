package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.converters.MeasurementUnitRoomConverters
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class MeasurementUnitMappingTest {
    private val converters = MeasurementUnitRoomConverters()
    @Test fun stableCodesRoundTripWithoutOrdinals() {
        for (unit in AreaUnit.entries) assertEquals(unit, converters.codeToArea(converters.areaToCode(unit)))
        for (unit in LengthUnit.entries) assertEquals(unit, converters.codeToLength(converters.lengthToCode(unit)))
        assertEquals("ha", converters.areaToCode(AreaUnit.HECTARE))
        assertEquals("m2", converters.areaToCode(AreaUnit.SQUARE_METRE))
        assertEquals("ft", converters.lengthToCode(LengthUnit.FOOT))
    }
    @Test fun unknownUnitsStayNullAndUnsupportedCodesFail() {
        assertNull(converters.codeToArea(null)); assertNull(converters.codeToLength(null))
        assertNull(converters.areaToCode(null)); assertNull(converters.lengthToCode(null))
        assertFailsWith<IllegalArgumentException> { converters.codeToArea("future-unit") }
        assertFailsWith<IllegalArgumentException> { converters.codeToLength("future-unit") }
    }
    @Test fun vineyardRoundTripPreservesNumberAndUnitIndependently() {
        val date = LocalDate(2026, 11, 5)
        val original = VineyardEntity(1, 2, "Vineyard", 1.25, "BG", "Plovdiv", 42.1, 24.7,
            "Europe/Sofia", 200.0, 0, date, date, AreaUnit.HECTARE)
        val mapper = VineyardMapper()
        assertEquals(original, mapper.domainToEntity(mapper.entityToDomain(original)))
        assertEquals(original.copy(sizeUnit = null), mapper.domainToEntity(mapper.entityToDomain(original.copy(sizeUnit = null))))
    }
    @Test fun blockRoundTripPreservesLegacyNumbersAndExplicitUnits() {
        val original = BlockDomainModel(1, 2, "Block", 10.0, "Merlot", "red", "unknown", 5, 2.0,
            LocalDate(2020, 1, 1), null, AreaUnit.SQUARE_METRE, LengthUnit.METRE)
        val mapper = BlockMapper()
        assertEquals(original, mapper.toDomain(mapper.toEntity(original)))
        assertEquals(original.copy(areaUnit = null, rowSpacingUnit = null),
            mapper.toDomain(mapper.toEntity(original.copy(areaUnit = null, rowSpacingUnit = null))))
    }
}
