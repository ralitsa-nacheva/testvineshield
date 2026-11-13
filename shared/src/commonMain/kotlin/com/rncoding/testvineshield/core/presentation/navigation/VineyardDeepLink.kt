package com.rncoding.testvineshield.core.presentation.navigation

private const val PREFIX = "vineshield://vineyards/"

fun parseVineyardDeepLink(url: String?): Long? {
    if (url == null || !url.startsWith(PREFIX)) return null
    val id = url.substring(PREFIX.length)
    if (id.length !in 1..19 || id[0] !in '1'..'9' || id.any { it !in '0'..'9' }) return null
    return id.toLongOrNull()
}
