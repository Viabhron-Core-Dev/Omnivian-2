import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'r') as f:
    content = f.read()

replacement = """
    val availableModels = aiModelDao.getAllModels().map { models ->
        val chatModels = models.filter { it.outputType == "TEXT" && it.inputType != "AUDIO" }
        if (chatModels.isEmpty()) {
            listOf("No models fetched (Add keys and Refresh)")
        } else {
            chatModels.map { "${it.providerId}/${it.modelId}" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Loading..."))

    val availableModelEntities = aiModelDao.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun inferModelTypes(modelId: String): Pair<String, String> {
        val lowerId = modelId.lowercase()
        return when {
            lowerId.contains("tts") || lowerId.contains("speech") -> "TEXT" to "AUDIO"
            lowerId.contains("whisper") -> "AUDIO" to "TEXT"
            lowerId.contains("embed") -> "TEXT" to "EMBEDDING"
            lowerId.contains("dall-e") || lowerId.contains("midjourney") || lowerId.contains("image") -> "TEXT" to "IMAGE"
            lowerId.contains("antigravity") || lowerId.contains("unsupported") -> "TEXT" to "UNSUPPORTED"
            lowerId.contains("vision") || lowerId.contains("gpt-4o") || lowerId.contains("claude-3-5-sonnet") || lowerId.contains("gemini-1.5") -> "MULTIMODAL" to "TEXT"
            else -> "TEXT" to "TEXT"
        }
    }
    
    val totalTokens"""

content = re.sub(r'    val availableModels =.*?    val totalTokens', replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'w') as f:
    f.write(content)
