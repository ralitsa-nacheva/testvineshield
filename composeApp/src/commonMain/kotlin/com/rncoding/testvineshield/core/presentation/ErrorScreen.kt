package com.rncoding.testvineshield.core.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ErrorScreen(message: String) {
    Column {
        Text("Error: $message")
    }
}
