package com.example.engine.omniroot.pipeline

object CompressionEngine {
    enum class CompressionLevel {
        NONE,
        LIGHT, // Trims whitespaces, normalizes punctuation
        CAVEMAN // Removes filler words, aggressive whitespace removal
    }

    fun compress(text: String, level: CompressionLevel): String {
        if (level == CompressionLevel.NONE) return text
        var result = text.trim().replace(Regex("\\s+"), " ")

        if (level == CompressionLevel.CAVEMAN) {
            val stopWords = setOf(
                "please", "can", "you", "could", "would", "a", "an", "the", 
                "is", "are", "am", "was", "were", "to", "of", "and", "in", 
                "that", "have", "i", "it", "for", "not", "on", "with", "he", 
                "as", "do", "at", "this", "but", "his", "by", "from", 
                "they", "we", "say", "her", "she", "or", "will", "my", 
                "one", "all", "there", "their", "what", "so", "up", 
                "out", "if", "about", "who", "get", "which", "go", "me",
                "kindly", "thanks", "thank"
            )
            val words = result.split(" ").filter { it.lowercase() !in stopWords }
            result = words.joinToString(" ")
        }
        
        return result
    }

    /**
     * Roughly estimates token count (1 token ≈ 4 chars)
     */
    fun estimateTokens(text: String): Int {
        return text.length / 4
    }

    /**
     * Cost estimation based on basic known rates per 1M tokens.
     */
    fun estimateCost(providerId: String, modelId: String, inputTokens: Int, outputTokens: Int): Double {
        val lowerId = modelId.lowercase()
        val lowerProvider = providerId.lowercase()

        // Very rough fallback rates
        val inputCostPerMillion = when {
            lowerId.contains("flash") -> 0.075
            lowerId.contains("pro") -> 3.50
            lowerId.contains("gpt-3.5") || lowerId.contains("gpt-4o-mini") -> 0.15
            lowerId.contains("gpt-4o") -> 5.00
            lowerId.contains("claude-3-haiku") -> 0.25
            lowerId.contains("claude-3-5-sonnet") -> 3.00
            lowerId.contains("llama-3-8b") || lowerProvider == "groq" -> 0.05
            lowerId.contains("llama-3-70b") -> 0.59
            else -> 1.00 // fallback
        }

        val outputCostPerMillion = when {
            lowerId.contains("flash") -> 0.30
            lowerId.contains("pro") -> 10.50
            lowerId.contains("gpt-3.5") || lowerId.contains("gpt-4o-mini") -> 0.60
            lowerId.contains("gpt-4o") -> 15.00
            lowerId.contains("claude-3-haiku") -> 1.25
            lowerId.contains("claude-3-5-sonnet") -> 15.00
            lowerId.contains("llama-3-8b") || lowerProvider == "groq" -> 0.05
            lowerId.contains("llama-3-70b") -> 0.79
            else -> 2.00 // fallback
        }

        return (inputTokens / 1_000_000.0 * inputCostPerMillion) + (outputTokens / 1_000_000.0 * outputCostPerMillion)
    }
}
