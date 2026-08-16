import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

# Fix missing state
if "var showSaveAsPRDialog by remember" not in content:
    content = content.replace(
        "var showSyntaxCheckDialog by remember { mutableStateOf(false) }",
        "var showSyntaxCheckDialog by remember { mutableStateOf(false) }\n    var showSaveAsPRDialog by remember { mutableStateOf(false) }"
    )

if "import com.example.engine.db.AppDatabase" not in content:
    content = content.replace("import java.io.File", "import java.io.File\nimport com.example.engine.db.AppDatabase\nimport com.example.engine.db.WorkspacePullRequestEntity\nimport java.util.UUID")

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)
