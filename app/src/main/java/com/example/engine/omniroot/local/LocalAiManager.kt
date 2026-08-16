package com.example.engine.omniroot.local

import android.content.Context
import com.example.utils.LogKeeper

object LocalAiManager {
    private var activeEngine: LlamaEngine? = null
    private var currentModelPath: String? = null
    private var currentContextSize: Int = 2048
    private var currentThreads: Int = 4

    fun getOrLoadEngine(
        context: Context, 
        absolutePath: String,
        contextSize: Int = 2048,
        numThreads: Int = 4,
        useMmap: Boolean = true,
        useMlock: Boolean = false
    ): LlamaEngine? {
        if (activeEngine != null && currentModelPath == absolutePath && currentContextSize == contextSize && currentThreads == numThreads) {
            LogKeeper.log("Local AI", "getOrLoadEngine", "Using cached RAM engine for $absolutePath")
            return activeEngine
        }
        
        LogKeeper.log("Local AI", "getOrLoadEngine", "Unloading previous model (if any) and loading new model into RAM: $absolutePath (ctx=$contextSize, threads=$numThreads)")
        activeEngine?.unloadModel()
        
        val engine = LlamaEngine(context.applicationContext)
        val loaded = engine.loadModelSafely(absolutePath, contextSize, numThreads, useMmap, useMlock)
        if (loaded) {
            activeEngine = engine
            currentModelPath = absolutePath
            currentContextSize = contextSize
            currentThreads = numThreads
            LogKeeper.log("Local AI", "getOrLoadEngine", "Successfully loaded $absolutePath into RAM.")
            return engine
        } else {
            LogKeeper.log("Local AI", "ERROR", "Failed to load $absolutePath (OOM or File Not Found)")
            engine.unloadModel()
            activeEngine = null
            currentModelPath = null
            return null
        }
    }

    fun unload() {
        LogKeeper.log("Local AI", "unload", "Unloading model from RAM.")
        activeEngine?.unloadModel()
        activeEngine = null
        currentModelPath = null
    }
}
