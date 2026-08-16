import re

with open('app/src/main/java/com/example/engine/fs/LocalFileManager.kt', 'r') as f:
    content = f.read()

new_methods = """
    suspend fun copyUriToFile(context: Context, uri: android.net.Uri, destFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            refreshFileTree()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
"""
content = content.replace("fun switchWorkspace", new_methods + "\n    fun switchWorkspace")

with open('app/src/main/java/com/example/engine/fs/LocalFileManager.kt', 'w') as f:
    f.write(content)
