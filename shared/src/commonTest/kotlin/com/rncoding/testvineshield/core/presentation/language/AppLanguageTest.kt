package com.rncoding.testvineshield.core.presentation.language

import kotlin.test.*

class AppLanguageTest {
    @Test fun absentAndUnsupportedValuesUseSystem() {
        listOf(null, "", "fr", "invalid").forEach { assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(it)) }
        assertNull(AppLanguage.SYSTEM.tag)
    }
    @Test fun supportedRegionalTagsResolveToTheirLanguage() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.BULGARIAN, AppLanguage.fromTag("bg-BG"))
        assertEquals(AppLanguage.BULGARIAN, AppLanguage.fromTag("BG_bg"))
    }
}
