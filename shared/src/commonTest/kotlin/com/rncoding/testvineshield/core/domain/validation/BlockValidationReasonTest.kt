package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.BlockValidationReason
import kotlin.test.*

class BlockValidationReasonTest {
    private val validator = BlockValidator()
    private val valid = BlockDomainModel(0, 10, "North", null, "Local", "", "", null, null, null, null)

    @Test fun optionalUnknownValuesRemainValidAndFailuresAreTyped() {
        assertNull(validator.validate(valid))
        val cases = listOf(
            valid.copy(vineyardId = 0) to BlockValidationReason.Vineyard,
            valid.copy(name = " ") to BlockValidationReason.Name,
            valid.copy(vineVariety = " ") to BlockValidationReason.Variety,
            valid.copy(area = Double.NaN) to BlockValidationReason.Area,
            valid.copy(rowCount = 0) to BlockValidationReason.Rows,
            valid.copy(rowSpacing = 0.0) to BlockValidationReason.Spacing
        )
        for ((record, expected) in cases) assertEquals(expected, assertNotNull(validator.validate(record)).reason)
    }
}
