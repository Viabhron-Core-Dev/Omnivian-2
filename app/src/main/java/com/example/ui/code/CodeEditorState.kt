package com.example.ui.code

import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import java.io.File
import com.example.engine.fs.LocalFileManager
import com.example.engine.fs.FileHistoryEngine

class CodeEditorState {
    var content by mutableStateOf(TextFieldValue(""))
    var error by mutableStateOf<String?>(null)
    var isLiveGeneration by mutableStateOf(false)
    var isLineWrapEnabled by mutableStateOf(false)
    var currentFile by mutableStateOf<File?>(null)

    suspend fun loadFile(file: File) {
        currentFile = file
        val result = LocalFileManager.readFileString(file)
        if (result.isSuccess) {
            content = TextFieldValue(result.getOrNull() ?: "")
            error = null
        } else {
            error = result.exceptionOrNull()?.message ?: "Failed to read file"
        }
    }

    fun saveFile() {
        val file = currentFile ?: return
        FileHistoryEngine.saveRevision(file) // save to history before overwrite
        file.writeText(content.text)
    }
}

@Composable
fun rememberCodeEditorState(): CodeEditorState {
    return remember { CodeEditorState() }
}
