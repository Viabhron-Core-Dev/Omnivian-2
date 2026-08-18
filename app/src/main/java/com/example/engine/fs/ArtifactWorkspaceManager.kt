package com.example.engine.fs

import android.content.Context
import com.example.engine.db.AppDatabase
import com.example.engine.db.ArtifactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object ArtifactWorkspaceManager {

    /**
     * Initializes or prepares a workspace folder for an artifact.
     * Extracts existing artifact content (HTML/CSS/JS or Notes) into the workspace directory if not already created.
     */
    suspend fun openArtifactInWorkspace(context: Context, artifact: ArtifactEntity): String = withContext(Dispatchers.IO) {
        val workspaceId = artifact.workspaceId?.ifBlank { null } ?: "artifact_${artifact.id}"
        val baseDir = context.filesDir
        val workspaceFolder = File(baseDir, "workspaces/$workspaceId")
        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdirs()
        }

        // Write workspace metadata name
        val nameFile = File(workspaceFolder, ".workspace_name")
        nameFile.writeText(artifact.title)

        // Store artifact reference ID
        val metaFile = File(workspaceFolder, ".artifact_id")
        metaFile.writeText(artifact.id)

        // Extract content into files if the workspace is empty or newly created
        val existingFiles = workspaceFolder.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
        if (existingFiles.isEmpty()) {
            if (artifact.type == "COLOR_NOTES") {
                val notesFile = File(workspaceFolder, "notes.json")
                notesFile.writeText(artifact.content)
                
                val mdFile = File(workspaceFolder, "README.md")
                mdFile.writeText("# ${artifact.title}\n\nVoice and Color Notes collection.\n")
            } else {
                // HTML / PWA / Web app
                val indexFile = File(workspaceFolder, "index.html")
                indexFile.writeText(artifact.content)

                val manifestFile = File(workspaceFolder, "manifest.json")
                if (artifact.manifestJson != null) {
                    manifestFile.writeText(artifact.manifestJson)
                } else {
                    val defaultManifest = JSONObject().apply {
                        put("name", artifact.title)
                        put("short_name", artifact.title.take(12))
                        put("start_url", "./index.html")
                        put("display", if (artifact.isLightweight) "minimal-ui" else "standalone")
                        put("theme_color", "#1E1E2E")
                        put("background_color", "#11111B")
                    }
                    manifestFile.writeText(defaultManifest.toString(2))
                }

                val settingsFile = File(workspaceFolder, "settings.json")
                if (artifact.settingsJson != null) {
                    settingsFile.writeText(artifact.settingsJson)
                } else {
                    settingsFile.writeText(JSONObject().put("theme", "auto").put("isLightweight", artifact.isLightweight).toString(2))
                }
                
                val readme = File(workspaceFolder, "README.md")
                readme.writeText("# ${artifact.title}\n\nInteractive web artifact.\n")
            }
        }

        // Link artifact to this workspaceId if not already linked
        if (artifact.workspaceId != workspaceId) {
            val db = AppDatabase.getDatabase(context)
            db.artifactDao().updateArtifact(artifact.copy(workspaceId = workspaceId, updatedAt = System.currentTimeMillis()))
        }

        LocalFileManager.switchWorkspace(workspaceId)
        workspaceId
    }

    /**
     * Finds the artifact ID associated with the current workspace.
     */
    fun getArtifactIdForWorkspace(workspaceId: String): String? {
        val workspaceDir = LocalFileManager.getWorkspaceDir()
        val metaFile = File(workspaceDir, ".artifact_id")
        if (metaFile.exists()) {
            return metaFile.readText().trim().ifBlank { null }
        }
        if (workspaceId.startsWith("artifact_")) {
            return workspaceId.removePrefix("artifact_")
        }
        return null
    }

    /**
     * Saves changes from the workspace back to the original artifact entity.
     */
    suspend fun saveWorkspaceToArtifact(context: Context, workspaceId: String): Result<ArtifactEntity> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val artifactId = getArtifactIdForWorkspace(workspaceId)
                ?: return@withContext Result.failure(IllegalStateException("No linked artifact for workspace $workspaceId"))

            val existing = db.artifactDao().getArtifactById(artifactId)
                ?: return@withContext Result.failure(IllegalStateException("Artifact $artifactId not found in database"))

            val workspaceDir = LocalFileManager.getWorkspaceDir()
            val updatedContent = if (existing.type == "COLOR_NOTES") {
                val notesFile = File(workspaceDir, "notes.json")
                if (notesFile.exists()) notesFile.readText() else existing.content
            } else {
                val indexFile = File(workspaceDir, "index.html")
                if (indexFile.exists()) indexFile.readText() else existing.content
            }

            val manifestFile = File(workspaceDir, "manifest.json")
            val manifestJson = if (manifestFile.exists()) manifestFile.readText() else existing.manifestJson

            val settingsFile = File(workspaceDir, "settings.json")
            val settingsJson = if (settingsFile.exists()) settingsFile.readText() else existing.settingsJson

            val isLightweight = try {
                if (settingsJson != null) JSONObject(settingsJson).optBoolean("isLightweight", existing.isLightweight) else existing.isLightweight
            } catch (_: Exception) { existing.isLightweight }

            val updatedName = LocalFileManager.getWorkspaceName(workspaceId)
            val updatedEntity = existing.copy(
                title = updatedName.ifBlank { existing.title },
                content = updatedContent,
                manifestJson = manifestJson,
                settingsJson = settingsJson,
                isLightweight = isLightweight,
                version = existing.version + 1L,
                updatedAt = System.currentTimeMillis()
            )

            db.artifactDao().updateArtifact(updatedEntity)
            Result.success(updatedEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Forks the current workspace into a new artifact and new workspace repo.
     */
    suspend fun forkWorkspaceToNewArtifact(context: Context, currentWorkspaceId: String, newTitle: String): Result<Pair<ArtifactEntity, String>> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val newArtifactId = UUID.randomUUID().toString()
            val newWorkspaceId = "artifact_$newArtifactId"

            val sourceDir = LocalFileManager.getWorkspaceDir()
            val targetDir = File(context.filesDir, "workspaces/$newWorkspaceId")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Copy all files
            sourceDir.copyRecursively(targetDir, overwrite = true)

            // Update metadata files in new workspace
            File(targetDir, ".workspace_name").writeText(newTitle)
            File(targetDir, ".artifact_id").writeText(newArtifactId)

            // Determine content
            val indexFile = File(targetDir, "index.html")
            val notesFile = File(targetDir, "notes.json")
            val (type, content) = when {
                indexFile.exists() -> "HTML" to indexFile.readText()
                notesFile.exists() -> "COLOR_NOTES" to notesFile.readText()
                else -> "HTML" to "<html><body><h2>${newTitle}</h2></body></html>"
            }

            val manifestFile = File(targetDir, "manifest.json")
            val manifestJson = if (manifestFile.exists()) manifestFile.readText() else null

            val settingsFile = File(targetDir, "settings.json")
            val settingsJson = if (settingsFile.exists()) settingsFile.readText() else null

            val newArtifact = ArtifactEntity(
                id = newArtifactId,
                title = newTitle,
                type = type,
                content = content,
                isPinned = false,
                manifestJson = manifestJson,
                settingsJson = settingsJson,
                version = 1L,
                updatedAt = System.currentTimeMillis(),
                workspaceId = newWorkspaceId
            )

            db.artifactDao().insertArtifact(newArtifact)
            LocalFileManager.switchWorkspace(newWorkspaceId)

            Result.success(Pair(newArtifact, newWorkspaceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves normal chat workspace as a new Artifact (mini app).
     */
    suspend fun saveCurrentChatAsArtifact(context: Context, workspaceId: String, title: String): Result<ArtifactEntity> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val workspaceDir = LocalFileManager.getWorkspaceDir()
            val indexFile = File(workspaceDir, "index.html")
            val notesFile = File(workspaceDir, "notes.json")

            val (type, content) = when {
                indexFile.exists() -> "HTML" to indexFile.readText()
                notesFile.exists() -> "COLOR_NOTES" to notesFile.readText()
                else -> {
                    // Collect html or code files if index.html is missing
                    val htmlFile = workspaceDir.listFiles()?.firstOrNull { it.extension.lowercase() in listOf("html", "htm", "js") }
                    if (htmlFile != null) {
                        "HTML" to htmlFile.readText()
                    } else {
                        "HTML" to "<html><body style='font-family:sans-serif;padding:24px;'><h2>$title</h2><p>Saved from chat session $workspaceId.</p></body></html>"
                    }
                }
            }

            val manifestFile = File(workspaceDir, "manifest.json")
            val manifestJson = if (manifestFile.exists()) manifestFile.readText() else null

            val settingsFile = File(workspaceDir, "settings.json")
            val settingsJson = if (settingsFile.exists()) settingsFile.readText() else null

            val artifactId = UUID.randomUUID().toString()
            val artifact = ArtifactEntity(
                id = artifactId,
                title = title.ifBlank { "Mini App ($workspaceId)" },
                type = type,
                content = content,
                isPinned = false,
                manifestJson = manifestJson,
                settingsJson = settingsJson,
                version = 1L,
                updatedAt = System.currentTimeMillis(),
                workspaceId = workspaceId
            )

            // Also tag the workspace
            File(workspaceDir, ".artifact_id").writeText(artifactId)

            db.artifactDao().insertArtifact(artifact)
            Result.success(artifact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
