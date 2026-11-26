package com.rncoding.testvineshield.core.presentation.security

import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
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
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.security_title)) }, navigationIcon = {
        TextButton(onClick = userAction(onBack), enabled = !state.isUpdating) { Text(stringResource(Res.string.back_action)) }
    }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            InfoHelp(title = stringResource(Res.string.security_lock_toggle), text = stringResource(Res.string.security_lock_help))
            SecuritySwitch(stringResource(Res.string.security_lock_toggle), state.settings.inactivityLockEnabled, state.canEdit, onLockChanged)
            val active = state.canEdit && state.settings.inactivityLockEnabled
            Text(stringResource(Res.string.security_timeout_summary, (state.settings.inactivityTimeoutMillis / 60_000L).toInt()))
            // A bounded list avoids invalid, zero or accidentally enormous timeout values.
            SecuritySettings.timeoutMinutes.forEach { minutes ->
                Row {
                    RadioButton(selected = state.settings.inactivityTimeoutMillis == minutes * 60_000L,
                        onClick = userAction { onTimeoutChanged(minutes) }, enabled = active)
                    Text(stringResource(Res.string.security_timeout_minutes, minutes), Modifier.padding(top = 12.dp))
                }
            }
            Text(stringResource(Res.string.security_password_available), style = MaterialTheme.typography.titleSmall)
            SecuritySwitch(stringResource(Res.string.security_biometric), state.settings.biometricEnabled,
                active && (state.biometricAvailable || state.settings.biometricEnabled), onBiometricChanged)
            if (!state.biometricAvailable) Text(stringResource(Res.string.security_biometric_unavailable))
            SecuritySwitch(stringResource(Res.string.security_device_unlock), state.settings.deviceCredentialEnabled,
                active && (state.deviceCredentialAvailable || state.settings.deviceCredentialEnabled), onDeviceCredentialChanged)
            InfoHelp(title = stringResource(Res.string.security_device_unlock), text = stringResource(Res.string.security_device_help))
            if (!state.deviceCredentialAvailable) Text(stringResource(Res.string.security_device_unavailable))
            InfoHelp(title = stringResource(Res.string.security_lock_toggle), text = stringResource(Res.string.security_disable_help))
            state.operationIssue?.let {
                Text(localizedSecuritySettingsIssue(it), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(onClearError)) { Text(stringResource(Res.string.dismiss_action)) }
            }
            TextButton(onClick = userAction(onReload), enabled = !state.isLoading && !state.isUpdating) { Text(stringResource(Res.string.security_reload)) }
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
