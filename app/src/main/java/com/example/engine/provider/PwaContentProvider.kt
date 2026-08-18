package com.example.engine.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.engine.db.AppDatabase
import com.example.engine.db.ArtifactEntity
import com.example.engine.fs.LocalFileManager
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

class PwaContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.provider.pwa"
        
        private const val CODE_ARTIFACTS = 100
        private const val CODE_ARTIFACT_ITEM = 101
        private const val CODE_ARTIFACT_FILES = 102

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "artifacts", CODE_ARTIFACTS)
            addURI(AUTHORITY, "artifacts/*", CODE_ARTIFACT_ITEM)
            addURI(AUTHORITY, "artifacts/*/files/*", CODE_ARTIFACT_FILES)
            addURI(AUTHORITY, "artifacts/*/files/*/*", CODE_ARTIFACT_FILES)
            addURI(AUTHORITY, "artifacts/*/files/*/*/*", CODE_ARTIFACT_FILES)
        }
    }

    private val db by lazy {
        context?.let { AppDatabase.getDatabase(it) }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val database = db ?: return null
        return when (uriMatcher.match(uri)) {
            CODE_ARTIFACTS -> {
                database.artifactDao().getPwaArtifactsCursor().apply {
                    setNotificationUri(context?.contentResolver, uri)
                }
            }
            CODE_ARTIFACT_ITEM -> {
                val id = uri.lastPathSegment ?: return null
                database.artifactDao().getArtifactCursorById(id).apply {
                    setNotificationUri(context?.contentResolver, uri)
                }
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_ARTIFACTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.artifact"
            CODE_ARTIFACT_ITEM -> "vnd.android.cursor.item/vnd.$AUTHORITY.artifact"
            CODE_ARTIFACT_FILES -> {
                val path = uri.path ?: return null
                when {
                    path.endsWith(".html", ignoreCase = true) -> "text/html"
                    path.endsWith(".css", ignoreCase = true) -> "text/css"
                    path.endsWith(".js", ignoreCase = true) -> "application/javascript"
                    path.endsWith(".json", ignoreCase = true) -> "application/json"
                    path.endsWith(".png", ignoreCase = true) -> "image/png"
                    path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    path.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
                    else -> "application/octet-stream"
                }
            }
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val database = db ?: return null
        val ctx = context ?: return null
        if (uriMatcher.match(uri) != CODE_ARTIFACTS || values == null) {
            return null
        }

        val id = values.getAsString("id") ?: UUID.randomUUID().toString()
        val title = values.getAsString("title") ?: "New PWA"
        val type = values.getAsString("type") ?: "HTML"
        val content = values.getAsString("content") ?: "<html><body><h1>$title</h1></body></html>"
        val isPinned = values.getAsBoolean("isPinned") ?: false
        val iconUri = values.getAsString("iconUri")
        val isLightweight = values.getAsBoolean("isLightweight") ?: false
        val manifestJson = values.getAsString("manifestJson")
        val settingsJson = values.getAsString("settingsJson")
        val version = values.getAsLong("version") ?: 1L
        val workspaceId = values.getAsString("workspaceId") ?: "artifact_$id"

        val entity = ArtifactEntity(
            id = id,
            title = title,
            type = type,
            content = content,
            isPinned = isPinned,
            iconUri = iconUri,
            isLightweight = isLightweight,
            manifestJson = manifestJson,
            settingsJson = settingsJson,
            version = version,
            updatedAt = System.currentTimeMillis(),
            workspaceId = workspaceId
        )

        // Initialize folder bundle on internal storage
        val workspaceDir = File(ctx.filesDir, "workspaces/$workspaceId")
        if (!workspaceDir.exists()) workspaceDir.mkdirs()
        File(workspaceDir, "index.html").writeText(content)
        if (manifestJson != null) {
            File(workspaceDir, "manifest.json").writeText(manifestJson)
        }
        if (settingsJson != null) {
            File(workspaceDir, "settings.json").writeText(settingsJson)
        }

        runBlocking {
            database.artifactDao().insertArtifact(entity)
        }

        val resultUri = Uri.parse("content://$AUTHORITY/artifacts/$id")
        ctx.contentResolver.notifyChange(Uri.parse("content://$AUTHORITY/artifacts"), null)
        return resultUri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val database = db ?: return 0
        val ctx = context ?: return 0
        val id = when (uriMatcher.match(uri)) {
            CODE_ARTIFACT_ITEM -> uri.lastPathSegment ?: return 0
            else -> return 0
        }

        if (values == null) return 0

        var rowsUpdated = 0
        runBlocking {
            val existing = database.artifactDao().getArtifactById(id)
            if (existing != null) {
                val newTitle = values.getAsString("title") ?: existing.title
                val newContent = values.getAsString("content") ?: existing.content
                val newIconUri = values.getAsString("iconUri") ?: existing.iconUri
                val newIsLightweight = values.getAsBoolean("isLightweight") ?: existing.isLightweight
                val newManifestJson = values.getAsString("manifestJson") ?: existing.manifestJson
                val newSettingsJson = values.getAsString("settingsJson") ?: existing.settingsJson
                val newVersion = values.getAsLong("version") ?: (existing.version + 1L)

                val updated = existing.copy(
                    title = newTitle,
                    content = newContent,
                    iconUri = newIconUri,
                    isLightweight = newIsLightweight,
                    manifestJson = newManifestJson,
                    settingsJson = newSettingsJson,
                    version = newVersion,
                    updatedAt = System.currentTimeMillis()
                )

                database.artifactDao().updateArtifact(updated)
                
                // Sync file bundle
                val workspaceDir = File(ctx.filesDir, "workspaces/${existing.workspaceId ?: "artifact_$id"}")
                if (workspaceDir.exists()) {
                    if (values.containsKey("content")) {
                        File(workspaceDir, "index.html").writeText(newContent)
                    }
                    if (values.containsKey("manifestJson") && newManifestJson != null) {
                        File(workspaceDir, "manifest.json").writeText(newManifestJson)
                    }
                    if (values.containsKey("settingsJson") && newSettingsJson != null) {
                        File(workspaceDir, "settings.json").writeText(newSettingsJson)
                    }
                }
                rowsUpdated = 1
            }
        }

        if (rowsUpdated > 0) {
            ctx.contentResolver.notifyChange(uri, null)
            ctx.contentResolver.notifyChange(Uri.parse("content://$AUTHORITY/artifacts"), null)
        }
        return rowsUpdated
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val database = db ?: return 0
        val ctx = context ?: return 0
        val id = when (uriMatcher.match(uri)) {
            CODE_ARTIFACT_ITEM -> uri.lastPathSegment ?: return 0
            else -> return 0
        }

        var deleted = 0
        runBlocking {
            val existing = database.artifactDao().getArtifactById(id)
            if (existing != null) {
                database.artifactDao().deleteArtifact(existing)
                val workspaceDir = File(ctx.filesDir, "workspaces/${existing.workspaceId ?: "artifact_$id"}")
                if (workspaceDir.exists()) {
                    workspaceDir.deleteRecursively()
                }
                deleted = 1
            }
        }

        if (deleted > 0) {
            ctx.contentResolver.notifyChange(uri, null)
            ctx.contentResolver.notifyChange(Uri.parse("content://$AUTHORITY/artifacts"), null)
        }
        return deleted
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: throw FileNotFoundException("Context unavailable")
        val match = uriMatcher.match(uri)
        if (match != CODE_ARTIFACT_FILES) {
            throw FileNotFoundException("Unsupported URI: $uri")
        }

        val segments = uri.pathSegments
        if (segments.size < 4 || segments[0] != "artifacts" || segments[2] != "files") {
            throw FileNotFoundException("Malformed artifact file URI: $uri")
        }

        val artifactId = segments[1]
        val relativeFilePath = segments.subList(3, segments.size).joinToString(File.separator)

        val workspaceDir = File(ctx.filesDir, "workspaces/artifact_$artifactId")
        val targetFile = File(workspaceDir, relativeFilePath)

        // Strict Canonical Path traversal defense
        val baseCanonical = workspaceDir.canonicalFile
        val targetCanonical = targetFile.canonicalFile
        if (!targetCanonical.path.startsWith(baseCanonical.path)) {
            throw SecurityException("Path traversal attempt detected: $relativeFilePath")
        }

        if (!targetFile.exists()) {
            // Check alternate workspace naming if workspaceId is custom
            val altWorkspaceDir = File(ctx.filesDir, "workspaces/$artifactId")
            val altFile = File(altWorkspaceDir, relativeFilePath)
            if (altFile.exists() && altFile.canonicalPath.startsWith(altWorkspaceDir.canonicalPath)) {
                return ParcelFileDescriptor.open(altFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            throw FileNotFoundException("Artifact file not found: $relativeFilePath")
        }

        return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
