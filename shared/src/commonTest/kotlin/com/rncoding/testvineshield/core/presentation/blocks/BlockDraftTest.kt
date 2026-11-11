package com.rncoding.testvineshield.core.presentation.blocks

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class BlockDraftTest {
    private fun draft() = BlockDraft(name = "North", variety = "Merlot")
    @Test fun blankOptionalFieldsAreNullNotZeroOrToday() {
        val block = assertNotNull(draft().parsed(10, 0).block)
        assertNull(block.area)
        assertNull(block.rowCount)
        assertNull(block.rowSpacing)
        assertNull(block.plantedAt)
        assertNull(block.phenologicalStage)
    }
    @Test fun commaAndDotFractionsAndUnitsRoundTrip() {
        val block = assertNotNull(draft().copy(area = "1,25", spacing = "2.45", rows = "5", areaUnit = AreaUnit.HECTARE, spacingUnit = LengthUnit.METRE).parsed(10, 0).block)
        assertEquals(1.25, block.area)
        assertEquals(2.45, block.rowSpacing)
        assertEquals(5, block.rowCount)
        assertEquals(AreaUnit.HECTARE, block.areaUnit)
        assertEquals(LengthUnit.METRE, block.rowSpacingUnit)
    }
    @Test fun invalidAndNonPositiveMeasuresAreRejected() {
        listOf("0", "-1", "NaN", "Infinity", "1e3", "1,234.5", "bad").forEach {
            assertTrue(BlockField.Area in draft().copy(area = it).parsed(10, 0).errors)
            assertTrue(BlockField.Spacing in draft().copy(spacing = it).parsed(10, 0).errors)
        }
    }
    @Test fun rowCountMustBePositiveWholeInt() {
        listOf("0", "-1", "2.5", "2,5", "2147483648").forEach {assertTrue(BlockField.Rows in draft().copy(rows = it).parsed(10, 0).errors)}
    }
    @Test fun plantingDateIsOptionalExactAndIndependentFromStage() {
        val block = assertNotNull(draft().copy(planted = "2020-04-15").parsed(10, 0).block)
        assertEquals(LocalDate(2020, 4, 15), block.plantedAt)
        assertNull(block.phenologicalStage)
        assertTrue(BlockField.Planted in draft().copy(planted = "2020-02-30").parsed(10, 0).errors)
    }
    @Test fun unknownUnitsDoNotChangeNumbers() {
        val original = draft().copy(area = "1.25", spacing = "245", areaUnit = AreaUnit.HECTARE, spacingUnit = LengthUnit.CENTIMETRE)
        assertEquals(original.parsed(10, 20).block?.area, original.copy(areaUnit = null).parsed(10, 20).block?.area)
        assertEquals(original.parsed(10, 20).block?.rowSpacing, original.copy(spacingUnit = null).parsed(10, 20).block?.rowSpacing)
    }
    @Test fun savedDraftPreservesInvalidTextAndKnownChoicesUntilExplicitSave() {
        val original = draft().copy(area = "bad text", planted = "unfinished", stage = PhenologicalStage.FLOWERING, areaUnit = AreaUnit.ACRE)
        assertEquals(original, BlockDraft.restored(original.savedValues()))
        assertNull(BlockDraft.restored(emptyList()))
        assertNull(BlockDraft.restored(original.savedValues().also {it[8] = "unsupported-unit"}))
    }
    @Test fun loadSmallFractionAndKnownDateDoesNotBecomeInvalidExponent() {
        val model = BlockDomainModel(20, 10, "North", 0.0000001, "Merlot", "red", "", 5, 2.45, LocalDate(2020, 4, 15), null)
        val draft = BlockDraft.from(model)
        assertEquals(model, draft.parsed(10, 20).block)
    }
    @Test fun nameAndVarietyRequiredButOtherTextCanBeUnknown() {
        val result = BlockDraft().parsed(10, 0)
        assertTrue(BlockField.Name in result.errors)
        assertTrue(BlockField.Variety in result.errors)
        assertNotNull(draft().parsed(10, 0).block)
    }

    @Test fun observationDateSurvivesDraftRestoreAndUnrelatedEdit() {
        val original = draft().copy(stage = PhenologicalStage.FLOWERING, stageObservedOn = "2024-05-17")
        val restored = assertNotNull(BlockDraft.restored(original.savedValues()))
        val block = assertNotNull(restored.parsed(10, 20).block)
        val renamed = assertNotNull(BlockDraft.from(block).copy(name = "South").parsed(10, 20).block)
        assertEquals(LocalDate(2024, 5, 17), renamed.stageObservedOn)
        val oldDraft = assertNotNull(BlockDraft.restored(original.savedValues().dropLast(1)))
        assertEquals(PhenologicalStage.FLOWERING, oldDraft.stage)
        assertNull(oldDraft.parsed(10, 20).block?.stageObservedOn)
    }

    @Test fun observationDateRequiresAStageAndARealCalendarDate() {
        val dated = draft().copy(stageObservedOn = "2024-02-29")
        assertTrue(BlockField.StageObservedOn in dated.parsed(10, 20).errors)
        val invalid = dated.copy(stage = PhenologicalStage.FLOWERING, stageObservedOn = "2023-02-29")
        assertTrue(BlockField.StageObservedOn in invalid.parsed(10, 20).errors)
    }
}
