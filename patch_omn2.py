import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

old_thread = """                        composable("thread_settings") {
                ThreadSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }"""
new_thread = """                        composable("thread_settings") {
                ThreadSettingsScreen(
                    workspaceId = chatSessionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }"""
content = content.replace(old_thread, new_thread)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
