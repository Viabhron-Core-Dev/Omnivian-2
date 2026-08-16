import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# Update LazyColumn items
old_lazy = """        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                when (message.role) {
                    MessageRole.USER -> UserMessage(text = message.text)
                    MessageRole.AI -> AiMessage(text = message.text)
                    MessageRole.APP_ACTION -> AppActionMessage(
                        editedFiles = message.editedFiles,
                        appActions = message.appActions,
                        onFileClick = { selectedFile = it }
                    )
                }
            }
        }"""
        
new_lazy = """        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatMessages.size, key = { chatMessages[it].id }) { index ->
                val message = chatMessages[index]
                val isLastMessage = index == chatMessages.lastIndex
                when (message.role) {
                    MessageRole.USER -> UserMessage(text = message.text, isLastMessage = isLastMessage)
                    MessageRole.AI -> AiMessage(text = message.text, isLastMessage = isLastMessage)
                    MessageRole.APP_ACTION -> AppActionMessage(
                        editedFiles = message.editedFiles,
                        appActions = message.appActions,
                        onFileClick = { selectedFile = it },
                        isLastMessage = isLastMessage
                    )
                }
            }
        }"""
        
content = content.replace(old_lazy, new_lazy)

# Update UserMessage
content = content.replace("fun UserMessage(text: String) {", "fun UserMessage(text: String, isLastMessage: Boolean = true) {")
content = content.replace("var expanded by remember { mutableStateOf(true) }", "var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage) }", 1)

# Update AiMessage
content = content.replace("fun AiMessage(text: String) {", "fun AiMessage(text: String, isLastMessage: Boolean = true) {")
content = content.replace("var expanded by remember { mutableStateOf(true) }", "var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage) }", 1)

# Update AppActionMessage
old_app_action = """fun AppActionMessage(
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {}
) {"""
new_app_action = """fun AppActionMessage(
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {},
    isLastMessage: Boolean = true
) {"""
content = content.replace(old_app_action, new_app_action)
content = content.replace("onFileClick = onFileClick\n        )", "onFileClick = onFileClick,\n            isLastMessage = isLastMessage\n        )")

# Update ActionHistoryCard
old_history = """fun ActionHistoryCard(
    modifier: Modifier = Modifier,
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {}
) {"""
new_history = """fun ActionHistoryCard(
    modifier: Modifier = Modifier,
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {},
    isLastMessage: Boolean = true
) {"""
content = content.replace(old_history, new_history)
content = content.replace("var expanded by remember { mutableStateOf(false) }", "var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage && (appActions.isNotEmpty() || editedFiles.isNotEmpty())) }", 1)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
