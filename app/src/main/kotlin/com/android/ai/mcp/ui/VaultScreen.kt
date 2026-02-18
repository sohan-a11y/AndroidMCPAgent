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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.ai.mcp.storage.automation.CredentialEntryEntity

@Composable
fun VaultScreen(
    uiState: McpUiState,
    onAppPackageChanged: (String) -> Unit,
    onFieldHintChanged: (String) -> Unit,
    onAccountLabelChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSaveCredential: () -> Unit,
    onDeleteCredential: (Long) -> Unit,
    onRequestUnlock: () -> Unit,
    onLockVault: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (uiState.isVaultUnlocked) "Session: unlocked" else "Session: locked",
                    color = if (uiState.isVaultUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRequestUnlock) {
                        Text("Unlock")
                    }
                    OutlinedButton(onClick = onLockVault) {
                        Text("Lock")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Add Credential", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.credentialAppPackageInput,
                    onValueChange = onAppPackageChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("App package (e.g. com.example.app)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.credentialFieldHintInput,
                    onValueChange = onFieldHintChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Field hint (optional)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.credentialAccountLabelInput,
                    onValueChange = onAccountLabelChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account label") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.credentialUsernameInput,
                    onValueChange = onUsernameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username (optional)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.credentialPasswordInput,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onSaveCredential) {
                    Text("Save Credential")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Stored Credentials", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.credentials.isEmpty()) {
            Text("No credentials stored.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.credentials, key = { it.id }) { credential ->
                    CredentialCard(
                        credential = credential,
                        onDelete = { onDeleteCredential(credential.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialCard(
    credential: CredentialEntryEntity,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(credential.accountLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "package=${credential.appPackage}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            if (!credential.fieldHint.isNullOrBlank()) {
                Text("field_hint=${credential.fieldHint}", style = MaterialTheme.typography.bodySmall)
            }
            if (!credential.username.isNullOrBlank()) {
                Text("username=${credential.username}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}
