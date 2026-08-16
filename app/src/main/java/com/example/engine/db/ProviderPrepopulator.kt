package com.example.engine.db

object ProviderPrepopulator {
    val defaultProviders = listOf(
        ApiProviderEntity(
            id = "google_ai_studio",
            name = "Google AI Studio",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
            loginUrl = "https://aistudio.google.com/app/apikey",
            isFreeTierAvailable = true,
            description = "Access to Gemini models with a generous free tier."
        ),
        ApiProviderEntity(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            loginUrl = "https://platform.openai.com/api-keys",
            isFreeTierAvailable = false,
            description = "Access to GPT-4o, GPT-3.5, and more."
        ),
        ApiProviderEntity(
            id = "anthropic",
            name = "Anthropic",
            baseUrl = "https://api.anthropic.com/v1",
            loginUrl = "https://console.anthropic.com/settings/keys",
            isFreeTierAvailable = false,
            description = "Access to Claude 3 models."
        ),
        ApiProviderEntity(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            loginUrl = "https://openrouter.ai/keys",
            isFreeTierAvailable = true,
            description = "Aggregator for many models with free tier options."
        ),
        ApiProviderEntity(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            loginUrl = "https://console.groq.com/keys",
            isFreeTierAvailable = true,
            description = "Ultra-fast inference for Llama, Mixtral, and Gemma models."
        ),
        ApiProviderEntity(
            id = "together_ai",
            name = "Together AI",
            baseUrl = "https://api.together.xyz/v1",
            loginUrl = "https://api.together.xyz/settings/api-keys",
            isFreeTierAvailable = true,
            description = "Open source models with fast inference."
        ),
        ApiProviderEntity(
            id = "local_gguf",
            name = "Local GGUF (llama.cpp)",
            baseUrl = "localhost:8080",
            loginUrl = "",
            isFreeTierAvailable = true,
            description = "Run models directly on your device using llama.cpp."
        )
    )
}
