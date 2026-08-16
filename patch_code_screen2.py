import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

old_dialog = """                    if (showSyntaxCheckDialog) {
                        SyntaxCheckDialog(editorState = editorState, onDismiss = { showSyntaxCheckDialog = false })
                    }"""

new_dialog = """                    if (showSyntaxCheckDialog) {
                        SyntaxCheckDialog(editorState = editorState, onDismiss = { showSyntaxCheckDialog = false })
                    }
                    
                    if (showSaveAsPRDialog && selectedFile != null) {
                        var prTitle by remember { mutableStateOf("") }
                        var prDesc by remember { mutableStateOf("") }
                        val db = remember { AppDatabase.getDatabase(context) }
                        
                        AlertDialog(
                            onDismissRequest = { showSaveAsPRDialog = false },
                            title = { Text("Save as Pull Request") },
                            text = {
                                Column {
                                    Text("Propose your changes to ${selectedFile?.name}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = prTitle,
                                        onValueChange = { prTitle = it },
                                        label = { Text("PR Title") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = prDesc,
                                        onValueChange = { prDesc = it },
                                        label = { Text("Description") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    scope.launch {
                                        val pr = WorkspacePullRequestEntity(
                                            id = UUID.randomUUID().toString(),
                                            workspaceId = com.example.engine.fs.LocalFileManager.getWorkspaceDir().name,
                                            title = prTitle,
                                            description = prDesc,
                                            targetFile = selectedFile!!.file.absolutePath,
                                            diff = editorState.content.text, // Simply storing current editor state as diff for now
                                            status = "open",
                                            createdAt = System.currentTimeMillis()
                                        )
                                        db.workspacePullRequestDao().savePullRequest(pr)
                                        showSaveAsPRDialog = false
                                        Toast.makeText(context, "Pull Request Created", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("Submit PR")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveAsPRDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }"""

content = content.replace(old_dialog, new_dialog)

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)
