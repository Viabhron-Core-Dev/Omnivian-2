import os

files_to_fix = [
    'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt',
    'app/src/main/java/com/example/ui/settings/omniroot/TranslatorTab.kt'
]

for filepath in files_to_fix:
    if os.path.exists(filepath):
        with open(filepath, 'r') as f:
            content = f.read()
        
        new_content = content.replace('\\$', '$')
        
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Fixed {filepath}")
    else:
        print(f"File not found: {filepath}")
