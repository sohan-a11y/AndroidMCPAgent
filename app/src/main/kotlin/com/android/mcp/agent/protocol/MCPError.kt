package com.android.mcp.agent.protocol

/**
 * Standard error codes for the MCP protocol.
 */
enum class MCPError(val defaultMessage: String) {
    // Auth errors
    UNAUTHORIZED("Authentication required or token invalid"),
    PAIRING_FAILED("Pairing code incorrect or expired"),
    SESSION_EXPIRED("Session has expired, re-pair required"),

    // Permission errors
    PERMISSION_DENIED("This action type is not permitted by the user"),
    ACCESSIBILITY_NOT_ENABLED("Accessibility service is not enabled"),

    // Command errors
    INVALID_COMMAND("Unknown or malformed command"),
    MISSING_PARAMS("Required parameters are missing"),
    INVALID_PARAMS("Parameter values are invalid"),

    // Execution errors
    EXECUTION_FAILED("Command execution failed"),
    ELEMENT_NOT_FOUND("UI element not found on screen"),
    APP_NOT_FOUND("Application not installed"),
    TIMEOUT("Command execution timed out"),

    // System errors
    SERVICE_UNAVAILABLE("MCP agent service is not running"),
    INTERNAL_ERROR("Internal server error"),
    NOT_IMPLEMENTED("This action is not yet implemented")
}
