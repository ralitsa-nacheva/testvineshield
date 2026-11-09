package com.rncoding.testvineshield.core.presentation.activities

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import com.rncoding.testvineshield.core.domain.validation.ActivityValidator
import kotlinx.datetime.LocalDate
import kotlin.test.*

class ActivityDraftTest {
    private fun draft()=ActivityDraft(type="Pruning",season="2026",planned="2026-11-09")
    @Test fun unknownStageAndWholeVineyardScopeStayNull() {
        val record=assertNotNull(draft().parsed(10,null,null).record)
        assertNull(record.phenologicalStage);assertNull(record.blockId)
        assertNull(ActivityValidator().validate(record))
    }
    @Test fun blankNonNullStageIsStillInvalid() {
        val record=assertNotNull(draft().parsed(10,null,null).record)
        assertNotNull(ActivityValidator().validate(record.copy(phenologicalStage="")))
    }
    @Test fun requiredTextAndDateHaveFieldErrors() {
        val result=ActivityDraft().parsed(10,null,null)
        assertTrue(ActivityField.Type in result.errors);assertTrue(ActivityField.Season in result.errors);assertTrue(ActivityField.Planned in result.errors)
    }
    @Test fun impossibleDateIsNotReplacedWithToday() {
        val result=draft().copy(planned="2026-02-30").parsed(10,null,null)
        assertNull(result.record);assertTrue(ActivityField.Planned in result.errors)
    }
    @Test fun completionRequiresExplicitDate() {
        val result=draft().copy(status=ActivityStatus.COMPLETED).parsed(10,null,null)
        assertNull(result.record);assertTrue(ActivityField.Completed in result.errors)
        val completed=assertNotNull(draft().copy(status=ActivityStatus.COMPLETED,completed="2026-11-10").parsed(10,null,null).record)
        assertEquals(LocalDate(2026,11,10),completed.completedAt)
    }
    @Test fun nonCompletedDateAndCurrentDateOrderRuleAreEnforced() {
        assertTrue(ActivityField.Completed in draft().copy(completed="2026-11-10").parsed(10,null,null).errors)
        assertTrue(ActivityField.Completed in draft().copy(status=ActivityStatus.COMPLETED,completed="2026-11-08").parsed(10,null,null).errors)
    }
    @Test fun basicEditPreservesHiddenTreatmentIdentityAndMetadata() {
        val base=assertNotNull(draft().parsed(10,20,null).record).copy(activityId=30,createdAt=1,updatedAt=2,
            treatmentProduct="Existing product",treatmentProductQuantity=0.5,treatmentProductUnits="kg",waterQuantity=10.0,treatmentProductRate=0.125)
        val edited=ActivityDraft.from(base).copy(notes="New notes").parsed(10,null,base).record
        assertEquals(base.copy(notes="New notes"),edited)
    }
    @Test fun blockScopeAndManualStageAreExplicit() {
        val record=assertNotNull(draft().copy(stage=PhenologicalStage.FLOWERING).parsed(10,20,null).record)
        assertEquals(20L,record.blockId);assertEquals("flowering",record.phenologicalStage)
    }
    @Test fun unfinishedDraftRoundTripsWithStableCodes() {
        val original=draft().copy(planned="unfinished",status=ActivityStatus.IN_PROGRESS,priority=ActivityPriority.HIGH)
        assertEquals(original,ActivityDraft.restore(original.savedValues()))
        assertNull(ActivityDraft.restore(emptyList()))
        assertNull(ActivityDraft.restore(original.savedValues().also {it[7]="not-a-priority"}))
    }
}
