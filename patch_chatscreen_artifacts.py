import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# Replace the alert dialog with the bottom sheet
old_alert = '''        if (showArtifactsList) {
            AlertDialog(
                onDismissRequest = { showArtifactsList = false },
                title = { Text("Artifacts in this Chat") },
                text = { Text("List of generated mini webapps / PWAs for this thread will be displayed here.") },
                confirmButton = {
                    TextButton(onClick = { showArtifactsList = false }) {
                        Text("Close")
                    }
                }
            )
        }'''

new_sheets = '''        var selectedArtifact by remember { mutableStateOf<ArtifactItem?>(null) }
        
        if (showArtifactsList) {
            ArtifactsListBottomSheet(
                onDismiss = { showArtifactsList = false },
                onArtifactSelected = { artifact ->
                    selectedArtifact = artifact
                    showArtifactsList = false
                }
            )
        }
        
        selectedArtifact?.let { artifact ->
            PWAPreviewBottomSheet(
                url = artifact.url,
                title = artifact.name,
                onDismiss = { selectedArtifact = null }
            )
        }'''

content = content.replace(old_alert, new_sheets)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)

