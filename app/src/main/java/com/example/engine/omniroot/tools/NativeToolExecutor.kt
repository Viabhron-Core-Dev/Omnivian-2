package com.example.engine.omniroot.tools

import android.content.Context
import java.io.File
import org.json.JSONObject

object NativeToolExecutor {
    
    fun execute(context: Context, functionName: String, argumentsJson: String): String {
        return try {
            val args = JSONObject(argumentsJson)
            val rootDir = File(context.filesDir, "workspace")
            if (!rootDir.exists()) rootDir.mkdirs()
            
            when (functionName) {
                "read_file" -> {
                    val path = args.optString("path", "")
                    val file = File(rootDir, path)
                    if (file.exists() && file.isFile) {
                        file.readText()
                    } else {
                        "Error: File not found at $path"
                    }
                }
                "write_file" -> {
                    val path = args.optString("path", "")
                    val content = args.optString("content", "")
                    val file = File(rootDir, path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                    "Success: File written to $path"
                }
                "list_files" -> {
                    val path = args.optString("path", "")
                    val dir = if (path.isEmpty()) rootDir else File(rootDir, path)
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles()?.map { if (it.isDirectory) it.name + "/" else it.name }
                        "Files in directory: " + (files?.joinToString(", ") ?: "none")
                    } else {
                        "Error: Directory not found at $path"
                    }
                }
                "knowledge_bits", "save_knowledge_bit", "fetch_and_cache_bit", "query_knowledge_bits", "read_knowledge_bit", "refresh_knowledge_bit" -> {
                    kotlinx.coroutines.runBlocking {
                        val map = mutableMapOf<String, Any>()
                        val keys = args.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = args.get(key)
                        }
                        if (functionName != "knowledge_bits" && !map.containsKey("action")) {
                            map["action"] = when (functionName) {
                                "save_knowledge_bit" -> "save"
                                "fetch_and_cache_bit" -> "fetch_and_cache"
                                "query_knowledge_bits" -> "query"
                                "read_knowledge_bit" -> "read"
                                "refresh_knowledge_bit" -> "refresh"
                                else -> "query"
                            }
                        }
                        com.example.engine.tools.KnowledgeBitsTool(context).execute(map)
                    }
                }
                else -> {
                    "Error: Unknown tool $functionName"
                }
            }
        } catch (e: Exception) {
            "Error executing tool $functionName: ${e.message}"
        }
    }
}
