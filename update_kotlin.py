import re
path = 'app/src/main/java/com/example/engine/omniroot/local/LlamaEngine.kt'
with open(path, 'r') as f:
    content = f.read()

new_methods = """
    private var tokenListener: ((String) -> Unit)? = null

    // Called from C++ via JNI
    fun onTokenGenerated(token: String) {
        tokenListener?.invoke(token)
    }

    fun predictStream(prompt: String, listener: (String) -> Unit) {
        tokenListener = listener
        predictStreamNative(prompt)
        tokenListener = null // Cleanup after finish
    }

    private external fun predictStreamNative(prompt: String)
    external fun predict(prompt: String): String
    external fun unloadModel()
"""

# Replace existing external functions
if "    private external fun loadModel(path: String): Boolean" in content:
    idx = content.find("    external fun predict")
    if idx != -1:
        content = content[:idx] + new_methods.strip() + "\n}\n"

with open(path, 'w') as f:
    f.write(content)
print("Updated LlamaEngine.kt")
