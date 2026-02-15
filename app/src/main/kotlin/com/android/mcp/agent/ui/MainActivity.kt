package com.android.mcp.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.mcp.agent.MCPApplication

/**
 * Main activity — hosts the Compose navigation and bottom bar.
 *
 * Three tabs:
 * - Dashboard: Server controls, connection status, permission toggles
 * - Pairing: QR code + 6-digit code for connecting AI clients
 * - Logs: Audit log of all MCP commands
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MCPApplication

        setContent {
            MaterialTheme {
                MCPAgentApp(app)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPAgentApp(app: MCPApplication) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: "dashboard"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android MCP Agent") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Text("\u2699\uFE0F") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("\uD83D\uDD17") },
                    label = { Text("Pairing") },
                    selected = currentRoute == "pairing",
                    onClick = {
                        navController.navigate("pairing") {
                            popUpTo("dashboard")
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("\uD83D\uDCCB") },
                    label = { Text("Logs") },
                    selected = currentRoute == "logs",
                    onClick = {
                        navController.navigate("logs") {
                            popUpTo("dashboard")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") { DashboardScreen(app) }
            composable("pairing") { PairingScreen(app) }
            composable("logs") { LogScreen(app) }
        }
    }
}
