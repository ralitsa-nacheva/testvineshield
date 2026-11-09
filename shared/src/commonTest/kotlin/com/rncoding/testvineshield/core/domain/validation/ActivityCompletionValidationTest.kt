package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActivityCompletionValidationTest {
    private val validator = ActivityValidator()

    private fun record(status: ActivityStatus, actual: LocalDate?) = ActivityDomainModel(
        activityId = 0L,
        vineyardId = 10L,
        blockId = null,
        activityType = "Pruning",
        activityDescription = "",
        season = "2026/2027",
        phenologicalStage = null,
        priority = "medium",
        status = status.dbValue,
        plannedAt = LocalDate(2027, 1, 2),
        completedAt = actual,
        tool = null,
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
        treatmentProduct = null,
        treatmentProductQuantity = null,
        treatmentProductUnits = null,
        waterQuantity = null,
        treatmentProductRate = null
    )

    @Test
    fun earlyCompletionCanCrossCalendarYearWithoutChangingSeason() {
        assertNull(validator.validate(record(ActivityStatus.COMPLETED, LocalDate(2026, 12, 31))))
    }

    @Test
    fun completedStatusStillRequiresAnActualDate() {
        assertNotNull(validator.validate(record(ActivityStatus.COMPLETED, null)))
    }

    @Test
    fun otherStatusesRejectActualDatesWhetherBeforeOrAfterPlan() {
        for (status in ActivityStatus.entries.filter { it != ActivityStatus.COMPLETED }) {
            for (actual in listOf(LocalDate(2026, 12, 31), LocalDate(2027, 1, 3))) {
                assertNotNull(validator.validate(record(status, actual)))
            }
            assertNull(validator.validate(record(status, null)))
        }
    }
}
