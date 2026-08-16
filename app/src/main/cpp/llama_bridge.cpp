#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sstream>
#include <vector>
#include <algorithm>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_jvm = nullptr;
static jclass g_llamaEngineClass = nullptr;
static jmethodID g_onNativeLogMethod = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass clazz = env->FindClass("com/example/engine/omniroot/local/LlamaEngine");
    if (clazz) {
        g_llamaEngineClass = (jclass)env->NewGlobalRef(clazz);
        g_onNativeLogMethod = env->GetStaticMethodID(g_llamaEngineClass, "onNativeLog", "(Ljava/lang/String;Ljava/lang/String;)V");
    }
    return JNI_VERSION_1_6;
}

void sendLogToKotlin(const char* level, const char* format, ...) {
    if (!g_jvm || !g_onNativeLogMethod) return;
    JNIEnv* env;
    bool attached = false;
    int status = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) != 0) return;
        attached = true;
    } else if (status == JNI_EVERSION) {
        return;
    }

    char buffer[1024];
    va_list args;
    va_start(args, format);
    vsnprintf(buffer, sizeof(buffer), format, args);
    va_end(args);

    jstring jLevel = env->NewStringUTF(level);
    jstring jMsg = env->NewStringUTF(buffer);

    env->CallStaticVoidMethod(g_llamaEngineClass, g_onNativeLogMethod, jLevel, jMsg);

    env->DeleteLocalRef(jLevel);
    env->DeleteLocalRef(jMsg);

    if (attached) {
        g_jvm->DetachCurrentThread();
    }
}

#undef LOGI
#undef LOGE
#define LOGI(...) do { __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__); sendLogToKotlin("INFO", __VA_ARGS__); } while(0)
#define LOGE(...) do { __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__); sendLogToKotlin("ERROR", __VA_ARGS__); } while(0)


#ifdef USE_REAL_LLAMA
#include "llama.h"

llama_model *model = nullptr;
llama_context *ctx = nullptr;


static void native_log_callback(ggml_log_level level, const char * text, void * user_data) {
    const char* levelStr = "INFO";
    if (level == GGML_LOG_LEVEL_ERROR) levelStr = "ERROR";
    else if (level == GGML_LOG_LEVEL_WARN) levelStr = "WARN";

    std::string msg = text;
    if (!msg.empty() && msg.back() == '\n') msg.pop_back();

    sendLogToKotlin(levelStr, "%s", msg.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_loadModel(
    JNIEnv* env, jobject, jstring path, jint contextSize, jint numThreads, jboolean useMmap, jboolean useMlock
) {
    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("Loading real GGUF model: %s (ctx=%d, threads=%d, mmap=%d, mlock=%d)", 
         nativePath, contextSize, numThreads, useMmap ? 1 : 0, useMlock ? 1 : 0);

    llama_log_set(native_log_callback, nullptr);
    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = (useMmap == JNI_TRUE);
    model_params.use_mlock = (useMlock == JNI_TRUE);

    model = llama_model_load_from_file(nativePath, model_params);
    
    if (model == nullptr) {
        LOGE("Failed to load model");
        env->ReleaseStringUTFChars(path, nativePath);
        return JNI_FALSE;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (contextSize > 0) ? contextSize : 2048;
    
    int cpu_cores = sysconf(_SC_NPROCESSORS_ONLN);
    int optimal_threads = (numThreads > 0) ? numThreads : std::max(2, std::min(4, cpu_cores > 2 ? cpu_cores - 2 : cpu_cores));
    ctx_params.n_threads = optimal_threads;
    ctx_params.n_threads_batch = optimal_threads;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 256;

    LOGI("Configured llama_context with %d threads, %d ctx (CPU cores: %d)", optimal_threads, ctx_params.n_ctx, cpu_cores);
    ctx = llama_init_from_model(model, ctx_params);
    
    env->ReleaseStringUTFChars(path, nativePath);
    return ctx != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_predictStreamNative(
    JNIEnv* env, jobject thiz, jstring prompt, jfloat temperature, jfloat minP, jfloat topP, jint maxTokens
) {
    const char *nativePrompt = env->GetStringUTFChars(prompt, nullptr);
    
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID onTokenMethod = env->GetMethodID(clazz, "onTokenGenerated", "(Ljava/lang/String;)V");
    
    auto sendError = [&](const std::string& err) {
        if (onTokenMethod) {
            jstring jErr = env->NewStringUTF(err.c_str());
            env->CallVoidMethod(thiz, onTokenMethod, jErr);
            env->DeleteLocalRef(jErr);
        }
        LOGE("%s", err.c_str());
    };

    if (!onTokenMethod) {
        LOGE("Failed to find onTokenGenerated method in Kotlin");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }

    if (!model || !ctx) {
        sendError("\n[ERROR: Model or Context is null!]\n");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }

    // Reset KV cache so the new prompt evaluates from position 0 cleanly
    llama_kv_cache_clear(ctx);

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
            sendError("\n[ERROR: Failed to tokenize prompt]\n");
            env->ReleaseStringUTFChars(prompt, nativePrompt);
            return;
        }
    }
    prompt_tokens.resize(n_prompt_tokens);
    
    uint32_t ctx_size = llama_n_ctx(ctx);
    if (prompt_tokens.size() > ctx_size - 4) {
        sendError("\n[ERROR: Prompt exceeds context size]\n");
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }

    // Initialize sampler chain (Min-P, Top-P, Temperature)
    struct llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    struct llama_sampler * smpl = llama_sampler_chain_init(sparams);
    
    if (minP > 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_min_p(minP, 1));
    }
    if (topP > 0.0f && topP < 1.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    }
    if (temperature > 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    }

    // Prepare batch
    llama_batch batch = llama_batch_init(std::max((uint32_t)512, (uint32_t)prompt_tokens.size()), 0, 1);
    
    batch.n_tokens = prompt_tokens.size();
    for (size_t i = 0; i < prompt_tokens.size(); i++) {
        batch.token[i] = prompt_tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == prompt_tokens.size() - 1) ? 1 : 0;
    }
    
    if (llama_decode(ctx, batch) != 0) {
        sendError("\n[ERROR: llama_decode failed on initial prompt]\n");
        llama_batch_free(batch);
        llama_sampler_free(smpl);
        env->ReleaseStringUTFChars(prompt, nativePrompt);
        return;
    }
    
    int n_past = prompt_tokens.size();
    int n_predict = (maxTokens > 0) ? maxTokens : 1024;
    
    for (int i = 0; i < n_predict; i++) {
        if (n_past >= ctx_size) {
            sendError("\n[WARNING: Context size reached, stopping]\n");
            break;
        }

        // Sample token
        llama_token id = llama_sampler_sample(smpl, ctx, -1);
        llama_sampler_accept(smpl, id);
        
        // Check for end of generation BEFORE sending text
        if (llama_vocab_is_eog(vocab, id)) {
            break;
        }
        
        // Convert to string
        char buf[128] = {0};
        int32_t n = llama_token_to_piece(vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            jstring jToken = env->NewStringUTF(buf);
            env->CallVoidMethod(thiz, onTokenMethod, jToken);
            env->DeleteLocalRef(jToken);
        }
        
        // Prepare next batch
        batch.n_tokens = 1;
        batch.token[0] = id;
        batch.pos[0] = n_past;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        
        if (llama_decode(ctx, batch) != 0) {
            sendError("\n[ERROR: llama_decode failed during generation]\n");
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
    if (model) { llama_model_free(model); model = nullptr; }
    llama_backend_free();
}

#else // MOCK IMPLEMENTATION FOR FAST LOCAL BUILDS

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_loadModel(
    JNIEnv* env, jobject, jstring path, jint contextSize, jint numThreads, jboolean useMmap, jboolean useMlock
) {
    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("[MOCK] Simulating load for GGUF model: %s (ctx=%d, threads=%d, mmap=%d, mlock=%d)", 
         nativePath, contextSize, numThreads, useMmap ? 1 : 0, useMlock ? 1 : 0);
    env->ReleaseStringUTFChars(path, nativePath);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_omniroot_local_LlamaEngine_predictStreamNative(
    JNIEnv* env, jobject thiz, jstring prompt, jfloat temperature, jfloat minP, jfloat topP, jint maxTokens
) {
    const char *nativePrompt = env->GetStringUTFChars(prompt, nullptr);
    LOGI("[MOCK] Simulating streaming inference (temp=%.2f, minP=%.2f, topP=%.2f, maxTokens=%d) for prompt: %s", 
         temperature, minP, topP, maxTokens, nativePrompt);
    
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
