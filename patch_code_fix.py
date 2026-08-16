import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

bad_block = """                                showRevertDialog = false
                                Toast.makeText(context, "Reverted to ${revisionFile.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    """

content = content.replace(bad_block, "")

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)
