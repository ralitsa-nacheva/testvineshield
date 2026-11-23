package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticationPrompt
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedLocalAuthenticationPrompt(reason: String): LocalAuthenticationPrompt = LocalAuthenticationPrompt(
    reason = reason,
    title = stringResource(Res.string.local_auth_title),
    cancelLabel = stringResource(Res.string.local_auth_cancel)
)
