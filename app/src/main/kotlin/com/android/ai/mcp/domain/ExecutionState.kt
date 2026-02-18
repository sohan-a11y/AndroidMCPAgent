package com.android.ai.mcp.domain

enum class ExecutionState {
    IDLE,
    PLANNING,
    READY_FOR_CONFIRMATION,
    RUNNING,
    STOPPED,
    FAILED,
    COMPLETED
}
