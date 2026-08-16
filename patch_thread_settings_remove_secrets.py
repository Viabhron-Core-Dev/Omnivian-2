with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('    SECRETS("Secrets"),\n', '')
content = content.replace('                    ThreadSettingTab.SECRETS -> SecretsSettingsContent()\n', '')

import re
content = re.sub(r'@Composable\nfun SecretsSettingsContent\(\) \{.*?\n\}\n', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'w') as f:
    f.write(content)

