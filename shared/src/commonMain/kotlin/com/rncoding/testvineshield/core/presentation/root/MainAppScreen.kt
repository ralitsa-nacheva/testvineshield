package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.presentation.account.AccountScreenRoot
import com.rncoding.testvineshield.core.presentation.security.SecuritySettingsScreenRoot

@Composable
fun MainAppScreen(user: UserDomainModel) {

    var showAccount by remember { mutableStateOf(false) }
    var showSecuritySettings by remember { mutableStateOf(false) }

    when {
        showSecuritySettings -> {
            SecuritySettingsScreenRoot(
                userId = user.userId,
                onBack = {
                    showSecuritySettings = false
                }
            )
        }

        showAccount -> {
            AccountScreenRoot(
                onBack = {
                    showAccount = false
                },
                onSecuritySettings = {
                    showSecuritySettings = true
                }
            )
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("VineShield")

                Text("Welcome")

                Text(user.userEmail)

                Button(
                    onClick = {
                        showAccount = true
                    }
                ) {
                    Text("Account")
                }
            }
        }
    }
}