import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

# Update FileExplorer signature
old_sig = """fun FileExplorer(
    onFileClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {"""
new_sig = """fun FileExplorer(
    onFileClick: (FileNode) -> Unit,
    onCloseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {"""
content = content.replace(old_sig, new_sig)

# Update header with close button
old_header = """    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = workspaceName.value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )"""

new_header = """    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workspaceName.value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close Drawer")
            }
        }"""
content = content.replace(old_header, new_header)

# Ensure Icons.Default.Close is imported if needed, but we already have import androidx.compose.material.icons.filled.* 
# so Icons.Default.Close should be available.

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)
