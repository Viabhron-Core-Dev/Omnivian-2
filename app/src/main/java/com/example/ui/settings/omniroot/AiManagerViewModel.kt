package com.example.ui.settings.omniroot

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.db.AppDatabase
import com.example.engine.db.ApiProviderEntity
import com.example.engine.db.ApiKeyEntity
import com.example.engine.db.AiModelEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

data class OpenAiModelsResponse(val data: List<OpenAiModel>?)
data class OpenAiModel(val id: String)

class AiManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val db = AppDatabase.getDatabase(application)
    private val apiProviderDao = db.apiProviderDao()
    private val apiKeyDao = db.apiKeyDao()
    private val metricsDao = db.metricsDao()

    private val aiModelDao = db.aiModelDao()
    private val modelRatingDao = db.modelRatingDao()

    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val modelsAdapter = moshi.adapter(OpenAiModelsResponse::class.java)
    private val httpClient = OkHttpClient.Builder().build()

    val providers = apiProviderDao.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val activeKeys = apiKeyDao.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val availableModels = aiModelDao.getAllModels().map { models ->
        val chatModels = models.filter { it.outputType == "TEXT" && it.inputType != "AUDIO" }
        if (chatModels.isEmpty()) {
            listOf("No models fetched (Add keys and Refresh)")
        } else {
            chatModels.map { "${it.providerId}/${it.modelId}" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Loading..."))

    val availableModelEntities = aiModelDao.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun inferModelTypes(modelId: String): Pair<String, String> {
        val lowerId = modelId.lowercase()
        return when {
            lowerId.contains("tts") || lowerId.contains("speech") -> "TEXT" to "AUDIO"
            lowerId.contains("whisper") -> "AUDIO" to "TEXT"
            lowerId.contains("embed") -> "TEXT" to "EMBEDDING"
            lowerId.contains("dall-e") || lowerId.contains("midjourney") || lowerId.contains("image") -> "TEXT" to "IMAGE"
            lowerId.contains("antigravity") || lowerId.contains("unsupported") -> "TEXT" to "UNSUPPORTED"
            lowerId.contains("vision") || lowerId.contains("gpt-4o") || lowerId.contains("claude-3-5-sonnet") || lowerId.contains("gemini-1.5") -> "MULTIMODAL" to "TEXT"
            else -> "TEXT" to "TEXT"
        }
    }
    
    val totalTokens = metricsDao.getTotalTokensUsed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val totalRequests = metricsDao.getTotalRequestCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        

    val totalCost = metricsDao.getTotalEstimatedCost()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        
    val modelRatings = modelRatingDao.getRatingStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun refreshModels() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val currentKeys = apiKeyDao.getAllKeys().first()
                val currentProviders = apiProviderDao.getAllProviders().first().associateBy { it.id }
                
                for (key in currentKeys.filter { it.isActive }) {
                    val provider = currentProviders[key.providerId] ?: continue
                    val modelsUrl = if (provider.baseUrl.endsWith("/")) {
                        provider.baseUrl + "models"
                    } else {
                        provider.baseUrl + "/models"
                    }
                    
                    suspend fun applyFallbacks() {
                        val fallbacks = when(provider.id) {
                            "anthropic" -> listOf("claude-3-5-sonnet-20240620", "claude-3-opus-20240229")
                            "google_ai_studio" -> listOf("gemini-1.5-pro-latest", "gemini-1.5-flash-latest")
                            "openai" -> listOf("gpt-4o", "gpt-4o-mini", "text-embedding-3-large", "tts-1")
                            "openrouter" -> listOf("meta-llama/llama-3-8b-instruct:free")
                            "groq" -> listOf("llama3-8b-8192", "whisper-large-v3")
                            "together_ai" -> listOf("meta-llama/Llama-3-8b-chat-hf")
                            // local_gguf should not be hardcoded since we parse actual files
                            "local_gguf" -> emptyList()
                            else -> emptyList()
                        }
                        if (fallbacks.isNotEmpty()) {
                            aiModelDao.deleteModelsForProvider(provider.id)
                            aiModelDao.insertModels(fallbacks.map { 
                                val (iType, oType) = inferModelTypes(it)
                                AiModelEntity(providerId = provider.id, modelId = it, inputType = iType, outputType = oType) 
                            })
                        }
                    }
                    
                    try {
                        val reqBuilder = Request.Builder().url(modelsUrl)
                        if (provider.id == "anthropic") {
                            reqBuilder.addHeader("x-api-key", key.keyValue)
                            reqBuilder.addHeader("anthropic-version", "2023-06-01")
                        } else {
                            reqBuilder.addHeader("Authorization", "Bearer " + key.keyValue)
                        }
                        
                        val response = httpClient.newCall(reqBuilder.build()).execute()
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            val modelsResp = try { modelsAdapter.fromJson(responseBody) } catch(e: Exception) { null }
                            val entities = modelsResp?.data?.map { 
                                val (iType, oType) = inferModelTypes(it.id)
                                AiModelEntity(providerId = provider.id, modelId = it.id, inputType = iType, outputType = oType) 
                            } ?: emptyList()
                            
                            if (entities.isEmpty()) {
                                applyFallbacks()
                            } else {
                                aiModelDao.deleteModelsForProvider(provider.id)
                                aiModelDao.insertModels(entities)
                            }
                        } else {
                            Log.e("AiManager", "Failed to fetch models for ${provider.id}: ${response.code} $responseBody")
                            applyFallbacks()
                        }
                    } catch (e: Exception) {
                        Log.e("AiManager", "Exception fetching models for ${provider.id}", e)
                        applyFallbacks()
                    }
                }
            } catch (e: Exception) {
                Log.e("AiManager", "Error in refreshModels", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }



    val isImporting = MutableStateFlow(false)
    val importProgress = MutableStateFlow(0f)

    fun addLocalModel(context: Context, fileName: String, uri: android.net.Uri) {
        viewModelScope.launch {
            isImporting.value = true
            importProgress.value = 0f
            try {
                val modelsDir = java.io.File(context.filesDir, "models")
                if (!modelsDir.exists()) modelsDir.mkdirs()
                val targetFile = java.io.File(modelsDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int
                        
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        var size = -1L
                        cursor?.use { c ->
                            if (c.moveToFirst()) {
                                val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                if (sizeIndex != -1) size = c.getLong(sizeIndex)
                            }
                        }
                        
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (size > 0) {
                                importProgress.value = (totalRead.toFloat() / size.toFloat()).coerceIn(0f, 1f)
                            } else {
                                importProgress.value = (importProgress.value + 0.01f) % 1f
                            }
                        }
                    }
                }

                val (iType, oType) = inferModelTypes(fileName)
                aiModelDao.insertModels(
                    listOf(
                        com.example.engine.db.AiModelEntity(
                            providerId = "local_gguf",
                            modelId = fileName,
                            description = targetFile.absolutePath,
                            inputType = iType,
                            outputType = oType
                        )
                    )
                )
                refreshModels()
            } catch (e: Exception) {
                Log.e("AiManagerViewModel", "Error importing model", e)
            } finally {
                isImporting.value = false
            }
        }
    }

    fun addMockKey(providerId: String) {
        viewModelScope.launch {
            apiKeyDao.insertKey(
                ApiKeyEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    providerId = providerId,
                    alias = "Test Key",
                    keyMasked = "sk-...abcd",
                    keyValue = "fake-key",
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            refreshModels()
        }
    }

    fun saveRealKey(providerId: String, alias: String, keyValue: String) {
        viewModelScope.launch {
            val keyMasked = if (keyValue.length > 8) {
                "sk-..." + keyValue.takeLast(4)
            } else {
                "sk-***"
            }
            apiKeyDao.insertKey(
                ApiKeyEntity(
                    id = UUID.randomUUID().toString(),
                    providerId = providerId,
                    alias = alias,
                    keyMasked = keyMasked,
                    keyValue = keyValue,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            refreshModels()
        }
    }

    fun rateModel(providerId: String, modelName: String, isPositive: Boolean, messageId: String) {
        viewModelScope.launch {
            modelRatingDao.insertRating(
                com.example.engine.db.ModelRatingEntity(
                    messageId = messageId,
                    modelName = modelName,
                    providerId = providerId,
                    isPositive = isPositive,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
