package com.android.mcp.agent.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.mcp.agent.MCPApplication
import com.android.mcp.agent.accessibility.MCPAccessibilityService
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.service.MCPForegroundService

/**
 * Dashboard screen — main control panel.
 *
 * Shows:
 * - Server status (running/stopped) with start/stop button
 * - Accessibility service status
 * - Connected client info
 * - Permission toggles for each action type
 */
@Composable
fun DashboardScreen(app: MCPApplication) {
    val context = LocalContext.current
    val isServerRunning by app.webSocketServer.isRunning.collectAsState()
    val activeSession by app.sessionManager.activeSession.collectAsState()
    val isAccessibilityEnabled = MCPAccessibilityService.isRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServerRunning)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isServerRunning) "Server Running" else "Server Stopped",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isServerRunning) {
                    Text(
                        text = "Port: ${app.webSocketServer.getPort()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (isServerRunning) {
                            MCPForegroundService.stopService(context)
                        } else {
                            MCPForegroundService.startService(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServerRunning)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isServerRunning) "Stop Server" else "Start Server")
                }
            }
        }

        // Accessibility Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAccessibilityEnabled) "Enabled" else "Not Enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAccessibilityEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                if (!isAccessibilityEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                    ) {
                        Text("Open Accessibility Settings")
                    }
                }
            }
        }

        // Connection Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (activeSession != null) {
                    Text("Client: ${activeSession!!.clientId}")
                    Text("Connected from: ${activeSession!!.remoteAddress}")
                } else {
                    Text(
                        text = "No client connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Permissions Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                PermissionManager.ALL_PERMISSIONS.keys.forEach { permission ->
                    val label = PermissionManager.PERMISSION_LABELS[permission] ?: permission
                    var enabled by remember {
                        mutableStateOf(app.permissionManager.isAllowed(permission))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                app.permissionManager.setPermission(permission, it)
                            }
                        )
                    }
                }
            }
        }
    }
}
