import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

# Add showNewChatDialog state
old_vars = """    val navController = rememberNavController()

    ModalNavigationDrawer("""
new_vars = """    val navController = rememberNavController()
    var showNewChatDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer("""
content = content.replace(old_vars, new_vars)

# Update onNewChat
old_new_chat = """                onNewChat = { 
                    val newSessionId = java.util.UUID.randomUUID().toString()
                    val count = com.example.engine.fs.LocalFileManager.getWorkspaces().size
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newSessionId, "Chat ${count + 1}")
                    chatSessionId = newSessionId 
                },"""
new_new_chat = """                onNewChat = { 
                    showNewChatDialog = true
                },"""
content = content.replace(old_new_chat, new_new_chat)

# Add NewChatDialog UI element
old_nav_host = """        }
    ) {
        NavHost(navController = navController, startDestination = "main") {"""
new_nav_host = """        }
    ) {
        if (showNewChatDialog) {
            com.example.ui.chat.NewChatDialog(
                onDismiss = { showNewChatDialog = false },
                onCreate = { threadName, appType, model, integrations, instructions ->
                    val newSessionId = java.util.UUID.randomUUID().toString()
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newSessionId, threadName)
                    chatSessionId = newSessionId
                    showNewChatDialog = false
                    scope.launch { drawerState.close() }
                }
            )
        }
        
        NavHost(navController = navController, startDestination = "main") {"""
content = content.replace(old_nav_host, new_nav_host)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
