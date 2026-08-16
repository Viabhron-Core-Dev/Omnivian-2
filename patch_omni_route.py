import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

old_when = """                            "font" -> com.example.ui.settings.FontSettingsContent()
                            "backup" -> com.example.ui.settings.BackupSettingsContent()"""

new_when = """                            "font" -> com.example.ui.settings.FontSettingsContent()
                            "backup" -> com.example.ui.settings.BackupSettingsContent()
                            "editor" -> com.example.ui.settings.EditorSettingsContent()"""

content = content.replace(old_when, new_when)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
