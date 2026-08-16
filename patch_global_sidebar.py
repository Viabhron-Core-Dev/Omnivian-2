import re

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'r') as f:
    content = f.read()

# Add showLibrary state
content = content.replace("    var showArtifactsList by remember { mutableStateOf(false) }", "    var showArtifactsList by remember { mutableStateOf(false) }\n    var showLibrary by remember { mutableStateOf(false) }")

# Update Library NavigationDrawerItem
old_library = """                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    label = { Text("Library") },
                    selected = false,
                    onClick = { 
                        android.widget.Toast.makeText(context, "Component Library coming soon", android.widget.Toast.LENGTH_SHORT).show()
                        onClose()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )"""

new_library = """                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    label = { Text("Library") },
                    selected = false,
                    onClick = { 
                        showLibrary = true
                        onClose()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )"""

content = content.replace(old_library, new_library)

# Render LibraryBottomSheet
render_lib = """        if (showArtifactsList) {
            com.example.ui.artifacts.ArtifactsListBottomSheet(
                onDismiss = { showArtifactsList = false }
            )
        }
        
        if (showLibrary) {
            com.example.ui.library.LibraryBottomSheet(
                onDismiss = { showLibrary = false }
            )
        }"""
content = content.replace("""        if (showArtifactsList) {
            com.example.ui.artifacts.ArtifactsListBottomSheet(
                onDismiss = { showArtifactsList = false }
            )
        }""", render_lib)

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'w') as f:
    f.write(content)
