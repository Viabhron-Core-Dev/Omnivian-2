package com.example.engine.mcp

interface McpProvider {
    val providerId: String
    suspend fun connect()
    suspend fun executeRequest(request: String): String
}
