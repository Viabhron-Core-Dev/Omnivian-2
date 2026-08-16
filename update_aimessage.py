import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

replacement_1 = """                    MessageRole.AI -> AiMessage(message = message, isLastMessage = isLastMessage, aiViewModel = aiViewModel)"""
content = re.sub(r'                    MessageRole\.AI -> AiMessage\(text = message\.text, isLastMessage = isLastMessage\)', replacement_1, content)

replacement_2 = """
fun AiMessage(message: ChatMessage, isLastMessage: Boolean = true, aiViewModel: AiManagerViewModel) {
    val context = LocalContext.current
    var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage) }
    var userRating by remember(message.id) { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    
    val displayName = message.modelName ?: "Gemini Pro Latest"
    
    Column(modifier = Modifier.fillMaxWidth().padding(end = 32.dp, start = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = !expanded }.padding(4.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(text = message.text, style = MaterialTheme.typography.bodyLarge)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Copy Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Response", message.text))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                
                // Ratings
                if (message.modelName != null && message.providerId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (userRating != true) {
                                userRating = true
                                aiViewModel.rateModel(message.providerId, message.modelName, true)
                                Toast.makeText(context, "Rated: Upvote", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(androidx.compose.material.icons.filled.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(16.dp), tint = if (userRating == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (userRating != false) {
                                userRating = false
                                aiViewModel.rateModel(message.providerId, message.modelName, false)
                                Toast.makeText(context, "Rated: Downvote", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(androidx.compose.material.icons.filled.ThumbDown, contentDescription = "Downvote", modifier = Modifier.size(16.dp), tint = if (userRating == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
"""
content = re.sub(r'\nfun AiMessage\(text: String, isLastMessage: Boolean = true\) \{.*?\n\}\n', replacement_2, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
