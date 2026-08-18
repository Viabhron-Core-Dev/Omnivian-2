package com.example.ai;

import android.os.Bundle;
import com.example.ai.IAiCallback;

interface IAiInferenceService {
    void streamPrompt(String modelId, String systemPrompt, String userPrompt, in Bundle options, IAiCallback callback);
    void cancelInference(String requestId);
    List<String> getAvailableModels();
}
