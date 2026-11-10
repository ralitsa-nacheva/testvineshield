package com.rncoding.testvineshield.core.presentation.spray

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.domain.validation.SprayValidator
import kotlin.test.*

class SprayDraftTest {
    private val validator = SprayValidator(VineyardTimeZoneCatalog(setOf("UTC", "Europe/Sofia")))
    private fun draft() = SprayDraft(date = "2026-11-10", zone = "Europe/Sofia", lines = listOf(SprayLineDraft(name = "Product A")))
    @Test fun unknownAmountsAndTimeRemainNull() {
        val record = assertNotNull(draft().parsed(30).record)
        assertNull(record.localTime); assertNull(record.treatedArea); assertNull(record.waterQuantity)
        assertNull(record.products.single().quantity); assertNull(record.products.single().unit)
        assertNull(validator.validate(record))
    }
    @Test fun applicationDateIsRequiredAndNotInferred() {
        assertNull(draft().copy(date = "").parsed(30).record)
        assertTrue("date" in draft().copy(date = "2026-02-30").parsed(30).errors)
    }
    @Test fun knownMeasurementsRequireExplicitUnitsAndAcceptFractionalComma() {
        assertTrue("areaUnit" in draft().copy(area = "0,25").parsed(30).errors)
        val good = draft().copy(area = "0,25", areaUnit = AreaUnit.HECTARE, water = "10.5", waterUnit = SprayQuantityUnit.US_GALLON,
            lines = listOf(SprayLineDraft(name = "A", quantity = "0,5", unit = SprayQuantityUnit.KILOGRAM)))
        val record = assertNotNull(good.parsed(30).record)
        assertEquals(0.25, record.treatedArea); assertEquals(0.5, record.products.single().quantity)
        assertNull(validator.validate(record))
    }
    @Test fun invalidNumbersTimeAndCarrierDimensionAreRejected() {
        for (text in listOf("0", "-1", "NaN", "Infinity", "1e2", "1,2.3")) assertNull(draft().copy(water = text).parsed(30).record)
        assertTrue("time" in draft().copy(time = "24:30").parsed(30).errors)
        val record = assertNotNull(draft().parsed(30).record)
        assertNotNull(validator.validate(record.copy(waterQuantity = 2.0, waterUnit = SprayQuantityUnit.KILOGRAM)))
        assertNotNull(validator.validate(record.copy(treatedArea = Double.POSITIVE_INFINITY)))
    }
    @Test fun restoredDraftRetainsBaseVersionLineIdentityAndUnfinishedText() {
        val raw = draft().copy(time = "unfinished", baseRevision = 3, baseGeneration = "a".repeat(32),
            lines = listOf(SprayLineDraft(10, "A", "0,", SprayQuantityUnit.LITRE, "User reference"), SprayLineDraft(name = "B")))
        assertEquals(raw, SprayDraft.restore(raw.savedValues()))
        assertNull(SprayDraft.restore(emptyList()))
        assertNull(SprayDraft.restore(raw.savedValues().also { it[15] = "unknown-code" }))
    }
    @Test fun domainChecksBoundsAndDuplicateLineIdentities() {
        val record = assertNotNull(draft().parsed(30).record)
        assertNotNull(validator.validate(record.copy(products = emptyList())))
        assertNotNull(validator.validate(record.copy(products = List(21) { record.products.single() })))
        assertNotNull(validator.validate(record.copy(products = listOf(SprayProductLine(5, "A"), SprayProductLine(5, "B")))))
        assertNotNull(validator.validate(record.copy(notes = "x".repeat(4001))))
        assertNotNull(validator.validate(record.copy(revision = 1, generation = null)))
    }
    @Test fun localDateTimeAndZoneAreKeptWithoutInventingAnInstant() {
        val record = assertNotNull(draft().copy(date = "2026-10-25", time = "03:30").parsed(30).record)
        assertEquals("03:30", record.localTime); assertEquals("Europe/Sofia", record.timeZone)
        assertNull(validator.validate(record))
    }
    @Test fun unitChangeDoesNotConvertNumbersAndStableCodesAreDistinct() {
        val first = draft().copy(water = "10", waterUnit = SprayQuantityUnit.US_GALLON)
        val changed = assertNotNull(first.copy(waterUnit = SprayQuantityUnit.IMPERIAL_GALLON).parsed(30).record)
        assertEquals(10.0, changed.waterQuantity)
        assertNotEquals(SprayQuantityUnit.US_GALLON.code, SprayQuantityUnit.IMPERIAL_GALLON.code)
    }
}
