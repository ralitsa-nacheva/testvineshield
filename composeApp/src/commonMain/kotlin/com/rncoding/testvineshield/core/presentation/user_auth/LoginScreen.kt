package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.auth.AuthState

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    reason: AuthState.Unauthenticated.Reason,
    onNavigateToRegister: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "VineShield"
        )

        Text(
            text = "Login"
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Email")
            },
            isError = uiState.emailError != null,
            singleLine = true
        )

        uiState.emailError?.let { message ->

            Text(
                text = message
            )
        }

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            visualTransformation =
                PasswordVisualTransformation(),
            isError = uiState.passwordError != null,
            singleLine = true
        )

        uiState.passwordError?.let { message ->

            Text(
                text = message
            )
        }

        uiState.error?.let { error ->

            Text(
                text = error.userMessage
            )
        }

        Button(
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting
        ) {

            if (uiState.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Text("Login")
            }
        }

        TextButton(
            onClick = onNavigateToRegister,
            enabled = !uiState.isSubmitting
        ) {
            Text("Create an account")
        }
    }
}