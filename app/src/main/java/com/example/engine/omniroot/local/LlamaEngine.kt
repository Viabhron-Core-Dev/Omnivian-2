package com.example.engine.omniroot.local

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LlamaEngine(private val context: Context) {
    companion object {
        private const val TAG = "LlamaEngine"
        
        @JvmStatic
        fun onNativeLog(level: String, message: String) {
            com.example.utils.LogKeeper.log(level, "Local AI (C++)", message)
        }

        init {
            try {
                System.loadLibrary("llama_bridge")
                Log.i(TAG, "Successfully loaded native llama_bridge library")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library llama_bridge", e)
            }
        }
    }

    /**
     * Initializes the native context with a specific .gguf file, with RAM safety checks.
     */
    fun loadModelSafely(
        path: String,
        contextSize: Int = 2048,
        numThreads: Int = 4,
        useMmap: Boolean = true,
        useMlock: Boolean = false
    ): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableRamBytes = memoryInfo.availMem
        Log.i(TAG, "Available RAM: ${availableRamBytes / (1024*1024)} MB")

        if (availableRamBytes < (512L * 1024 * 1024)) {
            Log.w(TAG, "OOM WARNING: Device has very low RAM available.")
        }

        return loadModel(path, contextSize, numThreads, useMmap, useMlock)
    }

    private external fun loadModel(
        path: String, 
        contextSize: Int, 
        numThreads: Int, 
        useMmap: Boolean, 
        useMlock: Boolean
    ): Boolean

    private var tokenListener: ((String) -> Unit)? = null

    // Called from C++ via JNI
    fun onTokenGenerated(token: String) {
        tokenListener?.invoke(token)
    }

    fun predictStream(
        prompt: String, 
        temperature: Float = 0.7f,
        minP: Float = 0.05f,
        topP: Float = 0.95f,
        maxTokens: Int = 2048,
        listener: (String) -> Unit
    ) {
        tokenListener = listener
        predictStreamNative(prompt, temperature, minP, topP, maxTokens)
        tokenListener = null // Cleanup after finish
    }

    fun predictFlow(
        prompt: String,
        temperature: Float = 0.7f,
        minP: Float = 0.05f,
        topP: Float = 0.95f,
        maxTokens: Int = 2048
    ): Flow<String> = callbackFlow {
        // 1. Assign the JNI listener to push words into the Flow pipe
        tokenListener = { token ->
            trySend(token)
        }

        // 2. Launch the heavy C++ math in a background thread so the UI doesn't freeze
        launch(Dispatchers.IO) {
            predictStreamNative(prompt, temperature, minP, topP, maxTokens)
            // 3. When C++ finishes the loop, close the pipe
            close()
        }

        // 4. Cleanup if the user hits "Stop" and cancels the coroutine
        awaitClose {
            tokenListener = null
        }
    }

    private external fun predictStreamNative(
        prompt: String,
        temperature: Float,
        minP: Float,
        topP: Float,
        maxTokens: Int
    )
    external fun predict(prompt: String): String
    external fun unloadModel()
}
