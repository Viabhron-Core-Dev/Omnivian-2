import re

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'r') as f:
    content = f.read()

old_launched_effect = """    LaunchedEffect(workspaceId) {
        val db = com.example.engine.db.AppDatabase.getDatabase(context)
        config = db.workspaceConfigDao().getConfig(workspaceId)
    }"""
new_launched_effect = """    LaunchedEffect(workspaceId) {
        val db = com.example.engine.db.AppDatabase.getDatabase(context)
        val existingConfig = db.workspaceConfigDao().getConfig(workspaceId)
        if (existingConfig != null) {
            config = existingConfig
        } else {
            val defaultConfig = com.example.engine.db.WorkspaceConfigEntity(
                workspaceId = workspaceId,
                threadName = com.example.engine.fs.LocalFileManager.getWorkspaceName(workspaceId),
                appType = "Android Full Native",
                model = "Gemini Pro Latest",
                integrations = "Default Skills & Tools",
                instructions = "You are a helpful coding assistant."
            )
            db.workspaceConfigDao().saveConfig(defaultConfig)
            config = defaultConfig
        }
    }"""
content = content.replace(old_launched_effect, new_launched_effect)

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'w') as f:
    f.write(content)
