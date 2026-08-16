import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

old_on_create = """                onCreate = { threadName, appType, model, integrations, instructions ->
                    val newSessionId = java.util.UUID.randomUUID().toString()
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newSessionId, threadName)
                    chatSessionId = newSessionId
                    showNewChatDialog = false
                    scope.launch { drawerState.close() }
                }"""
                
new_on_create = """                onCreate = { threadName, appType, model, integrations, instructions ->
                    val newSessionId = java.util.UUID.randomUUID().toString()
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newSessionId, threadName)
                    scope.launch {
                        val db = com.example.engine.db.AppDatabase.getDatabase(context)
                        db.workspaceConfigDao().saveConfig(
                            com.example.engine.db.WorkspaceConfigEntity(
                                workspaceId = newSessionId,
                                threadName = threadName,
                                appType = appType,
                                model = model,
                                integrations = integrations,
                                instructions = instructions
                            )
                        )
                    }
                    chatSessionId = newSessionId
                    showNewChatDialog = false
                    scope.launch { drawerState.close() }
                }"""
content = content.replace(old_on_create, new_on_create)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
