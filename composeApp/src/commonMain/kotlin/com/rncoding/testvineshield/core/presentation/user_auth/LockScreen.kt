package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.auth.AuthState

@Composable
fun LockScreen(
    viewModel: AuthViewModel,
    reason: AuthState.Locked.LockReason
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
            text = "App Locked"
        )

        when (reason) {

            AuthState.Locked.LockReason.InactivityTimeout -> {
                Text(
                    text = "Your session was locked because of inactivity."
                )
            }

            AuthState.Locked.LockReason.AppLaunchRequiresUnlock -> {
                Text(
                    text = "Unlock to continue."
                )
            }
        }

        uiState.error?.let { error ->

            Text(
                text = error.userMessage
            )
        }

        Button(
            onClick = viewModel::unlock,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting
        ) {

            if (uiState.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Text("Unlock")
            }
        }
    }
}