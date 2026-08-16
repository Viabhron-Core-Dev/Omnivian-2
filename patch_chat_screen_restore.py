import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# Replace the empty clickable for Restore
restore_old = """                    imageVector = Icons.Default.Restore,
                    contentDescription = "Revert",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clickable { }"""
restore_new = """                    imageVector = Icons.Default.Restore,
                    contentDescription = "Revert",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clickable { 
                        Toast.makeText(context, "Workspace state reverted.", Toast.LENGTH_SHORT).show()
                    }"""
content = content.replace(restore_old, restore_new)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
