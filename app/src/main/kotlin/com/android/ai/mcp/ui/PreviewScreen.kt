package com.android.ai.mcp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.execution.ActionValidator

@Composable
fun PreviewScreen(
    uiState: McpUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val plan = uiState.pendingPlan

    if (plan == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("No pending plan. Generate a plan from the Command tab.")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onCancel) {
                Text("Back")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val provider = uiState.settings.selectedProvider
        val modelId = when (provider) {
            AiProvider.OPENROUTER -> uiState.settings.openRouterModelId
            AiProvider.NVIDIA -> uiState.settings.nvidiaModelId
        }
        val maxSteps = uiState.pendingMaxPlanSteps ?: uiState.settings.maxPlanSteps
        Text("Action Preview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Provider: ${provider.value}")
        Text("Model: $modelId")
        Text("Source: ${uiState.pendingCommandSource.value}")
        Text("Steps: ${plan.steps.size} / Max: $maxSteps")
        Text(
            "Review all actions before execution. Confirmation is mandatory.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(plan.steps) { index, step ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${index + 1}. ${step.action}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (step.params.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (step.action == ActionValidator.ACTION_FILL_SAVED_PASSWORD) {
                                    "Saved credential fill (masked)"
                                } else {
                                    step.params.toString()
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text("Confirm And Execute")
            }
        }
    }
}
