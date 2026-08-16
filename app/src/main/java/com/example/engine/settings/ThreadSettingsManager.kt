package com.example.engine.settings

import com.example.engine.tools.ToolPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThreadSettings(
    val threadId: String,
    val toolPermissions: Map<String, ToolPermission> = emptyMap()
)

class ThreadSettingsManager(private val threadId: String) {
    private val _settings = MutableStateFlow(ThreadSettings(threadId = threadId))
    val settings: StateFlow<ThreadSettings> = _settings.asStateFlow()

    fun updateToolPermission(toolName: String, permission: ToolPermission) {
        val current = _settings.value
        _settings.value = current.copy(
            toolPermissions = current.toolPermissions + (toolName to permission)
        )
    }
}
