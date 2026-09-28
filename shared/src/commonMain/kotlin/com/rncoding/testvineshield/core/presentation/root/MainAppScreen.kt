package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun MainAppScreen(
    user: UserDomainModel
) {
    var showAccount by remember {
        mutableStateOf(false)
    }

    if (showAccount) {

        AccountScreenRoot(
            onBack = {
                showAccount = false
            },
            onSecuritySettings = {
                // Connect to SecuritySettingsScreen
                // after the basic Account flow compiles.
            }
        )

    } else {

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
                text = "Welcome"
            )

            Text(
                text = user.userEmail
            )

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