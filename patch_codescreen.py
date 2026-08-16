import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

old_call = """                        FileExplorer(
                            onFileClick = { fileNode ->
                                selectedFile = fileNode
                                scope.launch { drawerState.close() }
                            }
                        )"""

new_call = """                        FileExplorer(
                            onFileClick = { fileNode ->
                                selectedFile = fileNode
                                scope.launch { drawerState.close() }
                            },
                            onCloseClick = { scope.launch { drawerState.close() } }
                        )"""

content = content.replace(old_call, new_call)

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)
