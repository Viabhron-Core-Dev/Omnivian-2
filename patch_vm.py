import re

path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

new_func = """
    fun addLocalModel(fileName: String, uriString: String) {
        viewModelScope.launch {
            val (iType, oType) = inferModelTypes(fileName)
            aiModelDao.insertModels(
                listOf(
                    com.example.engine.db.AiModelEntity(
                        providerId = "local_gguf",
                        modelId = fileName,
                        modelName = fileName,
                        description = uriString,
                        inputType = iType,
                        outputType = oType
                    )
                )
            )
            refreshModels()
        }
    }
"""

content = content.replace('    fun addMockKey(providerId: String) {', new_func + '\n    fun addMockKey(providerId: String) {')

with open(path, 'w') as f:
    f.write(content)
