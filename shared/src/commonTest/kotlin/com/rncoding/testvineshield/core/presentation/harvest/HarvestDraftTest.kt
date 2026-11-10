package com.rncoding.testvineshield.core.presentation.harvest

import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.domain.validation.HarvestValidator
import kotlinx.datetime.LocalDate
import kotlin.test.*

class HarvestDraftTest {
    private val scope = HarvestScope(10, 20)
    private fun draft() = HarvestDraft("2026/2027", "2026-11-09", "12,5", "kg")

    @Test fun unknownBrixIsNullAndCommaQuantityIsExplicit() {
        val record = assertNotNull(draft().parsed(scope, null).record)
        assertEquals(12.5, record.quantity)
        assertNull(record.brixSugarContent)
        assertEquals("2026/2027", record.season)
        assertEquals(LocalDate(2026, 11, 9), record.harvestDate)
        assertNull(HarvestValidator().validate(record))
    }
    @Test fun missingRequiredFieldsDoNotInventUnitsDatesOrQuantity() {
        val result = HarvestDraft().parsed(scope, null)
        assertNull(result.record)
        assertNotNull(result.unitError)
        for (field in listOf(HarvestField.Season, HarvestField.Date, HarvestField.Quantity)) assertTrue(field in result.errors)
    }
    @Test fun zeroBrixIsPreservedAndCanBeExplicitlyCleared() {
        val old = assertNotNull(draft().copy(brix = "0").parsed(scope, null).record).copy(harvestId = 40)
        assertEquals("0.0", HarvestDraft.from(old).brix)
        assertEquals(old, HarvestDraft.from(old).parsed(scope, old).record)
        val cleared = assertNotNull(HarvestDraft.from(old).copy(brix = "").parsed(scope, old).record)
        assertEquals(old.copy(brixSugarContent = null), cleared)
    }
    @Test fun invalidKnownBrixAndQuantityAreRejectedByDomainToo() {
        val base = assertNotNull(draft().parsed(scope, null).record)
        for (brix in listOf(-1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNotNull(HarvestValidator().validate(base.copy(brixSugarContent = brix)))
        }
        for (amount in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNotNull(HarvestValidator().validate(base.copy(quantity = amount)))
        }
    }
    @Test fun malformedDecimalsAndImpossibleDatesRemainInvalidAfterRestoration() {
        for (text in listOf("1,2.3", "1e3", "NaN", "1 000")) {
            val raw = draft().copy(quantity = text)
            val restored = assertNotNull(HarvestDraft.restore(raw.savedValues()))
            assertEquals(text, restored.quantity)
            assertNull(restored.parsed(scope, null).record)
        }
        assertTrue(HarvestField.Date in draft().copy(date = "2026-02-30").parsed(scope, null).errors)
        assertTrue(HarvestField.Brix in draft().copy(brix = "101").parsed(scope, null).errors)
    }
    @Test fun legacyUnitAndImmutableScopeArePreservedOnEdit() {
        val old = assertNotNull(draft().parsed(scope, null).record).copy(harvestId = 40, units = "baskets", brixSugarContent = 22.5)
        val edited = assertNotNull(HarvestDraft.from(old).copy(season = "Corrected").parsed(HarvestScope(999, 998), old).record)
        assertEquals(old.copy(season = "Corrected"), edited)
    }
    @Test fun changingUnitDoesNotConvertQuantity() {
        val first = assertNotNull(draft().parsed(scope, null).record)
        val changed = assertNotNull(draft().copy(units = "lb").parsed(scope, null).record)
        assertEquals(first.copy(units = "lb"), changed)
    }
    @Test fun draftRestorationPreservesUnfinishedTextAndNoUnitChoice() {
        val raw = HarvestDraft("2026", "unfinished", "12,", null, ".")
        assertEquals(raw, HarvestDraft.restore(raw.savedValues()))
        assertNull(HarvestDraft.restore(emptyList()))
    }
    @Test fun invalidHistoricalNumberCanBeDisplayedAndCorrected() {
        val old = assertNotNull(draft().parsed(scope, null).record).copy(brixSugarContent = Double.POSITIVE_INFINITY)
        val raw = HarvestDraft.from(old)
        assertEquals("Infinity", raw.brix)
        assertNull(raw.parsed(scope, old).record)
        assertNotNull(raw.copy(brix = "22.5").parsed(scope, old).record)
    }
}
