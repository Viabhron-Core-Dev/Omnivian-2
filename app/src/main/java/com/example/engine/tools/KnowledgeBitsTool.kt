package com.example.engine.tools

import android.content.Context
import com.example.engine.knowledge.KnowledgeBitsManager
import org.json.JSONObject

class KnowledgeBitsTool(private val context: Context) : Tool {
    override val name: String = "knowledge_bits"
    override val description: String = "Manage active and ephemeral reference knowledge bits (GitHub files, code snippets, web articles, data tables, reference docs) across chat sessions. Supports querying, saving, fetching from URLs, reading, and refreshing stale references."

    override suspend fun execute(args: Map<String, Any>): String {
        val action = args["action"] as? String ?: args["operation"] as? String ?: "query"
        return try {
            when (action) {
                "save", "save_bit" -> {
                    val title = args["title"] as? String ?: "Knowledge Bit"
                    val content = args["content"] as? String ?: return "Error: 'content' parameter is required to save a knowledge bit."
                    val sourceUrl = args["source_url"] as? String ?: args["url"] as? String
                    val contentType = args["content_type"] as? String ?: "NOTE"
                    val isPinned = (args["is_pinned"] as? Boolean) ?: false
                    val ttlSeconds = ((args["ttl_seconds"] as? Number)?.toLong()) ?: (if (isPinned) 0L else 86400L)

                    val bit = KnowledgeBitsManager.saveBit(
                        context = context,
                        title = title,
                        content = content,
                        sourceUrl = sourceUrl,
                        contentType = contentType,
                        isPinned = isPinned,
                        ttlSeconds = ttlSeconds
                    )
                    "Success: Saved Knowledge Bit '${bit.title}' (ID: ${bit.id}, Type: ${bit.contentType}, Hits: ${bit.accessCount})"
                }

                "fetch", "fetch_and_cache", "cache_url" -> {
                    val url = args["url"] as? String ?: return "Error: 'url' parameter is required to fetch and cache."
                    val title = args["title"] as? String
                    val contentType = args["content_type"] as? String
                    val isPinned = (args["is_pinned"] as? Boolean) ?: false

                    val result = KnowledgeBitsManager.fetchAndCacheUrl(
                        context = context,
                        url = url,
                        customTitle = title,
                        contentType = contentType,
                        isPinned = isPinned
                    )

                    if (result.isSuccess) {
                        val bit = result.getOrThrow()
                        "Success: Fetched and cached '${bit.title}' (${bit.contentType}) from $url. Available for immediate reference."
                    } else {
                        "Error fetching from $url: ${result.exceptionOrNull()?.message}"
                    }
                }

                "query", "search" -> {
                    val query = args["query"] as? String ?: ""
                    val maxResults = ((args["max_results"] as? Number)?.toInt()) ?: 5
                    val bits = if (query.isBlank()) {
                        KnowledgeBitsManager.queryBits(context, "", maxResults)
                    } else {
                        KnowledgeBitsManager.queryBits(context, query, maxResults)
                    }

                    if (bits.isEmpty()) {
                        "No knowledge bits found matching query '$query'."
                    } else {
                        buildString {
                            appendLine("Found ${bits.size} knowledge bit(s):")
                            bits.forEachIndexed { i, bit ->
                                appendLine("${i + 1}. [${bit.contentType}] ${bit.title} (ID: ${bit.id})")
                                if (!bit.sourceUrl.isNullOrBlank()) appendLine("   Source: ${bit.sourceUrl}")
                                appendLine("   Summary: ${bit.summary ?: bit.content.take(80)}...")
                                appendLine("   Access count: ${bit.accessCount} | Pinned: ${bit.isPinned}")
                            }
                        }
                    }
                }

                "read", "get" -> {
                    val id = args["id"] as? String
                    val title = args["title"] as? String
                    val bit = if (!id.isNullOrBlank()) {
                        KnowledgeBitsManager.getBitById(context, id)
                    } else if (!title.isNullOrBlank()) {
                        KnowledgeBitsManager.queryBits(context, title, 1).firstOrNull()
                    } else {
                        return "Error: Provide 'id' or 'title' to read a knowledge bit."
                    }

                    if (bit != null) {
                        KnowledgeBitsManager.formatBitForPrompt(bit)
                    } else {
                        "Error: Knowledge bit not found."
                    }
                }

                "refresh" -> {
                    val id = args["id"] as? String ?: return "Error: 'id' parameter is required to refresh a knowledge bit."
                    val result = KnowledgeBitsManager.refreshBit(context, id)
                    if (result.isSuccess) {
                        val bit = result.getOrThrow()
                        "Success: Refreshed '${bit.title}' from upstream source (${bit.sourceUrl})."
                    } else {
                        "Error refreshing knowledge bit: ${result.exceptionOrNull()?.message}"
                    }
                }

                else -> "Error: Unknown knowledge_bits action '$action'. Available actions: 'query', 'save', 'fetch_and_cache', 'read', 'refresh'."
            }
        } catch (e: Exception) {
            "Error in knowledge_bits tool: ${e.message}"
        }
    }
}
