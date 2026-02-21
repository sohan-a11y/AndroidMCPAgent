package com.android.ai.mcp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ai.mcp.storage.logs.CommandRunEntity
import com.android.ai.mcp.storage.logs.StepExecutionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    uiState: McpUiState,
    onClearLogs: () -> Unit,
    onRunClicked: (Long) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Execution Logs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (uiState.logs.isNotEmpty()) {
                OutlinedButton(onClick = onClearLogs) {
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No runs recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.logs, key = { it.id }) { run ->
                    RunLogCard(run = run, onClick = { onRunClicked(run.id) })
                }
            }
        }
    }
}

@Composable
fun RunDetailScreen(
    uiState: McpUiState,
    onBack: () -> Unit,
    onRerun: (CommandRunEntity) -> Unit
) {
    val runId = uiState.selectedRunId ?: return
    val run = uiState.logs.firstOrNull { it.id == runId } ?: return
    val steps = uiState.selectedRunSteps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            OutlinedButton(onClick = { onRerun(run) }) {
                Text("Re-run")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            run.commandText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        val statusColor = statusColor(run.status)
        Text(
            "Status: ${run.status}",
            color = statusColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Provider: ${run.provider} | Model: ${run.modelId}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Started: ${formatTime(run.startedAt)}${run.endedAt?.let { " | Ended: ${formatTime(it)}" } ?: ""}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
        if (!run.errorMessage.isNullOrBlank()) {
            Text(
                run.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Steps (${steps.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (steps.isEmpty()) {
            Text(
                "No step executions recorded.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(steps, key = { it.id }) { step ->
                    StepDetailCard(step)
                }
            }
        }
    }
}

@Composable
private fun StepDetailCard(step: StepExecutionEntity) {
    val isSuccess = step.status == "success"
    val borderColor = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Step ${step.stepIndex + 1}: ${step.action}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isSuccess) "OK" else "FAIL",
                    color = borderColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                "${step.durationMs}ms",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (step.paramsJson.isNotBlank() && step.paramsJson != "{}") {
                Text(
                    step.paramsJson,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }

            if (!step.errorMessage.isNullOrBlank()) {
                Text(
                    step.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RunLogCard(run: CommandRunEntity, onClick: () -> Unit) {
    val statusColor = statusColor(run.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    run.commandText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(formatTime(run.startedAt), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "status=${run.status}",
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "provider=${run.provider}, model=${run.modelId}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "source=${run.commandSource}, template=${run.templateId ?: "-"}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "steps=${run.planStepCount}, max=${run.maxPlanSteps}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            if (!run.errorMessage.isNullOrBlank()) {
                Text(
                    run.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: String) = when (status) {
    "completed" -> MaterialTheme.colorScheme.primary
    "failed", "validation_failed" -> MaterialTheme.colorScheme.error
    "awaiting_user" -> MaterialTheme.colorScheme.tertiary
    "stopped" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
