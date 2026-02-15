package com.android.mcp.agent

import android.app.Application
import android.util.Log
import com.android.mcp.agent.commands.CommandRouter
import com.android.mcp.agent.commands.handlers.*
import com.android.mcp.agent.logging.AuditLogger
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.permissions.PermissionStore
import com.android.mcp.agent.server.AuthManager
import com.android.mcp.agent.server.SessionManager
import com.android.mcp.agent.server.WebSocketServer

/**
 * Application class — initializes all core components.
 *
 * This is the dependency root. All shared instances are created here
 * and accessed by services, activities, and handlers.
 *
 * No DI framework — manual wiring keeps things simple for MVP.
 */
class MCPApplication : Application() {

    companion object {
        private const val TAG = "MCPApplication"
        const val DEFAULT_PORT = 8765
    }

    // Core components
    lateinit var permissionStore: PermissionStore
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var auditLogger: AuditLogger
        private set
    lateinit var authManager: AuthManager
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var commandRouter: CommandRouter
        private set
    lateinit var webSocketServer: WebSocketServer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing MCP Agent application")

        // Initialize in dependency order
        permissionStore = PermissionStore(this)
        permissionManager = PermissionManager(permissionStore)
        auditLogger = AuditLogger()
        authManager = AuthManager()
        sessionManager = SessionManager()

        // Command router with all handlers
        commandRouter = CommandRouter(permissionManager, auditLogger).apply {
            registerAll(
                OpenAppHandler(this@MCPApplication),
                ClickHandler(),
                InputTextHandler(),
                ScrollHandler(),
                GetScreenTextHandler(),
                ScreenshotHandler(),
                DeviceStateHandler(this@MCPApplication),
                ListAppsHandler(this@MCPApplication)
            )
        }

        // WebSocket server
        webSocketServer = WebSocketServer(
            authManager = authManager,
            sessionManager = sessionManager,
            commandRouter = commandRouter,
            port = DEFAULT_PORT
        )

        Log.i(TAG, "MCP Agent initialized. Registered actions: ${commandRouter.supportedActions()}")
    }
}
