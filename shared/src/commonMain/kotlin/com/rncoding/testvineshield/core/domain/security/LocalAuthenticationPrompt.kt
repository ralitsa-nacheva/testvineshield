package com.rncoding.testvineshield.core.domain.security

/** Display-only text supplied by the caller; never changes authentication policy. */
data class LocalAuthenticationPrompt(
    val reason: String,
    val title: String = "Unlock Vineyard",
    val cancelLabel: String = "Cancel"
)
