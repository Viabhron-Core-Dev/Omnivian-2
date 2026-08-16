import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

# Replace onFileClick
old_click = """                        FileExplorer(
                            onFileClick = { fileNode ->
                                selectedFile = fileNode
                            }
                        )"""

new_click = """                        FileExplorer(
                            onFileClick = { fileNode ->
                                selectedFile = fileNode
                                scope.launch { drawerState.close() }
                            }
                        )"""

content = content.replace(old_click, new_click)

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)
