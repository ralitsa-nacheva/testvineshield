package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.security.LocalUnlockMethod
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockRequest

@Composable
fun LockScreen(viewModel: AuthViewModel, reason: AuthState.Locked.LockReason) {
    val state by viewModel.uiState.collectAsState()
    val options by viewModel.unlockOptions.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshUnlockOptions() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center) {
        Text("VineShield", style = MaterialTheme.typography.headlineMedium)
        Text("App locked after inactivity")
        Text("Use your local account password. Optional methods appear when enabled and available.")
        OutlinedTextField(value = state.password, onValueChange = viewModel::onPasswordChanged,
            label = { Text("Account password") }, singleLine = true, enabled = !state.isSubmitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Button(onClick = viewModel::unlockWithPassword,
            enabled = !state.isSubmitting && state.password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Unlock with password")
        }
        if (options.biometric) Button(onClick = { viewModel.unlock(UnlockRequest.Platform(LocalUnlockMethod.Biometric)) },
            enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) { Text("Unlock with biometrics") }
        if (options.deviceCredential) Button(onClick = { viewModel.unlock(UnlockRequest.Platform(LocalUnlockMethod.DeviceCredential)) },
            enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) { Text("Unlock with device screen lock") }
        if (state.isSubmitting) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = viewModel::logout, enabled = !state.isSubmitting) { Text("Log out and return to login") }
    }
}
