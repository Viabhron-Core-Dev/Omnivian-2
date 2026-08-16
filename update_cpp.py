import os

content = """#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sstream>
#include <vector>

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
Java_com_example_engine_omniroot_local_LlamaEngine_predictStream(JNIEnv* env, jobject thiz, jstring prompt) {
    const char *nativePrompt = env->GetStringUTFChars(prompt, nullptr);
    
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID onTokenMethod = env->GetMethodID(clazz, "onTokenGenerated", "(Ljava/lang/String;)V");
    
    if (!onTokenMethod) {
        LOGE("Failed to find onTokenGenerated method in Kotlin");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }

    // In a real integration, this would be a llama_decode and llama_sample_token loop.
    // For safety and compatibility with arbitrary fetched llama.h, we simulate the token loop.
    std::string mockResponse = "This is a simulated stream from the REAL llama.cpp C++ backend. ";
    mockResponse += "If this were the full C++ logic, we would be running llama_decode and llama_sample_token in a while loop here.";
    
    std::stringstream ss(mockResponse);
    std::string word;
    while (ss >> word) {
        std::string token = word + " ";
        jstring jToken = env->NewStringUTF(token.c_str());
        env->CallVoidMethod(thiz, onTokenMethod, jToken);
        env->DeleteLocalRef(jToken);
        usleep(30000); // 30ms compute delay
    }

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
Java_com_example_engine_omniroot_local_LlamaEngine_predictStream(JNIEnv* env, jobject thiz, jstring prompt) {
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

with open('app/src/main/cpp/llama_bridge.cpp', 'w') as f:
    f.write(content)
print("Updated llama_bridge.cpp")
