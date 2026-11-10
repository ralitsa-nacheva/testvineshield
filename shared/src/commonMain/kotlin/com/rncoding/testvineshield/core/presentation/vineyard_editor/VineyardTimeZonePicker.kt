package com.rncoding.testvineshield.core.presentation.vineyard_editor

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.presentation.components.TimeZonePicker

@Composable
internal fun VineyardTimeZonePicker(selectedId: String, errorMessage: String?, enabled: Boolean, onSelect: (String) -> Unit) =
    TimeZonePicker(selectedId, errorMessage, enabled, onSelect)
