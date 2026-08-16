with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun GlobalSidebar(',
    '@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n@Composable\nfun GlobalSidebar('
)

content = content.replace(
    'onClick = { /* TODO: Artifacts */ onClose() },',
    'onClick = { \n                android.widget.Toast.makeText(context, "Artifacts list is managed in Chat", android.widget.Toast.LENGTH_SHORT).show()\n                onClose()\n            },'
)

content = content.replace(
    'onClick = { /* TODO: Design Studio */ onClose() },',
    'onClick = { \n                android.widget.Toast.makeText(context, "Design Studio coming soon", android.widget.Toast.LENGTH_SHORT).show()\n                onClose()\n            },'
)

content = content.replace(
    'onClick = { /* TODO: Library */ onClose() },',
    'onClick = { \n                android.widget.Toast.makeText(context, "Component Library coming soon", android.widget.Toast.LENGTH_SHORT).show()\n                onClose()\n            },'
)

content = content.replace(
    'onClick = { showMenu = false; /* TODO */ }',
    'onClick = { \n                            showMenu = false\n                            android.widget.Toast.makeText(context, "Archive requires Google Drive integration", android.widget.Toast.LENGTH_SHORT).show()\n                        }'
)

# We need to make sure context is available in GlobalSidebar and ChatSidebarItem
if 'val context =' not in content:
    content = content.replace(
        'var workspaces by remember { mutableStateOf(emptyList<File>()) }',
        'val context = androidx.compose.ui.platform.LocalContext.current\n    var workspaces by remember { mutableStateOf(emptyList<File>()) }'
    )
    
    content = content.replace(
        'var showMenu by remember { mutableStateOf(false) }',
        'val context = androidx.compose.ui.platform.LocalContext.current\n    var showMenu by remember { mutableStateOf(false) }'
    )

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'w') as f:
    f.write(content)

