package com.android.ai.mcp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ai.mcp.storage.automation.TaskTemplateEntity

@Composable
fun TemplatesScreen(
    uiState: McpUiState,
    onTemplateNameChanged: (String) -> Unit,
    onSaveCurrentCommandAsTemplate: () -> Unit,
    onRunTemplate: (TaskTemplateEntity) -> Unit,
    onDeleteTemplate: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Save Current Command", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.newTemplateName,
                    onValueChange = onTemplateNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Template Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onSaveCurrentCommandAsTemplate) {
                    Text("Save Template")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Templates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.templates.isEmpty()) {
            Text("No templates yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onRun = { onRunTemplate(template) },
                        onDelete = { onDeleteTemplate(template.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: TaskTemplateEntity,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                template.commandText,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "provider=${template.provider}, model=${template.modelId}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "max=${template.maxPlanSteps}, delay=${template.stepDelayMs}ms",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRun) {
                    Text("Run")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}
