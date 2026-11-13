package com.rncoding.testvineshield.core.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VineyardDeepLinkTest {
    @Test fun acceptsCanonicalPositiveIds() {
        assertEquals(42L, parseVineyardDeepLink("vineshield://vineyards/42"))
        assertEquals(Long.MAX_VALUE, parseVineyardDeepLink("vineshield://vineyards/${Long.MAX_VALUE}"))
    }

    @Test fun rejectsOtherRoutesAndNoncanonicalIds() {
        listOf(
            null, "https://vineyards/42", "vineshield://other/42", "vineshield://vineyards/42/blocks",
            "vineshield://vineyards/", "vineshield://vineyards/0", "vineshield://vineyards/-1",
            "vineshield://vineyards/042", "vineshield://vineyards/%34%32",
            "vineshield://vineyards/42?owner=1", "vineshield://vineyards/42#details",
            "vineshield://vineyards/9223372036854775808"
        ).forEach { url -> assertNull(parseVineyardDeepLink(url), url) }
    }
}
