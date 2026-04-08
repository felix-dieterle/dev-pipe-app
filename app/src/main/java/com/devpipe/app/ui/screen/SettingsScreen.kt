package com.devpipe.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devpipe.app.data.model.DiscoveryStatusResponse
import com.devpipe.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val backendUrl by viewModel.backendUrl.collectAsStateWithLifecycle()
    val phpDiscoveryUrl by viewModel.phpDiscoveryUrl.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var backendUrlInput by remember(backendUrl) { mutableStateOf(backendUrl) }
    var phpUrlInput by remember(phpDiscoveryUrl) { mutableStateOf(phpDiscoveryUrl) }
    var tokenInput by remember { mutableStateOf(viewModel.getToken()) }
    var tokenVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backend URL
            Text("Backend", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = backendUrlInput,
                onValueChange = { backendUrlInput = it },
                label = { Text("Backend URL") },
                placeholder = { Text("http://192.168.1.100:8080/") },
                supportingText = { Text("The base URL of your Dev-Pipe server") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { viewModel.saveBackendUrl(backendUrlInput.trim()) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Backend URL") }

            HorizontalDivider()

            // PHP Discovery
            Text("URL Discovery (PHP)", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = phpUrlInput,
                onValueChange = { phpUrlInput = it },
                label = { Text("PHP Discovery URL") },
                placeholder = { Text("https://example.com/api.php") },
                supportingText = { Text("URL to your api.php endpoint used to auto-discover the backend URL. Leave empty if you set the backend URL manually.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.savePhpDiscoveryUrl(phpUrlInput.trim()) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save URL") }
                OutlinedButton(
                    onClick = { viewModel.discoverUrl() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.discoveryInProgress
                ) {
                    if (uiState.discoveryInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Discover")
                    }
                }
            }
            if (uiState.discoveryError != null) {
                Text(uiState.discoveryError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (uiState.discoverySuccess) {
                Text(
                    "URL updated successfully" + (uiState.discoveredUrl?.let { ": $it" } ?: ""),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                uiState.discoveredUrlUpdated?.let {
                    Text(
                        "URL last set: $it",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.discoveryServerStatus?.let {
                    Text(
                        "Server status: $it",
                        color = if (it == DiscoveryStatusResponse.STATUS_ONLINE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.discoveredIp?.let { ip ->
                    Text(
                        "Public IP: $ip" + (uiState.discoveredIpUpdated?.let { " (updated: $it)" } ?: ""),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            // API Token
            Text("Authentication", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("API Token") },
                supportingText = { Text("Your Dev-Pipe API token. Find it in your server's configuration or admin panel.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            imageVector = if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (tokenVisible) "Hide token" else "Show token"
                        )
                    }
                }
            )
            Button(
                onClick = { viewModel.saveToken(tokenInput.trim()) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Token") }

            HorizontalDivider()

            // Theme
            Text("Theme", style = MaterialTheme.typography.titleSmall)
            listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = theme == value,
                        onClick = { viewModel.saveTheme(value) }
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
