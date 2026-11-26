package com.rncoding.testvineshield.core.presentation.language

import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import androidx.compose.foundation.layout.Column
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
        InfoHelp(title = stringResource(Res.string.language_title), text = stringResource(Res.string.language_partial_help))
    }
}
