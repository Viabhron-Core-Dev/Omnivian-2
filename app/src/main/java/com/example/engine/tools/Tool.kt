package com.example.engine.tools

interface Tool {
    val name: String
    val description: String
    suspend fun execute(args: Map<String, Any>): String
}
