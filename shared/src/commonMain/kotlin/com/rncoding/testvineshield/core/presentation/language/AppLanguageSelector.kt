package com.rncoding.testvineshield.core.presentation.language

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLanguageSelector(enabled: Boolean = true) {
    val settings = LocalAppLanguageSettings.current
    val labels = mapOf(
        AppLanguage.SYSTEM to stringResource(Res.string.language_follow_system),
        AppLanguage.ENGLISH to "English",
        AppLanguage.BULGARIAN to "Български"
    )
    Column {
        RecordChoice(stringResource(Res.string.language_title), settings.selected, AppLanguage.entries,
            enabled, { labels.getValue(it) }, { it?.let(settings.select) }, allowUnknown = false)
        Text(stringResource(Res.string.language_partial_help))
    }
}
