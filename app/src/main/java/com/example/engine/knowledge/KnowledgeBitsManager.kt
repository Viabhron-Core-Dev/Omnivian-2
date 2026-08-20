package com.example.engine.knowledge

import android.content.Context
import com.example.engine.db.AppDatabase
import com.example.engine.db.KnowledgeBitEntity
import com.example.utils.LogKeeper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object KnowledgeBitsManager {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun saveBit(
        context: Context,
        title: String,
        content: String,
        sourceUrl: String? = null,
        contentType: String = "NOTE",
        isPinned: Boolean = false,
        ttlSeconds: Long = 86400L,
        summary: String? = null,
        workspaceId: String? = null
    ): KnowledgeBitEntity = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        
        // If an existing bit with the same sourceUrl exists, update it
        val existing = sourceUrl?.let { db.knowledgeBitDao().getBitByUrl(it) }
        val bitId = existing?.id ?: UUID.randomUUID().toString()
        val originalTime = existing?.originalTimestamp ?: now

        val entity = KnowledgeBitEntity(
            id = bitId,
            title = title.ifBlank { "Untitled Knowledge Bit" },
            content = content,
            sourceUrl = sourceUrl,
            contentType = contentType.uppercase(),
            originalTimestamp = originalTime,
            cachedAt = now,
            lastAccessedAt = now,
            lastVerifiedAt = now,
            accessCount = (existing?.accessCount ?: 0) + 1,
            isPinned = isPinned || (existing?.isPinned == true),
            ttlSeconds = if (isPinned) 0L else ttlSeconds,
            workspaceId = workspaceId,
            summary = summary ?: content.take(150).replace("\n", " ").trim()
        )

        db.knowledgeBitDao().insertOrUpdate(entity)
        LogKeeper.log("KnowledgeBits", "Saved", "Saved knowledge bit: ${entity.title} (${entity.contentType})")
        entity
    }

    suspend fun fetchAndCacheUrl(
        context: Context,
        url: String,
        customTitle: String? = null,
        contentType: String? = null,
        isPinned: Boolean = false,
        ttlSeconds: Long = 86400L
    ): Result<KnowledgeBitEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            val request = Request.Builder()
                .url(normalizedUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile; OmniRoot/1.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: ""
            if (body.isBlank()) {
                return@withContext Result.failure(Exception("Received empty content from $url"))
            }

            // Derive Content Type
            val detectedType = contentType ?: inferContentType(normalizedUrl, body)
            
            // Clean body if HTML
            val cleanedContent = if (detectedType == "ARTICLE" || detectedType == "DOCUMENT") {
                cleanHtmlContent(body)
            } else {
                body
            }

            // Derive Title
            val title = customTitle?.takeIf { it.isNotBlank() } 
                ?: extractTitleFromUrlOrContent(normalizedUrl, body)

            val bit = saveBit(
                context = context,
                title = title,
                content = cleanedContent,
                sourceUrl = url,
                contentType = detectedType,
                isPinned = isPinned,
                ttlSeconds = ttlSeconds,
                summary = cleanedContent.take(160).replace("\n", " ").trim()
            )

            Result.success(bit)
        } catch (e: Exception) {
            LogKeeper.log("KnowledgeBits", "FetchError", "Failed to fetch $url: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun queryBits(context: Context, query: String, maxResults: Int = 10): List<KnowledgeBitEntity> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val bits = db.knowledgeBitDao().searchBits(query).take(maxResults)
        val now = System.currentTimeMillis()
        bits.forEach { bit ->
            db.knowledgeBitDao().incrementAccess(bit.id, now)
        }
        bits
    }

    suspend fun getBitById(context: Context, id: String): KnowledgeBitEntity? = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val bit = db.knowledgeBitDao().getBitById(id)
        if (bit != null) {
            db.knowledgeBitDao().incrementAccess(bit.id, System.currentTimeMillis())
        }
        bit
    }

    suspend fun refreshBit(context: Context, id: String): Result<KnowledgeBitEntity> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val bit = db.knowledgeBitDao().getBitById(id) ?: return@withContext Result.failure(Exception("Bit not found"))
        val sourceUrl = bit.sourceUrl
        if (sourceUrl.isNullOrBlank()) {
            return@withContext Result.failure(Exception("No upstream source URL to refresh from"))
        }

        fetchAndCacheUrl(
            context = context,
            url = sourceUrl,
            customTitle = bit.title,
            contentType = bit.contentType,
            isPinned = bit.isPinned,
            ttlSeconds = bit.ttlSeconds
        )
    }

    suspend fun togglePin(context: Context, id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        db.knowledgeBitDao().setPinned(id, isPinned)
    }

    suspend fun deleteBit(context: Context, id: String) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        db.knowledgeBitDao().deleteBit(id)
    }

    suspend fun pruneExpired(context: Context): Int = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val pruned = db.knowledgeBitDao().pruneExpired(System.currentTimeMillis())
        if (pruned > 0) {
            LogKeeper.log("KnowledgeBits", "Pruned", "Auto-pruned $pruned expired knowledge bits")
        }
        pruned
    }

    fun formatBitForPrompt(bit: KnowledgeBitEntity): String {
        val now = System.currentTimeMillis()
        val ageHours = (now - bit.cachedAt) / (1000 * 60 * 60)
        val ageDesc = when {
            ageHours < 1 -> "Cached just now"
            ageHours < 24 -> "Cached ${ageHours}h ago"
            else -> "Cached ${ageHours / 24}d ago"
        }
        val isStale = ageHours > 72 && !bit.sourceUrl.isNullOrBlank()
        val status = if (isStale) "⚠️ POTENTIALLY STALE (Consider refresh if upstream changed)" else "✅ FRESH"

        return buildString {
            appendLine("---")
            appendLine("📑 [KNOWLEDGE BIT: ${bit.title}]")
            appendLine("• Type: ${bit.contentType} | Status: $status")
            if (!bit.sourceUrl.isNullOrBlank()) {
                appendLine("• Upstream Source: ${bit.sourceUrl}")
            }
            appendLine("• $ageDesc (Hit Count: ${bit.accessCount})")
            appendLine("---")
            appendLine(bit.content)
            appendLine("---")
        }
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        // Convert GitHub standard web view URLs to rawusercontent
        if (clean.contains("github.com") && clean.contains("/blob/")) {
            clean = clean.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
        }
        return clean
    }

    private fun inferContentType(url: String, content: String): String {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.endsWith(".kt") || lowerUrl.endsWith(".java") || lowerUrl.endsWith(".py") ||
            lowerUrl.endsWith(".js") || lowerUrl.endsWith(".ts") || lowerUrl.endsWith(".cpp") ||
            lowerUrl.endsWith(".rs") || lowerUrl.endsWith(".go") || lowerUrl.endsWith(".json") ||
            lowerUrl.endsWith(".yaml") || lowerUrl.endsWith(".xml") || lowerUrl.endsWith(".sh") ||
            lowerUrl.endsWith(".c") || lowerUrl.endsWith(".h") || lowerUrl.endsWith(".gradle.kts") -> "CODE"
            
            lowerUrl.endsWith(".csv") || lowerUrl.endsWith(".tsv") || content.lines().take(5).all { it.contains(",") || it.contains("\t") } -> "TABLE"
            lowerUrl.endsWith(".md") || lowerUrl.endsWith(".txt") -> "NOTE"
            content.contains("<html", ignoreCase = true) || content.contains("<!DOCTYPE html", ignoreCase = true) -> "ARTICLE"
            else -> "DOCUMENT"
        }
    }

    private fun cleanHtmlContent(html: String): String {
        // Strip scripts, styles, and html tags to produce clean readable text
        var text = html.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<nav[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<footer[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<header[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), "")
        
        // Convert basic formatting
        text = text.replace(Regex("<h[1-6][^>]*>(.*?)</h[1-6]>", RegexOption.IGNORE_CASE), "\n\n### $1\n")
        text = text.replace(Regex("<p[^>]*>(.*?)</p>", RegexOption.IGNORE_CASE), "\n$1\n")
        text = text.replace(Regex("<li[^>]*>(.*?)</li>", RegexOption.IGNORE_CASE), "\n- $1")
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        
        // Remove remaining tags
        text = text.replace(Regex("<[^>]+>"), " ")
        
        // Decode common HTML entities
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        
        // Normalize whitespace
        return text.replace(Regex("[ \\t]+"), " ").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun extractTitleFromUrlOrContent(url: String, content: String): String {
        // Try extracting HTML title
        val titleMatcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(content)
        if (titleMatcher.find()) {
            val title = titleMatcher.group(1)?.trim()
            if (!title.isNullOrBlank()) {
                return title.replace(Regex("\\s+"), " ")
            }
        }

        // Fallback: extract last path component from URL
        val uriPath = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val lastSegment = uriPath.substringAfterLast('/')
        if (lastSegment.isNotBlank()) {
            return lastSegment
        }

        return "Knowledge Bit (${url.take(30)}...)"
    }
}
