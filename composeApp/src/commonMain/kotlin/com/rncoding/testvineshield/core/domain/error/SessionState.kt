package com.rncoding.testvineshield.core.domain.error

sealed interface SessionState {
    object Active : SessionState
    object RequiresReauth : SessionState
    object TimedOut : SessionState
    object Expired : SessionState
    object LoggedOut : SessionState
}