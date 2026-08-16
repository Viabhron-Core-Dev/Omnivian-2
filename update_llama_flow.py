import re
path = 'app/src/main/java/com/example/engine/omniroot/local/LlamaEngine.kt'
with open(path, 'r') as f:
    content = f.read()

imports = """import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
"""

if "import kotlinx.coroutines.flow.Flow" not in content:
    content = content.replace("import java.io.File", "import java.io.File\n" + imports)

new_flow_method = """
    fun predictFlow(prompt: String): Flow<String> = callbackFlow {
        // 1. Assign the JNI listener to push words into the Flow pipe
        tokenListener = { token ->
            trySend(token)
        }

        // 2. Launch the heavy C++ math in a background thread so the UI doesn't freeze
        launch(Dispatchers.IO) {
            predictStreamNative(prompt)
            // 3. When C++ finishes the loop, close the pipe
            close()
        }

        // 4. Cleanup if the user hits "Stop" and cancels the coroutine
        awaitClose {
            tokenListener = null
        }
    }
"""

if "fun predictFlow" not in content:
    content = content.replace("    private external fun predictStreamNative(prompt: String)", new_flow_method.strip() + "\n\n    private external fun predictStreamNative(prompt: String)")

with open(path, 'w') as f:
    f.write(content)
print("Updated LlamaEngine.kt with Flow")
