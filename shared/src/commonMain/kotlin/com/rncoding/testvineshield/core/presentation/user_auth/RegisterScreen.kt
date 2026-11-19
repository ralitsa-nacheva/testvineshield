package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
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
            text = stringResource(Res.string.auth_create_account_title)
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(Res.string.auth_email))
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
                Text(stringResource(Res.string.auth_password))
            },
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.passwordError != null,
            singleLine = true
        )

        uiState.passwordError?.let { message ->
            Text(
                text = message
            )
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message
            )
        }

        Button(
            onClick = viewModel::register,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(Res.string.auth_register))
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            enabled = !uiState.isSubmitting
        ) {
            Text(stringResource(Res.string.auth_return_login))
        }
    }
}