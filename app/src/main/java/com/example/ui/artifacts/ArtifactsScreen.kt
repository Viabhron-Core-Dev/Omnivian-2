package com.example.ui.artifacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.engine.fs.LocalFileManager
import com.example.ui.chat.ArtifactItem
import com.example.ui.chat.PWAPreviewBottomSheet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsScreen(
    onNavigateBack: () -> Unit
) {
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewTitle by remember { mutableStateOf("Preview") }
    
    // Discover HTML/Web artifacts in current workspace + system artifacts
    val workspaceDir = LocalFileManager.getWorkspaceDir()
    val artifacts = remember(workspaceDir) {
        val list = mutableListOf<ArtifactItem>()
        
        // Scan workspace for html files
        try {
            workspaceDir.walkTopDown().maxDepth(3).forEach { file ->
                if (file.isFile && (file.name.endsWith(".html") || file.name.endsWith(".htm"))) {
                    val relPath = file.relativeTo(workspaceDir).path
                    list.add(
                        ArtifactItem(
                            id = relPath,
                            name = file.nameWithoutExtension.replaceFirstChar { it.uppercase() },
                            description = "Workspace artifact: $relPath",
                            url = "http://127.0.0.1:8080/$relPath"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Add standard defaults if empty
        if (list.isEmpty()) {
            list.addAll(
                listOf(
                    ArtifactItem("1", "Todo App", "A simple Interactive Web Todo App", "http://127.0.0.1:8080/index.html"),
                    ArtifactItem("2", "Weather Dashboard", "Interactive Weather & Chart Dashboard", "http://127.0.0.1:8080/weather.html"),
                    ArtifactItem("3", "Calculator", "Vanilla JS Interactive Calculator", "http://127.0.0.1:8080/calc.html")
                )
            )
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artifacts & Previews") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (artifacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No artifacts found in current workspace.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(artifacts) { artifact ->
                        ListItem(
                            headlineContent = { Text(artifact.name, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text(artifact.description, style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Web,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    previewTitle = artifact.name
                                    previewUrl = artifact.url
                                }) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = "Preview")
                                }
                            },
                            modifier = Modifier.clickable {
                                previewTitle = artifact.name
                                previewUrl = artifact.url
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    previewUrl?.let { url ->
        PWAPreviewBottomSheet(
            url = url,
            title = previewTitle,
            onDismiss = { previewUrl = null }
        )
    }
}
