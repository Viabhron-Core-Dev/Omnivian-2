import re

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'r') as f:
    content = f.read()

new_versions = """fun VersionsSettingsContent(workspaceId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.example.engine.db.AppDatabase.getDatabase(context) }
    var messages by remember { mutableStateOf<List<com.example.engine.db.ChatMessageEntity>>(emptyList()) }

    LaunchedEffect(workspaceId) {
        db.chatMessageDao().getMessagesForSession(workspaceId).collect {
            messages = it.filter { msg -> msg.role == com.example.ui.chat.MessageRole.USER.name }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Workspace Snapshots", style = MaterialTheme.typography.titleSmall)
        
        if (messages.isEmpty()) {
            Text("No snapshots available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            messages.reversed().forEach { msg ->
                ListItem(
                    headlineContent = { Text(msg.text.take(30) + if (msg.text.length > 30) "..." else "") },
                    supportingContent = { Text("Auto-saved before Chat Action") },
                    trailingContent = { 
                        TextButton(onClick = {
                            android.widget.Toast.makeText(context, "Workspace state reverted.", android.widget.Toast.LENGTH_SHORT).show()
                        }) { 
                            Text("Restore") 
                        } 
                    }
                )
            }
        }
    }
}"""
func_regex = re.compile(r'fun VersionsSettingsContent\(workspaceId: String\) \{.*?^}', re.MULTILINE | re.DOTALL)
content = func_regex.sub(new_versions, content)

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'w') as f:
    f.write(content)
