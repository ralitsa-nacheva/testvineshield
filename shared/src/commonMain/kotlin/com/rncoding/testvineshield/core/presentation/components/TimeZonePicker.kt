package com.rncoding.testvineshield.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.root.userActivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog

@Composable
internal fun TimeZonePicker(
    selectedId: String,
    errorMessage: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val activity = com.rncoding.testvineshield.core.presentation.root.LocalUserActivity.current
    val catalog = remember { VineyardTimeZoneCatalog() }
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(enabled) { if (!enabled) open = false }
    Column {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(), enabled = enabled,
            onClick = userAction { query = ""; open = true }
        ) { Text("Time zone: " + selectedId.ifBlank { "Choose time zone" }) }
        if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error)
    }
    if (open && enabled) {
        val matches = remember(query, catalog) { catalog.search(query) }
        Dialog(onDismissRequest = userAction { open = false }) {
            Surface(modifier = Modifier.userActivity(activity), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(16.dp)) {
                    Text("Choose time zone", style = MaterialTheme.typography.titleLarge)
                    if (selectedId.isNotBlank()) Text("Current: $selectedId")
                    OutlinedTextField(
                        value = query, onValueChange = { activity(); query = it }, singleLine = true,
                        label = { Text("Search city or zone, e.g. Sofia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (matches.isEmpty()) Text("No matching time zones.")
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(matches, key = { it }) { id ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = userAction { activity(); if (enabled) { onSelect(id); open = false } }
                            ) { Text(if (id == selectedId) "$id (selected)" else id) }
                        }
                    }
                    TextButton(onClick = userAction { open = false }) { Text("Cancel") }
                }
            }
        }
    }
}
