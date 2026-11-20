package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.error.AppError

@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = stringResource(Res.string.error_generic_heading),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = localizedAppErrorMessage(error),
            modifier = Modifier.padding(top = 8.dp)
        )

        if (onRetry != null) {

            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {

                Text(stringResource(Res.string.retry))
            }
        }
    }
}