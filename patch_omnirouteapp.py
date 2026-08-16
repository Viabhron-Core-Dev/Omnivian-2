import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

old_route = """            composable("settings/omniroute") {
                com.example.ui.settings.omniroute.AiManagerPanelScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }"""

new_route = """            composable("settings/omniroute") {
                com.example.ui.settings.omniroute.AiManagerPanelScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddKeyClick = { providerId ->
                        navController.navigate("settings/omniroute/add_key/$providerId")
                    }
                )
            }
            composable("settings/omniroute/add_key/{providerId}") { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
                com.example.ui.settings.omniroute.DirectToKeyWebViewScreen(
                    providerId = providerId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }"""

content = content.replace(old_route, new_route)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
