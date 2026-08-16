package com.example.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ArtifactItem(val id: String, val name: String, val description: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsListBottomSheet(
    onDismiss: () -> Unit,
    onArtifactSelected: (ArtifactItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Mock artifacts for now. Later these would be fetched from the local workspace.
    val artifacts = remember {
        listOf(
            ArtifactItem("1", "Todo App", "A simple React Todo App", "http://localhost:8081/artifacts/todo/index.html"),
            ArtifactItem("2", "Weather Dashboard", "Vue.js weather dashboard", "http://localhost:8081/artifacts/weather/index.html"),
            ArtifactItem("3", "Calculator", "Vanilla JS Calculator", "http://localhost:8081/artifacts/calc/index.html")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                "Thread Artifacts", 
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()
            LazyColumn {
                items(artifacts) { artifact ->
                    ListItem(
                        headlineContent = { Text(artifact.name) },
                        supportingContent = { Text(artifact.description) },
                        leadingContent = { 
                            Icon(Icons.Default.Web, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                        },
                        modifier = Modifier.clickable { onArtifactSelected(artifact) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
