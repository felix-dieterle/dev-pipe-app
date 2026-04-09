package com.devpipe.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devpipe.app.data.model.Session
import com.devpipe.app.ui.theme.*
import com.devpipe.app.ui.viewmodel.SessionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onSessionClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Create Session")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            state.sessions.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No sessions yet. Create one with the + button.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.sessions) { session ->
                        val sessionId = session.sessionId
                        if (!sessionId.isNullOrBlank()) {
                            SessionCard(session = session, onClick = { onSessionClick(sessionId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status = session.status)
            }
            if (!session.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = session.description.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            session.repo?.let { repo ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${repo.owner}/${repo.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String?) {
    val (backgroundColor, label) = when (status) {
        "planning" -> StatusGray to "Planning"
        "approved" -> StatusBlue to "Approved"
        "implementing" -> StatusOrange to "Implementing"
        "awaiting_approval" -> StatusYellow to "Awaiting Approval"
        "done" -> StatusGreen to "Done"
        "failed" -> StatusRed to "Failed"
        else -> StatusGray to (status ?: "Unknown")
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor
        )
    }
}
