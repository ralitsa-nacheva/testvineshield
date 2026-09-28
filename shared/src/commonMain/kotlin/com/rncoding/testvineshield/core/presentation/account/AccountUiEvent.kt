package com.rncoding.testvineshield.core.presentation.account

sealed interface AccountUiEvent {

    data object AccountUpdated : AccountUiEvent

    data object AccountDeleted : AccountUiEvent

    data object LoggedOut : AccountUiEvent
}