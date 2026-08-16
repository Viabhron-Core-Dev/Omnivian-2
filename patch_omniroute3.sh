sed -i 's/import com.example.ui.settings.GlobalSettingsScreen/import com.example.ui.settings.GlobalSettingsScreen\nimport com.example.ui.settings.ThreadSettingsScreen/g' app/src/main/java/com/example/ui/OmniRouteApp.kt
sed -i '/onExportClick = {/,/}/c \
                        onExportClick = {\n                            showWorkspaceActions = false\n                            showGithubExport = true\n                        },\n                        onThreadSettingsClick = {\n                            showWorkspaceActions = false\n                            navController.navigate("thread_settings")\n                        }' app/src/main/java/com/example/ui/OmniRouteApp.kt
sed -i '/composable("settings") {/i \
            composable("thread_settings") {\n                ThreadSettingsScreen(\n                    onNavigateBack = { navController.popBackStack() }\n                )\n            }\n' app/src/main/java/com/example/ui/OmniRouteApp.kt
