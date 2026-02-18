package com.android.ai.mcp.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    var maxStepsInput by remember(uiState.settings.maxPlanSteps) {
        mutableStateOf(uiState.settings.maxPlanSteps.toString())
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
                    if (uiState.isAccessibilityEnabled) {
                        "Enabled"
                    } else {
                        "Not enabled"
                    },
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

                ProviderOption(
                    label = "OpenRouter",
                    selected = uiState.settings.selectedProvider == AiProvider.OPENROUTER,
                    onClick = { onProviderSelected(AiProvider.OPENROUTER) }
                )
                ProviderOption(
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

                Spacer(modifier = Modifier.height(8.dp))
                Text("Current: ${uiState.settings.maxPlanSteps}")
                Text("Step delay: ${uiState.settings.stepDelayMs}ms")
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
private fun ProviderOption(
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
