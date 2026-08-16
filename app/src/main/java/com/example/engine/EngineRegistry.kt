package com.example.engine

import com.example.engine.mcp.McpProvider
import com.example.engine.skills.Skill
import com.example.engine.tools.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EngineRegistry {
    private val _tools = MutableStateFlow<List<Tool>>(emptyList())
    val tools: StateFlow<List<Tool>> = _tools.asStateFlow()

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val _mcpProviders = MutableStateFlow<List<McpProvider>>(emptyList())
    val mcpProviders: StateFlow<List<McpProvider>> = _mcpProviders.asStateFlow()

    fun registerTool(tool: Tool) {
        _tools.value = _tools.value + tool
    }

    fun registerSkill(skill: Skill) {
        _skills.value = _skills.value + skill
    }

    fun registerMcpProvider(provider: McpProvider) {
        _mcpProviders.value = _mcpProviders.value + provider
    }
}
