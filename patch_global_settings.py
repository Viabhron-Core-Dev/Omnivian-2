import re

with open('app/src/main/java/com/example/ui/settings/GlobalSettingsScreen.kt', 'r') as f:
    content = f.read()

# Add Encrypted Backup and Restore
backup_item = """                SettingsItem("Font & Typography", "App-wide font settings", Icons.Default.FontDownload) { onNavigateTo("settings/font") }
                SettingsItem("Encrypted Backup", "Backup and restore your data securely", Icons.Default.Lock) { onNavigateTo("settings/backup") }"""
content = content.replace('                SettingsItem("Font & Typography", "App-wide font settings", Icons.Default.FontDownload) { onNavigateTo("settings/font") }', backup_item)

with open('app/src/main/java/com/example/ui/settings/GlobalSettingsScreen.kt', 'w') as f:
    f.write(content)
