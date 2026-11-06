package com.rncoding.testvineshield.core.domain.time

import kotlinx.datetime.TimeZone

/** Offline choices from the platform tzdb. Search text is never a stored zone ID. */
class VineyardTimeZoneCatalog(availableIds: Set<String> = TimeZone.availableZoneIds) {
    val ids: List<String> = availableIds.sorted()
    private val membership = availableIds.toSet()
    fun contains(id: String): Boolean = id in membership
    fun search(query: String): List<String> {
        val normalized = normalize(query.trim())
        return if (normalized.isEmpty()) ids else ids.filter { normalize(it).contains(normalized) }
    }
    private fun normalize(text: String): String = text.replace('_', ' ').replace('/', ' ').lowercase()
}
