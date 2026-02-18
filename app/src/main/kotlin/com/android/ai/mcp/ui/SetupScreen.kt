package com.android.ai.mcp.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.WakeScope

@Composable
fun SetupScreen(
    uiState: McpUiState,
    onRefreshAccessibility: () -> Unit,
    onProviderSelected: (AiProvider) -> Unit,
    onOpenRouterKeyChanged: (String) -> Unit,
    onNvidiaKeyChanged: (String) -> Unit,
    onSaveKeys: () -> Unit,
    onIncrementMaxSteps: () -> Unit,
    onDecrementMaxSteps: () -> Unit,
    onMaxStepsInputChanged: (String) -> Unit,
    onOpenRouterModelInputChanged: (String) -> Unit,
    onNvidiaModelInputChanged: (String) -> Unit,
    onRefreshOpenRouterModels: () -> Unit,
    onWakeWordChanged: (String) -> Unit,
    onWakeEnabledChanged: (Boolean) -> Unit,
    onWakeScopeChanged: (WakeScope) -> Unit,
    onStartVoiceListeningNow: () -> Unit,
    onStopVoiceListeningNow: () -> Unit,
    onVaultTimeoutChanged: (String) -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onRequestVaultUnlock: () -> Unit,
    onLockVault: () -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    var maxStepsInput by remember(uiState.settings.maxPlanSteps) {
        mutableStateOf(uiState.settings.maxPlanSteps.toString())
    }
    var modelSearch by remember { mutableStateOf("") }
    var vaultTimeoutInput by remember(uiState.settings.vaultSessionTimeoutMinutes) {
        mutableStateOf(uiState.settings.vaultSessionTimeoutMinutes.toString())
    }

    LaunchedEffect(Unit) {
        onRefreshAccessibility()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (uiState.isAccessibilityEnabled) "Enabled" else "Not enabled",
                    color = if (uiState.isAccessibilityEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                RadioOption(
                    label = "OpenRouter",
                    selected = uiState.settings.selectedProvider == AiProvider.OPENROUTER,
                    onClick = { onProviderSelected(AiProvider.OPENROUTER) }
                )
                RadioOption(
                    label = "NVIDIA",
                    selected = uiState.settings.selectedProvider == AiProvider.NVIDIA,
                    onClick = { onProviderSelected(AiProvider.NVIDIA) }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("API Keys", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.openRouterKeyInput,
                    onValueChange = onOpenRouterKeyChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        val suffix = if (uiState.hasOpenRouterKey) " (saved)" else ""
                        Text("OpenRouter API Key$suffix")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.nvidiaKeyInput,
                    onValueChange = onNvidiaKeyChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        val suffix = if (uiState.hasNvidiaKey) " (saved)" else ""
                        Text("NVIDIA API Key$suffix")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onSaveKeys) {
                    Text("Save Keys")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Model Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.settings.openRouterModelId,
                    onValueChange = onOpenRouterModelInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenRouter Model ID (free only)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.settings.nvidiaModelId,
                    onValueChange = onNvidiaModelInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("NVIDIA Model ID") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRefreshOpenRouterModels,
                        enabled = !uiState.isRefreshingOpenRouterModels
                    ) {
                        Text(if (uiState.isRefreshingOpenRouterModels) "Refreshing..." else "Refresh Free Models")
                    }
                    Text("Count: ${uiState.openRouterFreeModels.size}")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelSearch,
                    onValueChange = { modelSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search free models") },
                    singleLine = true
                )

                val filteredModels = uiState.openRouterFreeModels
                    .filter { model ->
                        modelSearch.isBlank() ||
                            model.id.contains(modelSearch, ignoreCase = true) ||
                            model.name.contains(modelSearch, ignoreCase = true)
                    }
                    .take(20)

                if (filteredModels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    filteredModels.forEach { model ->
                        OutlinedButton(
                            onClick = { onOpenRouterModelInputChanged(model.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${model.name} (${model.id})")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (!uiState.modelValidationMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        uiState.modelValidationMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Voice Wake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wake Enabled")
                    Switch(
                        checked = uiState.settings.wakeEnabled,
                        onCheckedChange = onWakeEnabledChanged
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.isMicrophonePermissionGranted) {
                        "Microphone permission: Granted"
                    } else {
                        "Microphone permission: Not granted"
                    },
                    color = if (uiState.isMicrophonePermissionGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.settings.wakeWord,
                    onValueChange = onWakeWordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Wake Word") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Wake Scope", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                RadioOption(
                    label = "Always-on foreground service",
                    selected = uiState.settings.wakeScope == WakeScope.ALWAYS_ON_FOREGROUND,
                    onClick = { onWakeScopeChanged(WakeScope.ALWAYS_ON_FOREGROUND) }
                )
                RadioOption(
                    label = "Only while app open",
                    selected = uiState.settings.wakeScope == WakeScope.APP_OPEN_ONLY,
                    onClick = { onWakeScopeChanged(WakeScope.APP_OPEN_ONLY) }
                )
                RadioOption(
                    label = "Manual start only",
                    selected = uiState.settings.wakeScope == WakeScope.MANUAL_START,
                    onClick = { onWakeScopeChanged(WakeScope.MANUAL_START) }
                )

                if (uiState.settings.wakeScope == WakeScope.MANUAL_START) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onStartVoiceListeningNow) {
                            Text("Start Listening")
                        }
                        OutlinedButton(onClick = onStopVoiceListeningNow) {
                            Text("Stop Listening")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onRequestMicrophonePermission) {
                    Text("Grant Microphone Permission")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Vault Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (uiState.isVaultUnlocked) "Vault status: Unlocked" else "Vault status: Locked",
                    color = if (uiState.isVaultUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = vaultTimeoutInput,
                    onValueChange = {
                        vaultTimeoutInput = it
                        onVaultTimeoutChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Session timeout minutes (1-30)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRequestVaultUnlock) {
                        Text("Unlock With Biometrics")
                    }
                    OutlinedButton(onClick = onLockVault) {
                        Text("Lock Now")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Max Steps Per Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
                        onDecrementMaxSteps()
                        maxStepsInput = (uiState.settings.maxPlanSteps - 1).toString()
                    }) {
                        Text("-")
                    }

                    OutlinedTextField(
                        value = maxStepsInput,
                        onValueChange = { value ->
                            maxStepsInput = value
                            onMaxStepsInputChanged(value)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("1-50") },
                        singleLine = true
                    )

                    OutlinedButton(onClick = {
                        onIncrementMaxSteps()
                        maxStepsInput = (uiState.settings.maxPlanSteps + 1).toString()
                    }) {
                        Text("+")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Applies to plan generation and validation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Text(uiState.errorMessage!!)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onDismissError) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}
