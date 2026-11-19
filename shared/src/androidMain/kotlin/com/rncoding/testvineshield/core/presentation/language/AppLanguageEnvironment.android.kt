package com.rncoding.testvineshield.core.presentation.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat

@Composable
actual fun AppLanguageEnvironment(content: @Composable () -> Unit) {
    // Subscribe to configuration changes, including selections made in Android Settings.
    val configuration = LocalConfiguration.current
    val selected = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag())
    val settings = AppLanguageSettings(selected) { language ->
        if (language != selected) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag.orEmpty()))
        }
    }
    // Reading configuration ensures this provider is invalidated after an OS locale change.
    @Suppress("UNUSED_VARIABLE") val localeRevision = configuration.locales.toLanguageTags()
    CompositionLocalProvider(LocalAppLanguageSettings provides settings, content = content)
}
