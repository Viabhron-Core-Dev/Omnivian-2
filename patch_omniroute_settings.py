import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

settings_subroute = """            composable("settings/{subRoute}") { backStackEntry ->
                val subRoute = backStackEntry.arguments?.getString("subRoute") ?: "Unknown"
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(subRoute.replaceFirstChar { it.uppercase() }) },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when (subRoute) {
                            "skills" -> com.example.ui.settings.SkillsSettingsContent()
                            "tools" -> com.example.ui.settings.ToolsSettingsContent()
                            "mcp" -> com.example.ui.settings.MCPSettingsContent()
                            "plugins" -> com.example.ui.settings.PluginsSettingsContent()
                            "github", "firebase", "gdrive" -> com.example.ui.settings.IntegrationsSettingsContent()
                            "permissions" -> com.example.ui.settings.PermissionsSettingsContent()
                            "font" -> com.example.ui.settings.FontSettingsContent()
                            "backup" -> com.example.ui.settings.BackupSettingsContent()
                            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Settings content for $subRoute (Pending implementation)") }
                        }
                    }
                }
            }"""

regex = re.compile(r'            composable\("settings/\{subRoute\}"\).*?\}\n            \}\n', re.MULTILINE | re.DOTALL)
content = regex.sub(settings_subroute + "\n", content)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
