package com.devpipe.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devpipe.app.data.model.Job
import com.devpipe.app.data.model.Step
import com.devpipe.app.ui.theme.*
import com.devpipe.app.ui.viewmodel.SessionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Session") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reject(rejectReason.takeIf { it.isNotBlank() })
                    showRejectDialog = false
                }) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.session?.name ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.session == null -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Session not found", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                val session = state.session!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status + details
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status", style = MaterialTheme.typography.labelMedium)
                                    StatusChip(session.status)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(session.description, style = MaterialTheme.typography.bodyMedium)
                                session.repo?.let { repo ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${repo.owner}/${repo.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                session.prUrl?.let { url ->
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = { uriHandler.openUri(url) }) {
                                        Text("Open Pull Request")
                                    }
                                }
                            }
                        }
                    }

                    // Error messages
                    if (state.actionError != null) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(
                                    "Error: ${state.actionError}",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Action buttons
                    item {
                        if (state.actionInProgress) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (session.status == "planning") {
                                    Button(
                                        onClick = { viewModel.approve() },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Approve") }
                                    OutlinedButton(
                                        onClick = { showRejectDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Reject") }
                                }
                                if (session.status == "awaiting_approval" && !session.prUrl.isNullOrBlank()) {
                                    Button(
                                        onClick = { viewModel.mergePr() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Merge Pull Request") }
                                }
                            }
                        }
                    }

                    // Jobs
                    if (state.jobs.isNotEmpty()) {
                        item {
                            Text("Pipeline Jobs", style = MaterialTheme.typography.titleMedium)
                        }
                        items(state.jobs) { job ->
                            JobCard(job)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobCard(job: Job) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(job.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                JobStatusBadge(job.status)
            }
            if (job.steps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                job.steps.forEach { step ->
                    StepRow(step)
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: Step) {
    val color = when (step.status) {
        "success" -> Color(0xFF4CAF50)
        "failed" -> Color(0xFFF44336)
        "running" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• ", color = color)
        Text(step.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(step.status, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun JobStatusBadge(status: String) {
    val color = when (status) {
        "success" -> Color(0xFF4CAF50)
        "failed" -> Color(0xFFF44336)
        "running" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.2f)) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
