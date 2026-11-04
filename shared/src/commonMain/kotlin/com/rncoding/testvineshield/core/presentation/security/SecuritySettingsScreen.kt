package com.rncoding.testvineshield.core.presentation.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    state: SecuritySettingsUiState,
    onBack: () -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    onPinChanged: (Boolean) -> Unit,
    onReload: () -> Unit,
    onClearError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Security")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !state.isUpdating
                    ) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            if (state.biometricAvailable || state.biometricEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Biometric authentication",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = state.biometricEnabled,
                        onCheckedChange = onBiometricChanged,
                        enabled =
                            !state.isLoading &&
                                    !state.isUpdating
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "App PIN (setup not available yet)",
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = state.pinEnabled,
                    onCheckedChange = onPinChanged,
                    enabled =
                        state.pinEnabled &&
                                !state.isLoading &&
                                !state.isUpdating
                )
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 16.dp)
                )

                TextButton(
                    onClick = onClearError
                ) {
                    Text("Dismiss")
                }
                TextButton(
                    onClick = onReload,
                    enabled = !state.isLoading && !state.isUpdating
                ) {
                    Text("Reload settings")
                }
            }
        }
    }
}