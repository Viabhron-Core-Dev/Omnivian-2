package com.example.engine.fs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

object LocalFileManager {
    private var workspaceDir: File? = null

    private val _fileTreeState = MutableStateFlow<FileNode?>(null)
    val fileTreeState: StateFlow<FileNode?> = _fileTreeState.asStateFlow()


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

    fun switchWorkspace(workspaceId: String) {
        val dir = File(baseDir, "workspaces/$workspaceId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        workspaceDir = dir
        refreshFileTree()
    }

    
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


    fun getWorkspaceDir(): File {
        return workspaceDir ?: throw IllegalStateException("LocalFileManager not initialized")
    }

    fun refreshFileTree() {
        val rootNode = buildFileTree(getWorkspaceDir())
        _fileTreeState.value = rootNode
    }

    private fun buildFileTree(file: File): FileNode {
        val children = if (file.isDirectory) {
            val listFiles = file.listFiles() ?: emptyArray()
            listFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { buildFileTree(it) }
        } else {
            emptyList()
        }
        return FileNode(file = file, children = children)
    }

    suspend fun createFile(parentDir: File, name: String, isDirectory: Boolean = false): Result<File> = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentDir, name)
            if (newFile.exists()) {
                return@withContext Result.failure(Exception("File already exists"))
            }
            if (isDirectory) {
                if (newFile.mkdirs()) {
                    refreshFileTree()
                    Result.success(newFile)
                } else {
                    Result.failure(Exception("Failed to create directory"))
                }
            } else {
                if (newFile.createNewFile()) {
                    refreshFileTree()
                    Result.success(newFile)
                } else {
                    Result.failure(Exception("Failed to create file"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readFileString(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.isDirectory) {
                return@withContext Result.failure(Exception("Invalid file"))
            }
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readFileBytes(file: File): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.isDirectory) {
                return@withContext Result.failure(Exception("Invalid file"))
            }
            Result.success(file.readBytes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFile(file: File, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            refreshFileTree()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFileBytes(file: File, content: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            file.writeBytes(content)
            refreshFileTree()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.deleteRecursively()) {
                refreshFileTree()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(file.parentFile, newName)
            if (dest.exists()) {
                return@withContext Result.failure(Exception("Destination file already exists"))
            }
            if (file.renameTo(dest)) {
                refreshFileTree()
                Result.success(dest)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unzipFile(zipFile: File, destDir: File): Result<Unit> = withContext(Dispatchers.IO) {
        val result = ZipUtils.unzip(zipFile, destDir)
        if (result.isSuccess) {
            refreshFileTree()
        }
        return@withContext result
    }

    suspend fun zipDirectory(sourceDir: File, targetZip: File): Result<Unit> = withContext(Dispatchers.IO) {
        val result = ZipUtils.zipDirectory(sourceDir, targetZip)
        if (result.isSuccess) {
            refreshFileTree()
        }
        return@withContext result
    }
}
