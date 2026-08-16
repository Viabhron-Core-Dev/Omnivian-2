package com.example.ui.code

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PptViewer(file: File, modifier: Modifier = Modifier) {
    var isExtracting by remember { mutableStateOf(true) }
    
    LaunchedEffect(file) {
        isExtracting = true
        // Simulate extraction delay
        delay(1500)
        isExtracting = false
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (isExtracting) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("Extracting text from PowerPoint...", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "Text Extracted from ${file.name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "[Simulated Extraction]\nSlide 1:\nTitle: Presentation Overview\nContent: This is a placeholder for actual text extraction from PPT files. In a real app, this would use a library like Apache POI to extract slide text.\n\nSlide 2:\nTitle: Key Points\nContent: • Point A\n• Point B\n• Point C",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
