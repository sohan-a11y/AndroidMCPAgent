package com.android.ai.mcp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: McpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AndroidAiMcpApp(viewModel)
            }
        }
    }
}

private object Routes {
    const val Setup = "setup"
    const val Command = "command"
    const val Preview = "preview"
    const val Logs = "logs"

    val BottomTabs = listOf(Setup, Command, Logs)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AndroidAiMcpApp(viewModel: McpViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.Setup

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                UiEvent.NavigateToPreview -> navController.navigate(Routes.Preview)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android AI MCP") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (currentRoute in Routes.BottomTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.Setup,
                        onClick = {
                            navController.navigate(Routes.Setup) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.Setup) { saveState = true }
                            }
                        },
                        label = { Text("Setup") },
                        icon = { Text("S") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.Command,
                        onClick = {
                            navController.navigate(Routes.Command) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.Setup) { saveState = true }
                            }
                        },
                        label = { Text("Command") },
                        icon = { Text("C") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.Logs,
                        onClick = {
                            navController.navigate(Routes.Logs) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.Setup) { saveState = true }
                            }
                        },
                        label = { Text("Logs") },
                        icon = { Text("L") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Setup,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.Setup) {
                SetupScreen(
                    uiState = uiState,
                    onRefreshAccessibility = viewModel::refreshAccessibilityStatus,
                    onProviderSelected = viewModel::onProviderSelected,
                    onOpenRouterKeyChanged = viewModel::onOpenRouterKeyInputChanged,
                    onNvidiaKeyChanged = viewModel::onNvidiaKeyInputChanged,
                    onSaveKeys = viewModel::saveApiKeys,
                    onIncrementMaxSteps = viewModel::incrementMaxSteps,
                    onDecrementMaxSteps = viewModel::decrementMaxSteps,
                    onMaxStepsInputChanged = viewModel::onMaxStepsInputChanged,
                    onDismissError = viewModel::dismissError
                )
            }

            composable(Routes.Command) {
                CommandScreen(
                    uiState = uiState,
                    onCommandChanged = viewModel::onCommandTextChanged,
                    onGeneratePlan = viewModel::generatePlan,
                    onStopExecution = viewModel::requestStopExecution,
                    onDismissError = viewModel::dismissError
                )
            }

            composable(Routes.Preview) {
                PreviewScreen(
                    uiState = uiState,
                    onConfirm = {
                        viewModel.confirmAndExecutePlan()
                        navController.navigate(Routes.Command) {
                            popUpTo(Routes.Command) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onCancel = {
                        viewModel.cancelPreview()
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.Logs) {
                LogsScreen(
                    uiState = uiState,
                    onClearLogs = viewModel::clearLogs
                )
            }
        }
    }
}
