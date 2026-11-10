package com.rncoding.testvineshield.core.presentation.spray

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.domain.validation.SprayValidator
import kotlin.test.*

class SprayRateDraftTest {
    private fun draft(line: SprayLineDraft = SprayLineDraft(name = "A")) = SprayDraft(date = "2026-11-10", zone = "UTC", lines = listOf(line))
    private val validator = SprayValidator(VineyardTimeZoneCatalog(setOf("UTC")))
    @Test fun absentRateStaysNullWithoutAnInferredBasis() {
        assertNull(assertNotNull(draft().parsed(30).record).products.single().rate)
    }
    @Test fun everyRateComponentIsRequiredOnceAnyPartIsEntered() {
        val partials = listOf(SprayLineDraft(name = "A", rateQuantity = "2"), SprayLineDraft(name = "A", rateUnit = SprayQuantityUnit.KILOGRAM),
            SprayLineDraft(name = "A", rateBasisQuantity = "1"), SprayLineDraft(name = "A", rateBasis = SprayRateBasis.HECTARE))
        for (partial in partials) assertNull(draft(partial).parsed(30).record)
    }
    @Test fun areaAndCarrierWaterBasesAreExplicitAndNotNormalised() {
        val line = SprayLineDraft(name = "A", quantity = "0.5", unit = SprayQuantityUnit.KILOGRAM,
            rateQuantity = "100", rateUnit = SprayQuantityUnit.MILLILITRE, rateBasisQuantity = "100", rateBasis = SprayRateBasis.LITRE)
        val record = assertNotNull(draft(line).parsed(30).record)
        assertEquals(0.5, record.products.single().quantity)
        assertEquals(SprayRate(100.0, SprayQuantityUnit.MILLILITRE, 100.0, SprayRateBasis.LITRE), record.products.single().rate)
        assertNull(validator.validate(record))
        assertEquals("carrier_water", SprayRateBasis.LITRE.dimension)
        assertEquals("treated_area", SprayRateBasis.HECTARE.dimension)
    }
    @Test fun knownRateAndBasisMustBeFiniteAndPositive() {
        val line = SprayLineDraft(name = "A", rateQuantity = "2", rateUnit = SprayQuantityUnit.KILOGRAM, rateBasisQuantity = "1", rateBasis = SprayRateBasis.HECTARE)
        for (text in listOf("0", "-1", "NaN", "Infinity", "1e3")) {
            assertNull(draft(line.copy(rateQuantity = text)).parsed(30).record)
            assertNull(draft(line.copy(rateBasisQuantity = text)).parsed(30).record)
        }
        val record = assertNotNull(draft(line).parsed(30).record)
        val invalid = record.copy(products = record.products.map { it.copy(rate = assertNotNull(it.rate).copy(quantity = Double.NaN)) })
        assertNotNull(validator.validate(invalid))
    }
    @Test fun versionOneDraftRestoresWithoutInventingARate() {
        val old = listOf("1", "2026-11-10", "", "UTC", "", "", "", "", "Old notes", "1", "2", "a".repeat(32), "5", "A", "0,5", "kg", "Reference")
        val restored = assertNotNull(SprayDraft.restore(old))
        assertEquals("0,5", restored.lines.single().quantity)
        assertEquals(2L, restored.baseRevision); assertEquals("a".repeat(32), restored.baseGeneration)
        assertNull(assertNotNull(restored.parsed(30).record).products.single().rate)
    }
    @Test fun versionTwoDraftPreservesUnfinishedRateAndBaseIdentity() {
        val raw = draft(SprayLineDraft(5, "A", rateQuantity = "unfinished", rateUnit = SprayQuantityUnit.LITRE, rateBasisQuantity = "100", rateBasis = SprayRateBasis.US_GALLON))
            .copy(baseRevision = 2, baseGeneration = "a".repeat(32))
        assertEquals("2", raw.savedValues().first())
        assertEquals(raw, SprayDraft.restore(raw.savedValues()))
    }
    @Test fun explicitClearRemovesAllFourPartsAndUnitChangeDoesNotConvert() {
        val line = SprayLineDraft(name = "A", rateQuantity = "2", rateUnit = SprayQuantityUnit.KILOGRAM, rateBasisQuantity = "1", rateBasis = SprayRateBasis.HECTARE)
        val changed = assertNotNull(draft(line.copy(rateBasis = SprayRateBasis.ACRE)).parsed(30).record).products.single().rate
        assertEquals(SprayRate(2.0, SprayQuantityUnit.KILOGRAM, 1.0, SprayRateBasis.ACRE), changed)
        val cleared = line.copy(rateQuantity = "", rateUnit = null, rateBasisQuantity = "", rateBasis = null)
        assertNull(assertNotNull(draft(cleared).parsed(30).record).products.single().rate)
    }
}
