import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# 1. Add sessionId to signature
if 'fun ChatScreen(\n    onMenuClick: () -> Unit\n)' in content:
    content = content.replace('fun ChatScreen(\n    onMenuClick: () -> Unit\n)', 'fun ChatScreen(\n    sessionId: String,\n    onMenuClick: () -> Unit\n)')

# 2. Add imports
imports = """import kotlinx.coroutines.flow.first
import com.example.engine.db.AppDatabase
import com.example.engine.db.toEntity
import com.example.engine.db.toDomainModel
"""
content = content.replace('import kotlinx.coroutines.launch', imports + 'import kotlinx.coroutines.launch')

# 3. Add LaunchedEffect to load messages
launched_effect = """
    val db = AppDatabase.getDatabase(context)
    val dao = db.chatMessageDao()
    
    LaunchedEffect(sessionId) {
        val initialMessages = dao.getMessagesForSession(sessionId).first()
        chatMessages.clear()
        chatMessages.addAll(initialMessages.map { it.toDomainModel() })
    }
"""
content = content.replace('val chatMessages = remember {\n        mutableStateListOf<ChatMessage>()\n    }', 'val chatMessages = remember {\n        mutableStateListOf<ChatMessage>()\n    }\n' + launched_effect)

# 4. Helper function to add message and persist
helper_functions = """
    fun saveMessage(msg: ChatMessage) {
        if (msg.text != "Thinking...") {
            scope.launch { dao.insertMessage(msg.toEntity(sessionId)) }
        }
    }
"""
content = content.replace('Column(modifier = Modifier.fillMaxSize()) {', helper_functions + '\n    Column(modifier = Modifier.fillMaxSize()) {')

# 5. Replace chatMessages.add with helper
# This is tricky because we need to intercept add and also update `chatMessages`.
# It's better to just write a python script that uses regex or just string replacements.

replacements = [
    ('chatMessages.add(ChatMessage(text = prompt, role = MessageRole.USER))', 
     'val msg = ChatMessage(text = prompt, role = MessageRole.USER)\n                                    chatMessages.add(msg)\n                                    saveMessage(msg)'),
    
    ('chatMessages.add(ChatMessage(\n                                                    text = "", \n                                                    role = MessageRole.APP_ACTION,\n                                                    appActions = response.actions,\n                                                    editedFiles = response.editedFiles\n                                                ))',
     'val msg = ChatMessage(text = "", role = MessageRole.APP_ACTION, appActions = response.actions, editedFiles = response.editedFiles)\n                                                chatMessages.add(msg)\n                                                saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = response.text, role = MessageRole.AI))',
     'val msg = ChatMessage(text = response.text, role = MessageRole.AI)\n                                                chatMessages.add(msg)\n                                                saveMessage(msg)'),
     
    ('chatMessages[index] = generatingMessage.copy(text = "Generation stopped.")',
     'val msg = generatingMessage.copy(text = "Generation stopped.")\n                                                chatMessages[index] = msg\n                                                saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Selected image: ${option.uri}", role = MessageRole.USER))',
     'val msg = ChatMessage(text = "Selected image: ${option.uri}", role = MessageRole.USER)\n                            chatMessages.add(msg)\n                            saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Selected file: ${option.uri}", role = MessageRole.USER))',
     'val msg = ChatMessage(text = "Selected file: ${option.uri}", role = MessageRole.USER)\n                            chatMessages.add(msg)\n                            saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "File content extracted (${text?.length} chars)", role = MessageRole.APP_ACTION))',
     'val msg = ChatMessage(text = "File content extracted (${text?.length} chars)", role = MessageRole.APP_ACTION)\n                                    chatMessages.add(msg)\n                                    saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Failed to extract text", role = MessageRole.APP_ACTION))',
     'val msg = ChatMessage(text = "Failed to extract text", role = MessageRole.APP_ACTION)\n                                    chatMessages.add(msg)\n                                    saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Importing repo: ${option.url} ...", role = MessageRole.USER))',
     'val msg = ChatMessage(text = "Importing repo: ${option.url} ...", role = MessageRole.USER)\n                            chatMessages.add(msg)\n                            saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Successfully imported GitHub repo \'$repoName\' into workspace.", role = MessageRole.APP_ACTION))',
     'val msg = ChatMessage(text = "Successfully imported GitHub repo \'$repoName\' into workspace.", role = MessageRole.APP_ACTION)\n                                    chatMessages.add(msg)\n                                    saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Failed to import repo: ${result.exceptionOrNull()?.message}", role = MessageRole.APP_ACTION))',
     'val msg = ChatMessage(text = "Failed to import repo: ${result.exceptionOrNull()?.message}", role = MessageRole.APP_ACTION)\n                                    chatMessages.add(msg)\n                                    saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Workspace artifacts picker triggered", role = MessageRole.USER))',
     'val msg = ChatMessage(text = "Workspace artifacts picker triggered", role = MessageRole.USER)\n                            chatMessages.add(msg)\n                            saveMessage(msg)'),
     
    ('chatMessages.add(ChatMessage(text = "Google Drive picker triggered", role = MessageRole.USER))',
     'val msg = ChatMessage(text = "Google Drive picker triggered", role = MessageRole.USER)\n                            chatMessages.add(msg)\n                            saveMessage(msg)')
]

for old, new in replacements:
    content = content.replace(old, new)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
