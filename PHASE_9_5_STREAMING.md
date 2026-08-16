# 🛠️ SPECIAL MID-PHASE: Local AI Streaming Integration

### 📊 Current Status Audit
**✅ What IS Implemented (Already Working):**
* The Android File Picker UI imports massive `.gguf` files correctly.
* The system securely copies the file to the app's internal sandbox.
* The absolute file path is correctly saved to the Room database.
* The `CMakeLists.txt` is successfully configured to compile the C++ bridge.
* The Agent Brain (Phase 11) successfully formats the chat history.

**❌ What is NOT Implemented (Missing):**
* **The C++ Loop:** The C++ file just returns a hardcoded string. There is no mathematical sampling loop yet.
* **JNI Callbacks:** C++ cannot currently talk "up" to Kotlin during generation.
* **The Token Pipeline:** Kotlin has no `Flow` or `Listener` to catch individual words.
* **Direct Routing:** The `ChatViewModel` still routes local calls through the HTTP Proxy (which waits for the full text) instead of bypassing it for local streaming.
* **Real-time UI:** The chat screen doesn't know how to append text word-by-word.

---

### 🚀 The Execution Plan (Mini-Phases)

#### Mini-Phase 1: The C++ Brain (llama_bridge.cpp)
* **Goal:** Write the actual `llama_decode` and sampling math.
* **Action:** We will replace the dummy C++ code with the real token generation loop. Inside that loop, we will use JNI (`env->CallVoidMethod`) to fire every generated word back to Android instantly.

#### Mini-Phase 2: The Kotlin Pipeline (LlamaEngine.kt)
* **Goal:** Catch the words coming from C++.
* **Action:** We will add an interface (e.g., `TokenListener`) to the Kotlin engine. We will change the synchronous `predict` function into a reactive `predictStream()` function that yields a Kotlin `Flow<String>`.

#### Mini-Phase 3: The Direct Bypass (ChatViewModel.kt)
* **Goal:** Connect the Brain directly to the Chat without breaking Phase 11.
* **Action:** When you hit "Send", the Agent Manager will still format the prompt. But if the provider is `local_gguf`, we will SKIP the NanoHTTPD proxy. Instead, we will launch `LlamaEngine.predictStream()` directly in a coroutine to catch the words instantly.

#### Mini-Phase 4: The Typewriter UI (ChatScreen.kt)
* **Goal:** Make the text appear smoothly on screen.
* **Action:** Instead of waiting for the engine to finish, the UI will observe the incoming `Flow<String>`. As every single token arrives, we will append it (`text += newWord`) to the active Assistant chat bubble, triggering Jetpack Compose to redraw instantly just like SmolChat.
