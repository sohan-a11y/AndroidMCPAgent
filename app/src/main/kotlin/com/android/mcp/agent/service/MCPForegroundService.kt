package com.android.mcp.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.android.mcp.agent.MCPApplication
import com.android.mcp.agent.R
import com.android.mcp.agent.ui.MainActivity

/**
 * Foreground service that keeps the WebSocket server alive.
 *
 * Android requires a foreground service with a persistent notification
 * for long-running background tasks. This service:
 * - Starts/stops the WebSocket server
 * - Shows a persistent notification with server status
 * - Provides a way to return to the app from the notification
 */
class MCPForegroundService : Service() {

    companion object {
        private const val TAG = "MCPForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mcp_agent_service"

        const val ACTION_START = "com.android.mcp.agent.START_SERVER"
        const val ACTION_STOP = "com.android.mcp.agent.STOP_SERVER"

        fun startService(context: Context) {
            val intent = Intent(context, MCPForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MCPForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "Starting MCP server via foreground service")
                startForeground(NOTIFICATION_ID, createNotification())
                val app = application as MCPApplication
                app.webSocketServer.start()
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping MCP server")
                val app = application as MCPApplication
                app.webSocketServer.stop()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        val app = application as MCPApplication
        app.webSocketServer.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val app = application as MCPApplication
        val port = app.webSocketServer.getPort()

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, port))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
