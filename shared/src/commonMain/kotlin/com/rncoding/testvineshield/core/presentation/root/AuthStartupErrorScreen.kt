package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = stringResource(Res.string.startup_unable)
        )

        Text(
            text = localizedAppErrorMessage(error)
        )

        Button(
            onClick = onRetry
        ) {
            Text(stringResource(Res.string.retry))
        }
    }
}