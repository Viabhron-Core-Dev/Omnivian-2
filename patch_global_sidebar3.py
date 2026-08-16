with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'r') as f:
    content = f.read()

replacement = """                    onDelete = {
                        LocalFileManager.deleteWorkspace(workspace.name)
                        val remaining = LocalFileManager.getWorkspaces()
                        workspaces = remaining
                        if (workspace.name == currentChatId) {
                            if (remaining.isNotEmpty()) {
                                onChatSelected(remaining.first().name)
                            } else {
                                onNewChat()
                            }
                        }
                    }"""

content = content.replace(
"""                    onDelete = {
                        LocalFileManager.deleteWorkspace(workspace.name)
                        workspaces = LocalFileManager.getWorkspaces()
                    }""", replacement
)

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'w') as f:
    f.write(content)

