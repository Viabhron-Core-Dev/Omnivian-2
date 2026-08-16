package com.example.engine.settings

import com.example.engine.tools.ToolPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GlobalSettings(
    val globalToolPermissions: Map<String, ToolPermission> = emptyMap()
)

object SettingsManager {
    private val _globalSettings = MutableStateFlow(GlobalSettings())
    val globalSettings: StateFlow<GlobalSettings> = _globalSettings.asStateFlow()

    fun updateGlobalToolPermission(toolName: String, permission: ToolPermission) {
        val current = _globalSettings.value
        _globalSettings.value = current.copy(
            globalToolPermissions = current.globalToolPermissions + (toolName to permission)
        )
    }
}
