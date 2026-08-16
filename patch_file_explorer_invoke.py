import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

content = content.replace('kotlinx.coroutines.Dispatchers.IO.invoke {', 'kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {')

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)

