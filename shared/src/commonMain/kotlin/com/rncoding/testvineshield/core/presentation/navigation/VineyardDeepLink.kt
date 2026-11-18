package com.rncoding.testvineshield.core.presentation.navigation

private const val PREFIX = "vineshield://vineyards/"

data class VineyardLinkTarget(val vineyardId: Long, val blockId: Long? = null)

fun parseVineyardLinkTarget(url: String?): VineyardLinkTarget? {
    if (url == null || !url.startsWith(PREFIX)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 1 && (parts.size != 3 || parts[1] != "blocks")) return null
    val vineyardId = parseLinkId(parts[0]) ?: return null
    val blockId = if (parts.size == 3) parseLinkId(parts[2]) ?: return null else null
    return VineyardLinkTarget(vineyardId, blockId)
}

fun parseVineyardDeepLink(url: String?): Long? =
    parseVineyardLinkTarget(url)?.takeIf { it.blockId == null }?.vineyardId

private fun parseLinkId(text: String): Long? {
    if (text.length !in 1..19 || text[0] !in '1'..'9' || text.any { it !in '0'..'9' }) return null
    return text.toLongOrNull()
}
