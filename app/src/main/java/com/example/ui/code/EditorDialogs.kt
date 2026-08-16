package com.example.ui.code

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun FindReplaceDialog(
    editorState: CodeEditorState,
    onDismiss: () -> Unit
) {
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find & Replace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = { findText = it },
                    label = { Text("Find") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    label = { Text("Replace") },
                    singleLine = true
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = matchCase, onCheckedChange = { matchCase = it })
                    Text("Match Case")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (findText.isNotEmpty()) {
                        val currentText = editorState.content.text
                        val newText = if (matchCase) {
                            currentText.replace(findText, replaceText)
                        } else {
                            currentText.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), replaceText)
                        }
                        editorState.content = editorState.content.copy(text = newText)
                    }
                    onDismiss()
                }
            ) { Text("Replace All") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GoToLineDialog(
    editorState: CodeEditorState,
    onDismiss: () -> Unit
) {
    var lineInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line") },
        text = {
            OutlinedTextField(
                value = lineInput,
                onValueChange = { lineInput = it },
                label = { Text("Line Number") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lineNum = lineInput.toIntOrNull()
                    if (lineNum != null && lineNum > 0) {
                        val lines = editorState.content.text.split("\n")
                        if (lineNum <= lines.size) {
                            var offset = 0
                            for (i in 0 until (lineNum - 1)) {
                                offset += lines[i].length + 1
                            }
                            editorState.content = editorState.content.copy(
                                selection = TextRange(offset)
                            )
                        }
                    }
                    onDismiss()
                }
            ) { Text("Go") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun checkSimpleSyntax(text: String): String {
    val stack = mutableListOf<Pair<Char, Int>>()
    var inString = false
    var stringChar = ' '
    var escapeNext = false

    val lines = text.split("\n")
    fun getLineCol(index: Int): String {
        var len = 0
        for ((i, line) in lines.withIndex()) {
            if (len + line.length + 1 > index) {
                return "Line ${i + 1}, Col ${index - len + 1}"
            }
            len += line.length + 1
        }
        return "Unknown"
    }

    for (i in text.indices) {
        val c = text[i]
        
        if (escapeNext) {
            escapeNext = false
            continue
        }
        
        if (c == '\\') {
            escapeNext = true
            continue
        }

        if (inString) {
            if (c == stringChar) {
                inString = false
            }
            continue
        }

        if (c == '"' || c == '\'') {
            inString = true
            stringChar = c
            continue
        }

        when (c) {
            '(', '{', '[' -> stack.add(Pair(c, i))
            ')' -> {
                if (stack.isEmpty() || stack.last().first != '(') return "Mismatched ')' at ${getLineCol(i)}"
                stack.removeAt(stack.size - 1)
            }
            '}' -> {
                if (stack.isEmpty() || stack.last().first != '{') return "Mismatched '}' at ${getLineCol(i)}"
                stack.removeAt(stack.size - 1)
            }
            ']' -> {
                if (stack.isEmpty() || stack.last().first != '[') return "Mismatched ']' at ${getLineCol(i)}"
                stack.removeAt(stack.size - 1)
            }
        }
    }
    
    if (inString) return "Unclosed string starting with '$stringChar'"
    if (stack.isNotEmpty()) return "Unclosed '${stack.last().first}' at ${getLineCol(stack.last().second)}"

    return "No simple syntax errors found."
}

@Composable
fun SyntaxCheckDialog(
    editorState: CodeEditorState,
    onDismiss: () -> Unit
) {
    val result = remember { checkSimpleSyntax(editorState.content.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Syntax Check") },
        text = { Text(result) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
