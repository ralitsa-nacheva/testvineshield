package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.error.AppError

@Composable
fun AuthStartupErrorScreen(
    error: AppError,
    onRetry: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Unable to start the application."
        )

        Text(
            text = error.userMessage
        )

        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}