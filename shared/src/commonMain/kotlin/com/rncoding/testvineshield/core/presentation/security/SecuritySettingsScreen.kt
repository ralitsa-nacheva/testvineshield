package com.rncoding.testvineshield.core.presentation.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.presentation.root.userAction
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.security.SecuritySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    state: SecuritySettingsUiState,
    onBack: () -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onTimeoutChanged: (Int) -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    onDeviceCredentialChanged: (Boolean) -> Unit,
    onReload: () -> Unit,
    onClearError: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Security") }, navigationIcon = {
        TextButton(onClick = userAction(onBack), enabled = !state.isUpdating) { Text("Back") }
    }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Inactivity lock is optional and off by default. Your account password is always available when locked.")
            SecuritySwitch("Lock after inactivity", state.settings.inactivityLockEnabled, state.canEdit, onLockChanged)
            val active = state.canEdit && state.settings.inactivityLockEnabled
            Text("Lock after ${state.settings.inactivityTimeoutMillis / 60_000L} minutes without interaction")
            // A bounded list avoids invalid, zero or accidentally enormous timeout values.
            SecuritySettings.timeoutMinutes.forEach { minutes ->
                Row {
                    RadioButton(selected = state.settings.inactivityTimeoutMillis == minutes * 60_000L,
                        onClick = userAction { onTimeoutChanged(minutes) }, enabled = active)
                    Text("$minutes minutes", Modifier.padding(top = 12.dp))
                }
            }
            Text("Account password — always available", style = MaterialTheme.typography.titleSmall)
            SecuritySwitch("Biometric unlock", state.settings.biometricEnabled,
                active && (state.biometricAvailable || state.settings.biometricEnabled), onBiometricChanged)
            if (!state.biometricAvailable) Text("Biometrics are unavailable or not enrolled. Password unlock still works.")
            SecuritySwitch("Device screen-lock unlock", state.settings.deviceCredentialEnabled,
                active && (state.deviceCredentialAvailable || state.settings.deviceCredentialEnabled), onDeviceCredentialChanged)
            Text("Uses your device PIN, pattern, password or passcode. On iOS, the system may also offer biometrics. No separate app PIN is stored.")
            if (!state.deviceCredentialAvailable) Text("Configure a screen lock in device settings to enable this option.")
            Text("Turning inactivity lock off also turns both optional unlock methods off. Turning it on again starts with password unlock.")
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(onClearError)) { Text("Dismiss") }
            }
            TextButton(onClick = userAction(onReload), enabled = !state.isLoading && !state.isUpdating) { Text("Reload availability") }
        }
    }
}

@Composable
private fun SecuritySwitch(label: String, checked: Boolean, enabled: Boolean, changed: (Boolean) -> Unit) {
    val activity = com.rncoding.testvineshield.core.presentation.root.LocalUserActivity.current
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f).padding(top = 12.dp))
        Switch(checked = checked, onCheckedChange = { activity(); changed(it) }, enabled = enabled)
    }
}
