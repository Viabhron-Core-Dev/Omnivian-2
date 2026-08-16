package com.example.ui.code

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun TextViewer(
    file: File,
    editorState: CodeEditorState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(file) {
        editorState.loadFile(file)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (editorState.error != null) {
            Text(text = "Error: ${editorState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        } else if (editorState.currentFile == file) {
            val scrollState = rememberScrollState()
            val hScrollState = rememberScrollState()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .then(
                        if (!editorState.isLineWrapEnabled) Modifier.horizontalScroll(hScrollState) 
                        else Modifier
                    )
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = editorState.content,
                    onValueChange = { 
                        if (!editorState.isLiveGeneration) {
                            editorState.content = it 
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    readOnly = editorState.isLiveGeneration,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = if (editorState.isLineWrapEnabled) Modifier.fillMaxWidth() else Modifier
                )
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
