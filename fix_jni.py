import re

path = 'app/src/main/cpp/llama_bridge.cpp'
with open(path, 'r') as f:
    content = f.read()

# Replace the macros
macro_replacement = """#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
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
"""

content = content.replace(
    '#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)\n#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)',
    macro_replacement
)

# And now we hook `llama_log_set` during `llama_backend_init` but wait, `llama_log_set` takes `(ggml_log_level level, const char * text, void * user_data)`.
hook_replacement = """
static void native_log_callback(ggml_log_level level, const char * text, void * user_data) {
    const char* levelStr = "INFO";
    if (level == GGML_LOG_LEVEL_ERROR) levelStr = "ERROR";
    else if (level == GGML_LOG_LEVEL_WARN) levelStr = "WARN";

    std::string msg = text;
    if (!msg.empty() && msg.back() == '\\n') msg.pop_back();

    sendLogToKotlin(levelStr, "%s", msg.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
"""

content = content.replace('extern "C" JNIEXPORT jboolean JNICALL', hook_replacement, 1)

llama_backend_init_replacement = """
    llama_log_set(native_log_callback, nullptr);
    llama_backend_init();
"""

content = content.replace('    llama_backend_init();', llama_backend_init_replacement)

with open(path, 'w') as f:
    f.write(content)

