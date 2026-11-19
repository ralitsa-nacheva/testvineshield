package com.rncoding.testvineshield.core.presentation.language

import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

private const val PREFERENCE_KEY = "TestVineshield.appLanguage"
private const val APPLE_LANGUAGES = "AppleLanguages"

// A static provider invalidates the resource-reading subtree without keying/recreating
// AppRoot. Navigation, view models and rememberSaveable drafts retain their identity.
private val LocalIosResourceLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }

@OptIn(ExperimentalForeignApi::class)
private object IosLanguagePreference {
    private val defaults = NSUserDefaults.standardUserDefaults
    var selected by mutableStateOf(AppLanguage.fromTag(defaults.stringForKey(PREFERENCE_KEY)))
        private set

    init {
        if (selected != AppLanguage.SYSTEM) {
            defaults.setObject(listOf(selected.tag!!), forKey = APPLE_LANGUAGES)
        }
    }

    fun select(language: AppLanguage) {
        if (language == selected) return
        if (language == AppLanguage.SYSTEM) {
            defaults.removeObjectForKey(PREFERENCE_KEY)
            defaults.removeObjectForKey(APPLE_LANGUAGES)
        } else {
            defaults.setObject(language.tag!!, forKey = PREFERENCE_KEY)
            defaults.setObject(listOf(language.tag!!), forKey = APPLE_LANGUAGES)
        }
        selected = language
    }
}

@Composable
actual fun AppLanguageEnvironment(content: @Composable () -> Unit) {
    val selected = IosLanguagePreference.selected
    CompositionLocalProvider(
        LocalIosResourceLanguage provides selected,
        LocalAppLanguageSettings provides AppLanguageSettings(selected, IosLanguagePreference::select),
        content = content
    )
}
