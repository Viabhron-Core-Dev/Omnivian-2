import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

start_idx = content.find('@Composable\nfun ActionHistoryCard(')
if start_idx != -1:
    # Just take everything up to start_idx, then append the new ActionHistoryCard and TokenUsageBar
    new_content = content[:start_idx] + """@Composable
fun ActionHistoryCard(
    modifier: Modifier = Modifier,
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Action history",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Action Log (${appActions.size + editedFiles.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (appActions.isNotEmpty()) {
                    appActions.forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (editedFiles.isNotEmpty()) {
                    // Edit section header
                    Text("Files edited:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    
                    editedFiles.forEach { (filePath, success) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clickable { onFileClick(filePath) }
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = if (success) "Success" else "Failed",
                                tint = if (success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = filePath.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokenUsageBar(usedTokens: Int, maxTokens: Int) {
    val progress = usedTokens.toFloat() / maxTokens.toFloat()
    val isWarning = progress > 0.8f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(4.dp),
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$usedTokens / $maxTokens Tokens",
            style = MaterialTheme.typography.labelSmall,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
"""
    with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
        f.write(new_content)
