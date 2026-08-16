import re

with open('app/src/main/java/com/example/ui/settings/SettingsPlaceholders.kt', 'r') as f:
    content = f.read()

# Make all divisions folded initially
content = content.replace('isExpanded = true,', 'isExpanded = false,')

# Find ToolsSettingsContent
match = re.search(r'(@Composable\nfun ToolsSettingsContent\(\) \{.*?^})', content, re.MULTILINE | re.DOTALL)
if match:
    tools_content = match.group(1)
    
    # Replace its Column with Box + Column
    tools_content = tools_content.replace(
        'Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {',
        'Box(modifier = Modifier.fillMaxSize()) {\n    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {',
        1
    )
    
    # Add FAB
    tools_content = tools_content.replace(
        '        }\n    }\n}',
        '        }\n    }\n    FloatingActionButton(\n        onClick = { /* TODO */ },\n        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)\n    ) {\n        Icon(Icons.Default.Add, contentDescription = "Create Tool")\n    }\n}\n}',
        1
    )
    
    content = content[:match.start()] + tools_content + content[match.end():]

# Add Icons.Default.Add import if missing
if 'import androidx.compose.material.icons.filled.Add' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Search', 'import androidx.compose.material.icons.filled.Search\nimport androidx.compose.material.icons.filled.Add')

with open('app/src/main/java/com/example/ui/settings/SettingsPlaceholders.kt', 'w') as f:
    f.write(content)
