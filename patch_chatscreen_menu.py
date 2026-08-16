import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add showRename state near showMenu
content = content.replace(
    'var showMenu by remember { mutableStateOf(false) }',
    'var showMenu by remember { mutableStateOf(false) }\n                var showRename by remember { mutableStateOf(false) }'
)

# Replace DropdownMenu
old_menu = """                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Chat") },
                        onClick = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive Chat") },
                        onClick = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false }
                    )
                }"""

new_menu = """                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRename = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive (GDrive)") },
                        onClick = { 
                            showMenu = false
                            android.widget.Toast.makeText(context, "Archive requires Google Drive integration", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false }
                    )
                }

                if (showRename) {
                    var newName by remember { mutableStateOf(workspaceName.value) }
                    AlertDialog(
                        onDismissRequest = { showRename = false },
                        title = { Text("Rename Chat") },
                        text = { 
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Chat Name") }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                workspaceName.value = newName
                                showRename = false
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRename = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }"""

content = content.replace(old_menu, new_menu)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
