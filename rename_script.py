import os
import glob

def rename_and_replace():
    base_dir = "app/src/main/java/com/example"
    
    # Files to rename
    file_renames = [
        ("engine/omniroute/service/OmniRouteProxyServer.kt", "engine/omniroute/service/OmniRootProxyServer.kt"),
        ("engine/omniroute/service/OmniRouteProxyService.kt", "engine/omniroute/service/OmniRootProxyService.kt"),
        ("ui/chat/OmniRouteClient.kt", "ui/chat/OmniRootClient.kt"),
        ("ui/OmniRouteApp.kt", "ui/OmniRootApp.kt")
    ]
    
    for old, new in file_renames:
        old_path = os.path.join(base_dir, old)
        new_path = os.path.join(base_dir, new)
        if os.path.exists(old_path):
            os.rename(old_path, new_path)
    
    # Directories to rename
    dir_renames = [
        ("engine/omniroute", "engine/omniroot"),
        ("ui/settings/omniroute", "ui/settings/omniroot")
    ]
    
    for old, new in dir_renames:
        old_path = os.path.join(base_dir, old)
        new_path = os.path.join(base_dir, new)
        if os.path.exists(old_path):
            os.rename(old_path, new_path)

    # Search and replace in all Kotlin files
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith(".kt"):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content.replace("omniroute", "omniroot").replace("OmniRoute", "OmniRoot")
                
                if content != new_content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)

rename_and_replace()
print("Renaming and text replacement complete.")
