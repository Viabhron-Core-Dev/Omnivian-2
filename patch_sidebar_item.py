with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'var chatName by remember { mutableStateOf(workspace.name) } // TODO: Read from metadata in the future',
    'var chatName by remember { mutableStateOf(LocalFileManager.getWorkspaceName(workspace.name)) }'
)

content = content.replace(
"""                TextButton(onClick = { 
                    chatName = newName
                    // In a real implementation we would save this name to a metadata file in the workspace
                    showRename = false 
                }) {""",
"""                TextButton(onClick = { 
                    chatName = newName
                    LocalFileManager.setWorkspaceName(workspace.name, newName)
                    showRename = false 
                }) {"""
)

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'w') as f:
    f.write(content)

