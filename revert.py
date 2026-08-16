import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# Revert LazyColumn items
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
        
content = content.replace(new_lazy, old_lazy)

content = content.replace("fun UserMessage(text: String, isLastMessage: Boolean = true) {", "fun UserMessage(text: String) {")
content = content.replace("var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage) }", "var expanded by remember { mutableStateOf(true) }", 1)

content = content.replace("fun AiMessage(text: String, isLastMessage: Boolean = true) {", "fun AiMessage(text: String) {")
content = content.replace("var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage) }", "var expanded by remember { mutableStateOf(true) }", 1)

old_app_action = """fun AppActionMessage(
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {},
    isLastMessage: Boolean = true
) {"""
new_app_action = """fun AppActionMessage(
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {}
) {"""
content = content.replace(old_app_action, new_app_action)
content = content.replace("onFileClick = onFileClick,\n            isLastMessage = isLastMessage\n        )", "onFileClick = onFileClick\n        )")

old_history = """fun ActionHistoryCard(
    modifier: Modifier = Modifier,
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {},
    isLastMessage: Boolean = true
) {"""
new_history = """fun ActionHistoryCard(
    modifier: Modifier = Modifier,
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {}
) {"""
content = content.replace(old_history, new_history)
content = content.replace("var expanded by remember(isLastMessage) { mutableStateOf(isLastMessage) }", "var expanded by remember { mutableStateOf(false) }", 1)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
