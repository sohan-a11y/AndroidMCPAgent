package com.android.ai.mcp.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {

    private val viewModel: McpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AndroidAiMcpApp(viewModel = viewModel, hostActivity = this)
            }
        }
    }
}

private object Routes {
    const val Setup = "setup"
    const val Command = "command"
    const val Templates = "templates"
    const val Vault = "vault"
    const val Preview = "preview"
    const val Logs = "logs"

    val BottomTabs = listOf(Setup, Command, Templates, Vault, Logs)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AndroidAiMcpApp(
    viewModel: McpViewModel,
    hostActivity: MainActivity
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.Setup

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.showError("Microphone permission denied")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                UiEvent.NavigateToPreview -> navController.navigate(Routes.Preview)
                UiEvent.RequestBiometricUnlock -> {
                    showBiometricPrompt(
                        activity = hostActivity,
                        onSuccess = { viewModel.onVaultUnlockAuthenticated() },
                        onFailure = { message ->
                            if (!message.isNullOrBlank()) {
                                viewModel.showError(message)
                            }
                        }
                    )
                }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BottomTabItem(
                            route = Routes.Setup,
                            label = "Setup",
                            currentRoute = currentRoute,
                            navController = navController
                        )
                        BottomTabItem(
                            route = Routes.Command,
                            label = "Command",
                            currentRoute = currentRoute,
                            navController = navController
                        )
                        BottomTabItem(
                            route = Routes.Templates,
                            label = "Templates",
                            currentRoute = currentRoute,
                            navController = navController
                        )
                        BottomTabItem(
                            route = Routes.Vault,
                            label = "Vault",
                            currentRoute = currentRoute,
                            navController = navController
                        )
                        BottomTabItem(
                            route = Routes.Logs,
                            label = "Logs",
                            currentRoute = currentRoute,
                            navController = navController
                        )
                    }
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
                    onOpenRouterModelInputChanged = viewModel::onOpenRouterModelInputChanged,
                    onNvidiaModelInputChanged = viewModel::onNvidiaModelInputChanged,
                    onRefreshOpenRouterModels = viewModel::refreshOpenRouterFreeModels,
                    onWakeWordChanged = viewModel::onWakeWordChanged,
                    onWakeEnabledChanged = viewModel::onWakeEnabledChanged,
                    onWakeScopeChanged = viewModel::onWakeScopeChanged,
                    onStartVoiceListeningNow = viewModel::startVoiceListeningNow,
                    onStopVoiceListeningNow = viewModel::stopVoiceListeningNow,
                    onVaultTimeoutChanged = viewModel::onVaultSessionTimeoutChanged,
                    onRequestMicrophonePermission = {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onRequestVaultUnlock = viewModel::requestVaultUnlock,
                    onLockVault = viewModel::lockVault,
                    onDismissError = viewModel::dismissError
                )
            }

            composable(Routes.Command) {
                CommandScreen(
                    uiState = uiState,
                    onCommandChanged = viewModel::onCommandTextChanged,
                    onGeneratePlan = viewModel::generatePlan,
                    onStopExecution = viewModel::requestStopExecution,
                    onResumeExecution = viewModel::confirmAndExecutePlan,
                    onApproveCredentialFill = viewModel::approveCredentialFill,
                    onRejectCredentialFill = viewModel::rejectCredentialFill,
                    onDismissError = viewModel::dismissError
                )
            }

            composable(Routes.Templates) {
                TemplatesScreen(
                    uiState = uiState,
                    onTemplateNameChanged = viewModel::onTemplateNameChanged,
                    onSaveCurrentCommandAsTemplate = viewModel::saveCurrentCommandAsTemplate,
                    onRunTemplate = viewModel::runTemplate,
                    onDeleteTemplate = viewModel::deleteTemplate
                )
            }

            composable(Routes.Vault) {
                VaultScreen(
                    uiState = uiState,
                    onAppPackageChanged = viewModel::onCredentialAppPackageChanged,
                    onFieldHintChanged = viewModel::onCredentialFieldHintChanged,
                    onAccountLabelChanged = viewModel::onCredentialAccountLabelChanged,
                    onUsernameChanged = viewModel::onCredentialUsernameChanged,
                    onPasswordChanged = viewModel::onCredentialPasswordChanged,
                    onSaveCredential = viewModel::saveCredential,
                    onDeleteCredential = viewModel::deleteCredential,
                    onRequestUnlock = viewModel::requestVaultUnlock,
                    onLockVault = viewModel::lockVault
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

@Composable
private fun BottomTabItem(
    route: String,
    label: String,
    currentRoute: String,
    navController: androidx.navigation.NavHostController
) {
    val selected = currentRoute == route
    TextButton(
        onClick = {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(Routes.Setup) { saveState = true }
            }
        }
    ) {
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun showBiometricPrompt(
    activity: MainActivity,
    onSuccess: () -> Unit,
    onFailure: (String?) -> Unit
) {
    val biometricManager = BiometricManager.from(activity)
    val canAuthenticate = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )
    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        onFailure("Biometric authentication not available on this device")
        return
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Credential Vault")
        .setSubtitle("Authenticate to unlock saved credentials")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailure("Authentication failed")
            }
        }
    )
    prompt.authenticate(promptInfo)
}
