with open('app/src/main/java/com/example/engine/fs/LocalFileManager.kt', 'r') as f:
    content = f.read()

# Add getWorkspaces method and switchWorkspace method
new_methods = """
    private var baseDir: File? = null

    fun init(context: Context) {
        if (baseDir == null) {
            baseDir = context.filesDir
            val defaultWorkspace = File(baseDir, "workspaces/default")
            if (!defaultWorkspace.exists()) {
                defaultWorkspace.mkdirs()
            }
            workspaceDir = defaultWorkspace
            refreshFileTree()
        }
    }

    fun switchWorkspace(workspaceId: String) {
        val dir = File(baseDir, "workspaces/$workspaceId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        workspaceDir = dir
        refreshFileTree()
    }

    fun getWorkspaces(): List<File> {
        val workspacesDir = File(baseDir, "workspaces")
        if (!workspacesDir.exists()) return emptyList()
        return workspacesDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun deleteWorkspace(workspaceId: String): Boolean {
        val dir = File(baseDir, "workspaces/$workspaceId")
        if (dir.exists() && dir.absolutePath != workspaceDir?.absolutePath) {
            return dir.deleteRecursively()
        }
        return false
    }
"""

content = content.replace(
"""    fun init(context: Context) {
        if (workspaceDir == null) {
            val dir = File(context.filesDir, "workspace")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            workspaceDir = dir
            refreshFileTree()
        }
    }""", new_methods)

with open('app/src/main/java/com/example/engine/fs/LocalFileManager.kt', 'w') as f:
    f.write(content)

