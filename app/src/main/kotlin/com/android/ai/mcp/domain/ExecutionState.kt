package com.android.ai.mcp.domain

enum class ExecutionState {
    IDLE,
    PLANNING,
    READY_FOR_CONFIRMATION,
    RUNNING,
    AWAITING_USER,
    STOPPED,
    FAILED,
    COMPLETED
}
