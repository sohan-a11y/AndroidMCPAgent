package com.android.ai.mcp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ai.mcp.domain.ExecutionState

@Composable
fun CommandScreen(
    uiState: McpUiState,
    onCommandChanged: (String) -> Unit,
    onGeneratePlan: () -> Unit,
    onStopExecution: () -> Unit,
    onDismissError: () -> Unit
) {
    val isRunning = uiState.executionState == ExecutionState.RUNNING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Command", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.commandText,
                    onValueChange = onCommandChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    label = { Text("Describe what to do") }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Provider: ${uiState.settings.selectedProvider.value}")
                Text("Max steps: ${uiState.settings.maxPlanSteps}")

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGeneratePlan,
                    enabled = !uiState.isPlanning && !isRunning
                ) {
                    Text(if (uiState.isPlanning) "Planning..." else "Generate Plan")
                }
            }
        }

        if (uiState.validationErrors.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Plan Validation Errors", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.validationErrors.forEach { error ->
                        Text("- $error")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Execution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("State: ${uiState.executionState.name}")
                if (uiState.executionTotalSteps > 0) {
                    Text("Progress: ${uiState.executionCurrentStep}/${uiState.executionTotalSteps}")
                }
                if (uiState.executionMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.executionMessage)
                }

                if (isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onStopExecution) {
                        Text("Stop Execution")
                    }
                }
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
