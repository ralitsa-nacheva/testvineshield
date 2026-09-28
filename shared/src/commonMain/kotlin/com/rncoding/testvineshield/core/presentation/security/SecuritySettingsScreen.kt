package com.rncoding.testvineshield.core.presentation.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SecuritySettingsScreen(
    state: SecuritySettingsUiState,
    onBiometricChanged: (Boolean) -> Unit,
    onPinChanged: (Boolean) -> Unit
) {
    Column {

        Text("Security")

        if (state.biometricAvailable) {

            Row {
                Text("Biometric authentication")

                Switch(
                    checked =
                        state.biometricEnabled,
                    onCheckedChange =
                        onBiometricChanged
                )
            }
        }

        Row {
            Text("PIN fallback")

            Switch(
                checked =
                    state.pinEnabled,
                onCheckedChange =
                    onPinChanged)
        }
    }

    state.error?.let {
        Text(it.userMessage)
    }
}
