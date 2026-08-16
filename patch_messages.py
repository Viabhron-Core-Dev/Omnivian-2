import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
"""

if "import androidx.compose.foundation.text.selection.SelectionContainer" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.List", imports)

user_message_old = """@Composable
fun UserMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}"""

user_message_new = """@Composable
fun UserMessage(text: String) {
    var expanded by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = !expanded }.padding(4.dp)
        ) {
            Text("You", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}"""

content = content.replace(user_message_old, user_message_new)


# AiMessage replacement
ai_message_pattern = re.compile(r'(@Composable\nfun AiMessage.*?\}\n\}\n)', re.DOTALL)
# wait, I need to match everything inside AiMessage. Let's just find and replace the whole function.

def get_ai_message_old(text):
    start_idx = text.find('@Composable\nfun AiMessage(text: String) {')
    if start_idx == -1: return None
    brace_count = 0
    in_fn = False
    for i in range(start_idx, len(text)):
        if text[i] == '{':
            if not in_fn:
                in_fn = True
            brace_count += 1
        elif text[i] == '}':
            brace_count -= 1
            if in_fn and brace_count == 0:
                return text[start_idx:i+1]
    return None

ai_message_old = get_ai_message_old(content)

ai_message_new = """@Composable
fun AiMessage(text: String) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(end = 32.dp, start = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = !expanded }.padding(4.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gemini Pro Latest", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Revert",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clickable { }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clickable { 
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Message", text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}"""

if ai_message_old:
    content = content.replace(ai_message_old, ai_message_new)

# ActionHistoryCard
def get_action_card_old(text):
    start_idx = text.find('@Composable\nfun ActionHistoryCard(')
    if start_idx == -1: return None
    brace_count = 0
    in_fn = False
    for i in range(start_idx, len(text)):
        if text[i] == '{':
            if not in_fn:
                in_fn = True
            brace_count += 1
        elif text[i] == '}':
            brace_count -= 1
            if in_fn and brace_count == 0:
                return text[start_idx:i+1]
    return None

action_card_old = get_action_card_old(content)

action_card_new = """@Composable
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
}"""

if action_card_old:
    content = content.replace(action_card_old, action_card_new)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)

print(f"UserMessage replaced: {user_message_old in content}")
print(f"AiMessage replaced: {ai_message_old != None}")
print(f"ActionHistoryCard replaced: {action_card_old != None}")
