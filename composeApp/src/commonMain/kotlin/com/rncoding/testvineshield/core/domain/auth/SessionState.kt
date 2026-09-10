package com.rncoding.testvineshield.core.domain.auth

sealed interface SessionState {
    data object Active : SessionState
    //object RequiresReauth : SessionState
    data object TimedOut : SessionState
    data object Expired : SessionState
    data object LoggedOut : SessionState
}