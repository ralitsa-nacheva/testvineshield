package com.rncoding.testvineshield.core.presentation.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val tag: String?) {
    SYSTEM(null), ENGLISH("en"), BULGARIAN("bg");

    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.replace('_', '-')?.substringBefore('-')?.lowercase()) {
            "en" -> ENGLISH
            "bg" -> BULGARIAN
            else -> SYSTEM
        }
    }
}

internal data class AppLanguageSettings(val selected: AppLanguage, val select: (AppLanguage) -> Unit)
internal val LocalAppLanguageSettings = staticCompositionLocalOf {
    AppLanguageSettings(AppLanguage.SYSTEM) { }
}

/** Platform storage is device-level, outside user accounts and their encrypted database. */
@Composable
expect fun AppLanguageEnvironment(content: @Composable () -> Unit)
