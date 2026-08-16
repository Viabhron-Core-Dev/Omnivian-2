package com.example.engine.server

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object PreviewServerManager {
    private var server: PreviewServer? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(workspaceRoot: File, port: Int = 8081) {
        if (server?.isAlive == true) return
        try {
            server = PreviewServer(port, workspaceRoot).apply {
                start(5000, false)
            }
            _isRunning.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isRunning.value = false
        }
    }

    fun stop() {
        server?.stop()
        server = null
        _isRunning.value = false
    }
}
