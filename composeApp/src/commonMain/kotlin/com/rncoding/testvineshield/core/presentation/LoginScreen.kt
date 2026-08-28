package com.rncoding.testvineshield.core.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.presentation.user_auth.AuthViewModel

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    viewModel: AuthViewModel
) {
    val state by viewModel.uiState
        .collectAsState()

    Column {

        OutlinedTextField(
            value = state.email,
            onValueChange =
                viewModel::onEmailChanged,
            label = {
                Text("Email")
            }
        )

        OutlinedTextField(
            value = state.password,
            onValueChange =
                viewModel::onPasswordChanged,
            label = {
                Text("Password")
            }
        )

        state.error?.let { error ->
            Text(error.userMessage)
        }

        Button(
            enabled = !state.isSubmitting,
            onClick = onLogin
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Text("Login")
            }
        }

        TextButton(
            onClick = onRegister
        ) {
            Text("Create account")
        }
    }
}