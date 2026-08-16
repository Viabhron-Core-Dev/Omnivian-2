import re
path = 'app/src/main/cpp/llama_bridge.cpp'
with open(path, 'r') as f:
    content = f.read()

new_content = """#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sstream>
#include <vector>
#include <algorithm>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef USE_REAL_LLAMA
#include "llama.h"

llama_model *model = nullptr;
llama_context *ctx = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_loadModel(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("Loading real GGUF model: %s", nativePath);
    
    llama_backend_init();
    
    llama_model_params model_params = llama_model_default_params();
    model = llama_load_model_from_file(nativePath, model_params);
    
    if (model == nullptr) {
        LOGE("Failed to load model");
        env->ReleaseStringUTFChars(path, nativePath);
        return JNI_FALSE;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048; 
    ctx = llama_new_context_with_model(model, ctx_params);
    
    env->ReleaseStringUTFChars(path, nativePath);
    return ctx != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_predictStreamNative(JNIEnv* env, jobject thiz, jstring prompt) {
    const char *nativePrompt = env->GetStringUTFChars(prompt, nullptr);
    
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID onTokenMethod = env->GetMethodID(clazz, "onTokenGenerated", "(Ljava/lang/String;)V");
    
    if (!onTokenMethod) {
        LOGE("Failed to find onTokenGenerated method in Kotlin");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }

    if (!model || !ctx) {
        LOGE("Model or Context is null");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }

    const struct llama_vocab * vocab = llama_model_get_vocab(model);
    
    // Tokenize
    int32_t prompt_len = strlen(nativePrompt);
    int32_t n_prompt_tokens_max = prompt_len + 128;
    std::vector<llama_token> prompt_tokens(n_prompt_tokens_max);
    
    int32_t n_prompt_tokens = llama_tokenize(vocab, nativePrompt, prompt_len, prompt_tokens.data(), prompt_tokens.size(), true, true);
    
    if (n_prompt_tokens < 0) {
        prompt_tokens.resize(-n_prompt_tokens);
        n_prompt_tokens = llama_tokenize(vocab, nativePrompt, prompt_len, prompt_tokens.data(), prompt_tokens.size(), true, true);
        if (n_prompt_tokens < 0) {
            LOGE("Failed to tokenize");
            env->ReleaseStringUTFChars(prompt, nativePrompt);
            return;
        }
    }
    prompt_tokens.resize(n_prompt_tokens);
    
    // Initialize sampler
    struct llama_sampler * smpl = llama_sampler_init_greedy();

    // Prepare batch
    llama_batch batch = llama_batch_init(std::max(1024, (int)prompt_tokens.size()), 0, 1);
    
    batch.n_tokens = prompt_tokens.size();
    for (size_t i = 0; i < prompt_tokens.size(); i++) {
        batch.token[i] = prompt_tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == prompt_tokens.size() - 1) ? 1 : 0;
    }
    
    if (llama_decode(ctx, batch) != 0) {
        LOGE("llama_decode failed");
        llama_batch_free(batch);
        llama_sampler_free(smpl);
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }
    
    int n_past = prompt_tokens.size();
    int n_predict = 1024; // Max output tokens
    
    for (int i = 0; i < n_predict; i++) {
        // Sample token
        llama_token id = llama_sampler_sample(smpl, ctx, -1);
        llama_sampler_accept(smpl, id);
        
        // Convert to string
        char buf[128] = {0};
        int32_t n = llama_token_to_piece(vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            jstring jToken = env->NewStringUTF(buf);
            env->CallVoidMethod(thiz, onTokenMethod, jToken);
            env->DeleteLocalRef(jToken);
        }
        
        // Check for end of generation
        if (llama_vocab_is_eog(vocab, id)) {
            break;
        }
        
        // Prepare next batch
        batch.n_tokens = 1;
        batch.token[0] = id;
        batch.pos[0] = n_past;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        
        if (llama_decode(ctx, batch) != 0) {
            LOGE("llama_decode failed during generation");
            break;
        }
        
        n_past++;
    }
    
    llama_batch_free(batch);
    llama_sampler_free(smpl);
    env->ReleaseStringUTFChars(prompt, nativePrompt);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_predict(JNIEnv* env, jobject, jstring prompt) {
    return env->NewStringUTF("Legacy predict called.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_unloadModel(JNIEnv* env, jobject) {
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_free_model(model); model = nullptr; }
    llama_backend_free();
}

#else // MOCK IMPLEMENTATION FOR FAST LOCAL BUILDS

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_loadModel(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("[MOCK] Simulating load for GGUF model: %s", nativePath);
    env->ReleaseStringUTFChars(path, nativePath);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_predictStreamNative(JNIEnv* env, jobject thiz, jstring prompt) {
    const char *nativePrompt = env->GetStringUTFChars(prompt, nullptr);
    LOGI("[MOCK] Simulating streaming inference for prompt: %s", nativePrompt);
    
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID onTokenMethod = env->GetMethodID(clazz, "onTokenGenerated", "(Ljava/lang/String;)V");
    
    if (!onTokenMethod) {
        LOGE("Failed to find onTokenGenerated method in Kotlin");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }
    
    std::string response = "I am a local AI running completely offline via llama.cpp on your device! This text is streaming token-by-token directly from the C++ layer via JNI.";
    
    std::stringstream ss(response);
    std::string word;
    while (ss >> word) {
        std::string token = word + " ";
        jstring jToken = env->NewStringUTF(token.c_str());
        env->CallVoidMethod(thiz, onTokenMethod, jToken);
        env->DeleteLocalRef(jToken);
        usleep(50000); // 50ms compute delay
    }
    env->ReleaseStringUTFChars(prompt, nativePrompt);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_predict(JNIEnv* env, jobject, jstring prompt) {
    return env->NewStringUTF("Legacy predict called.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_unloadModel(JNIEnv* env, jobject) {
    LOGI("[MOCK] Simulating model unload.");
}

#endif
"""
with open(path, 'w') as f:
    f.write(new_content)
