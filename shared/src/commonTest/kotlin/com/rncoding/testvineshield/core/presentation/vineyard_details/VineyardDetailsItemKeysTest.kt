package com.rncoding.testvineshield.core.presentation.vineyard_details

import kotlin.test.*

class VineyardDetailsItemKeysTest {
    private val fixedKeys = listOf(
        VineyardDetailsItemKeys.INFORMATION, VineyardDetailsItemKeys.WEATHER,
        VineyardDetailsItemKeys.BLOCKS_HEADING, VineyardDetailsItemKeys.BLOCKS_EMPTY,
        VineyardDetailsItemKeys.DISEASES_HEADING, VineyardDetailsItemKeys.DISEASES_EMPTY,
        VineyardDetailsItemKeys.ALERTS_HEADING, VineyardDetailsItemKeys.ALERTS_EMPTY,
        VineyardDetailsItemKeys.DELETE
    )

    @Test fun independentTablesMayReuseTheSameNumericIdsInOneLazyList() {
        // Reproduces the reported collision: block 1, occurrence 1 and alert 1 coexist.
        val ids = listOf(1L, 2L, Long.MAX_VALUE)
        val keys = fixedKeys + ids.flatMap { id -> listOf(
            VineyardDetailsItemKeys.block(id), VineyardDetailsItemKeys.occurrence(id),
            VineyardDetailsItemKeys.alert(id)
        ) }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test fun sectionInsertionsAndReorderingDoNotChangeRecordIdentity() {
        val before = listOf(1L, 2L).associateWith(VineyardDetailsItemKeys::block)
        val after = listOf(2L, 1L, 3L).associateWith(VineyardDetailsItemKeys::block)
        assertEquals(before.getValue(1L), after.getValue(1L))
        assertEquals(before.getValue(2L), after.getValue(2L))
        assertNotEquals(VineyardDetailsItemKeys.BLOCKS_EMPTY, after.getValue(1L))
    }
}
