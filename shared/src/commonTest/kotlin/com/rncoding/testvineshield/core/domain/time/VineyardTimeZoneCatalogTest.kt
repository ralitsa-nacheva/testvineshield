package com.rncoding.testvineshield.core.domain.time

import kotlinx.datetime.TimeZone
import kotlin.test.*

class VineyardTimeZoneCatalogTest {
    private val catalog = VineyardTimeZoneCatalog(setOf("Europe/Sofia", "America/New_York", "Europe/Athens"))
    @Test fun optionsAreStableSortedIds() {
        assertEquals(listOf("America/New_York", "Europe/Athens", "Europe/Sofia"), catalog.search(""))
    }
    @Test fun matchesCaseInsensitiveCitiesAndSpaces() {
        assertEquals(listOf("Europe/Sofia"), catalog.search(" SOFIA "))
        assertEquals(listOf("America/New_York"), catalog.search("new york"))
        assertEquals(listOf("Europe/Sofia"), catalog.search("europe sofia"))
    }
    @Test fun searchDoesNotCreateNewIdsOrChangeSelection() {
        assertTrue(catalog.search("not a time zone").isEmpty())
        assertFalse(catalog.contains("Sofia")); assertTrue(catalog.contains("Europe/Sofia"))
    }
    @Test fun platformChoicesAreAcceptedByTimeZoneLibrary() {
        val actual = VineyardTimeZoneCatalog()
        assertTrue(actual.ids.isNotEmpty())
        for (id in actual.ids) TimeZone.of(id)
    }
}
