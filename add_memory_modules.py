import re

# 1. Update GlobalSettingsScreen.kt
path1 = 'app/src/main/java/com/example/ui/settings/GlobalSettingsScreen.kt'
with open(path1, 'r') as f:
    content1 = f.read()

replacement1 = """        SettingsItem("MCP", "Manage Model Context Protocol connections", Icons.Default.Extension, "mcp"),
        SettingsItem("Plugins", "Manage combinations of skills, tools, and MCPs", Icons.Default.Dashboard, "plugins"),
        SettingsItem("Memory Modules", "Manage agent memory types and architectures", Icons.Default.Memory, "memory_modules")"""
content1 = content1.replace('        SettingsItem("MCP", "Manage Model Context Protocol connections", Icons.Default.Extension, "mcp"),\n        SettingsItem("Plugins", "Manage combinations of skills, tools, and MCPs", Icons.Default.Dashboard, "plugins")', replacement1)

with open(path1, 'w') as f:
    f.write(content1)

# 2. Update OmniRootApp.kt
path2 = 'app/src/main/java/com/example/ui/OmniRootApp.kt'
with open(path2, 'r') as f:
    content2 = f.read()

replacement2 = """        composable("plugins") { PlaceholderScreen("Plugins", navController) }
        composable("memory_modules") { PlaceholderScreen("Memory Modules", navController) }"""
content2 = content2.replace('        composable("plugins") { PlaceholderScreen("Plugins", navController) }', replacement2)

with open(path2, 'w') as f:
    f.write(content2)
    
print("Updated successfully")
