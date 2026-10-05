package com.rncoding.testvineshield.core.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onSecuritySettings: () -> Unit,
    onNewEmailChanged: (String) -> Unit,
    onCurrentPasswordChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    onUpdateAccount: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onClearError: () -> Unit
) {
    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    val isBusy =
        uiState.isUpdating ||
                uiState.isDeleting ||
                uiState.isLoggingOut

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Account")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !isBusy
                    ) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Account information")

            OutlinedTextField(
                value = uiState.newEmail,
                onValueChange = onNewEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Email")
                },
                singleLine = true,
                enabled = !isBusy
            )

            OutlinedTextField(
                value = uiState.currentPassword,
                onValueChange = onCurrentPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Current password")
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isBusy
            )

            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = onNewPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("New password (optional)")
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isBusy
            )

            OutlinedTextField(
                value = uiState.confirmNewPassword,
                onValueChange = onConfirmNewPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Confirm new password")
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isBusy
            )

            Button(
                onClick = onUpdateAccount,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Text(
                    if (uiState.isUpdating) {
                        "Updating..."
                    } else {
                        "Update account"
                    }
                )
            }

            uiState.errorMessage?.let { message ->
                Text(message)

                TextButton(
                    onClick = onClearError
                ) {
                    Text("Dismiss")
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            HorizontalDivider()

            Text("Security")

            OutlinedButton(
                onClick = onSecuritySettings,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Text("Security settings")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            HorizontalDivider()

            Text("Account actions")

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Text(
                    if (uiState.isLoggingOut) {
                        "Logging out..."
                    } else {
                        "Log out"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    showDeleteConfirmation = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Text("Delete account")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeleting) {
                    showDeleteConfirmation = false
                }
            },
            title = {
                Text("Delete account?")
            },
            text = {
                Text(
                    "This permanently deletes your account and its local vineyard data. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteAccount()
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        if (uiState.isDeleting) {
                            "Deleting..."
                        } else {
                            "Delete"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}