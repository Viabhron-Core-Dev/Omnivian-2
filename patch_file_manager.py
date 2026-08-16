with open('app/src/main/java/com/example/engine/fs/LocalFileManager.kt', 'r') as f:
    content = f.read()

new_methods = """
    fun getWorkspaceName(workspaceId: String): String {
        val dir = File(baseDir, "workspaces/$workspaceId")
        if (!dir.exists()) return workspaceId
        val nameFile = File(dir, ".workspace_name")
        if (nameFile.exists()) {
            return nameFile.readText().trim()
        }
        return workspaceId
    }

    fun setWorkspaceName(workspaceId: String, name: String) {
        val dir = File(baseDir, "workspaces/$workspaceId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val nameFile = File(dir, ".workspace_name")
        nameFile.writeText(name)
    }
"""

content = content.replace(
    'fun getWorkspaces(): List<File> {',
    new_methods + '\n    fun getWorkspaces(): List<File> {'
)

with open('app/src/main/java/com/example/engine/fs/LocalFileManager.kt', 'w') as f:
    f.write(content)

