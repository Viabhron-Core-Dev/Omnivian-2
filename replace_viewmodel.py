import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'r') as f:
    content = f.read()

new_content = """package com.example.ui.settings.omniroute

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
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

data class OpenAiModelsResponse(val data: List<OpenAiModel>?)
data class OpenAiModel(val id: String)

class AiManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val apiProviderDao = db.apiProviderDao()
    private val apiKeyDao = db.apiKeyDao()
    private val metricsDao = db.metricsDao()
    private val aiModelDao = db.aiModelDao()
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val modelsAdapter = moshi.adapter(OpenAiModelsResponse::class.java)
    private val httpClient = OkHttpClient.Builder().build()

    val providers = apiProviderDao.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val activeKeys = apiKeyDao.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableModels = aiModelDao.getAllModels().map { models ->
        if (models.isEmpty()) {
            listOf("No models fetched (Add keys and Refresh)")
        } else {
            models.map { "${it.providerId}/${it.modelId}" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Loading..."))

    val totalTokens = metricsDao.getTotalTokensUsed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val totalRequests = metricsDao.getTotalRequestCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val totalCost = metricsDao.getTotalEstimatedCost()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refreshModels() {
        viewModelScope.launch {
            try {
                val currentKeys = activeKeys.first()
                val currentProviders = providers.first().associateBy { it.id }
                
                for (key in currentKeys.filter { it.isActive }) {
                    val provider = currentProviders[key.providerId] ?: continue
                    val modelsUrl = if (provider.baseUrl.endsWith("/")) {
                        provider.baseUrl + "models"
                    } else {
                        provider.baseUrl + "/models"
                    }
                    
                    try {
                        val reqBuilder = Request.Builder().url(modelsUrl)
                        if (provider.id == "anthropic") {
                            reqBuilder.addHeader("x-api-key", key.keyValue)
                            reqBuilder.addHeader("anthropic-version", "2023-06-01")
                        } else if (provider.id == "google_ai_studio") {
                            // Gemini doesn't strictly need Bearer for the OpenAI endpoint if passed as Bearer, but let's standardise
                            reqBuilder.addHeader("Authorization", "Bearer " + key.keyValue)
                        } else {
                            reqBuilder.addHeader("Authorization", "Bearer " + key.keyValue)
                        }
                        
                        val response = httpClient.newCall(reqBuilder.build()).execute()
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            val modelsResp = modelsAdapter.fromJson(responseBody)
                            modelsResp?.data?.let { modelList ->
                                val entities = modelList.map { 
                                    AiModelEntity(providerId = provider.id, modelId = it.id) 
                                }
                                aiModelDao.deleteModelsForProvider(provider.id)
                                aiModelDao.insertModels(entities)
                            }
                        } else {
                            Log.e("AiManager", "Failed to fetch models for ${provider.id}: ${response.code} $responseBody")
                        }
                    } catch (e: Exception) {
                        Log.e("AiManager", "Exception fetching models for ${provider.id}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("AiManager", "Error in refreshModels", e)
            }
        }
    }

    fun addMockKey(providerId: String) {
        viewModelScope.launch {
            apiKeyDao.insertKey(
                ApiKeyEntity(
                    id = UUID.randomUUID().toString(),
                    providerId = providerId,
                    alias = "Test Key " + UUID.randomUUID().toString().take(4),
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
}
"""

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'w') as f:
    f.write(new_content)
