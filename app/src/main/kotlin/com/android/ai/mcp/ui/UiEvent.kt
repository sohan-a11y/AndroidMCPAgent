package com.android.ai.mcp.ui

sealed interface UiEvent {
    data object NavigateToPreview : UiEvent
    data object RequestBiometricUnlock : UiEvent
}
