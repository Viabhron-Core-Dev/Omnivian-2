import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'r') as f:
    content = f.read()

replacement = """
    fun addMockKey(providerId: String) {
        viewModelScope.launch {
            apiKeyDao.insertKey(
                ApiKeyEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    providerId = providerId,
                    alias = "Test Key",
                    keyMasked = "sk-...abcd",
                    keyValue = "fake-key",
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            refreshModels()
        }
    }
"""

content = re.sub(r'    fun addMockKey.*?    fun saveRealKey', replacement + '\n    fun saveRealKey', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'w') as f:
    f.write(content)
