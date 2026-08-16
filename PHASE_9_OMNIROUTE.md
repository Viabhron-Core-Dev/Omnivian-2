# Phase 9: Native Android AI Manager & Router

## Context & Goal
The previous OmniRoute (Node.js/Playwright) approach is too heavy and prone to crashes on low-end devices (like Android 15 Go with 3GB RAM). Termux is aggressively killed, and heavy browser automation drains battery and memory. Furthermore, silent session harvesting is unstable and easily blocked on Android WebViews.

This revised phase implements a **pure-Kotlin, lightweight Native AI Manager & Router** inspired by OmniRoute. It replicates the core value of OmniRoute (Agent Shifting, Token Monitoring, centralized provider list, universal payload translation, and combo routing) but replaces silent extraction with a highly stable, guided "Direct-to-Key" WebView approach, and introduces native local `.gguf` inference.

## Granular Mini-Phases

### Phase 9.1: Core Data Layer (Room Database)
* **Objective:** Establish the persistent storage for providers, keys, metrics, and ratings.
* **Tasks:**
  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, `TokenUsage`, `ModelRating`, and `RequestLog` (for request counts and cost tracking).
  * Ensure `ApiKey` supports multiple distinct keys/accounts per `ApiProvider` (e.g., multiple Google accounts for Google AI Studio).
  * Include an `alias` field in the `ApiKey` entity for user-friendly naming (e.g., "Personal", "Work").
  * Pre-populate the database with a massive directory of known AI providers (Google AI Studio, OpenAI, Anthropic, OpenRouter, Groq, Together AI, Mistral, Cohere, DeepSeek, Fireworks, Perplexity, Anyscale, Replicate, HuggingFace, Novita, etc.) and their API deep links.

### Phase 9.2: AI Manager Panel UI (Professional Dashboard)
* **Objective:** Build the main management interface with a professional, tabbed layout inspired by OmniRoute.
* **Tasks:**
  * Create a Jetpack Compose dashboard screen in the Global Settings with the following tabs:
    * **Directory Tab:** The massive list of all available AI providers. Provides deep links to their login pages for Phase 9.3 integration.
    * **Active Keys Tab:** Shows configured accounts. Allows multiple accounts per provider. Includes separated "Local AI" instances (which can also have multiple configured endpoints/GGUFs). **Privacy/Security UI:** Never display the raw API key text; use strict masking (`sk-...abcd`) and aliases.
    * **Available Models Tab:** Aggregates and lists all models currently available through the active keys.
    * **Metrics & Token Counter Tab:** Visualizes token usage across models and providers, total request counts, and estimated total costs (pulling from `TokenUsage` and `RequestLog`).
    * **Model Rater Tab:** Displays aggregated stats (thumbs up/down) to judge the overall strength of different AI models (pulling from `ModelRating`).
    * **Translator Playground Tab:** A debugging UI to visualize how API requests are translated between formats (e.g., OpenAI to Claude).

### Phase 9.3: Secure Guided WebView (Direct-to-Key)
* **Objective:** Secure, user-guided API key extraction.
* **Tasks:**
  * Implement the custom, secure Android `WebView` (HTTPS-only, mixed content disabled, Safe Browsing enabled).
  * Load the deep links to provider API pages when a provider is selected in the AI Manager Panel.
  * Provide the "Paste & Save" UI overlay to securely capture the user-generated key without silent cookie harvesting.

### Phase 9.4: Token Checker & Contextual AI Panel
* **Objective:** Track and display real-time API usage within the chat workspace.
* **Tasks:**
  * Implement the calculation logic for token consumption and rate limiting windows (e.g., RPM).
  * Build the **AI Manager Token Panel** (accessible via the Chat's 3-dot menu) as a bottom sheet.
  * Display a visual list of active models, token health, remaining limits, total request counts, and estimated total costs.

### Phase 9.5: Core Routing & Combo Engine (Agent Shifter)
* **Objective:** Implement the outbound HTTP interceptor, routing strategies, and failover logic.
* **Tasks:**
  * Implement advanced routing strategies: Priority Lists (forced ordering) and Combo Routing (automatic selection for speed, cost, or quality).
  * Implement the interceptor: Catch `429 Too Many Requests`, `401 Unauthorized`, or `5xx Server Error`.
  * Create the quota-aware retry mechanism that instantly shifts the request to the next provider/key in the FallbackChain upon failure or limit exhaustion.

### Phase 9.6: Universal Payload Translation & Compression Pipeline
* **Objective:** Handle LLM API "format chaos" and optimize token usage before sending requests upstream.
* **Tasks:**
  * **Payload Translation:** Build a translation layer that accepts a unified OpenAI-compatible payload and translates it into provider-specific formats (e.g., Anthropic Messages API, Gemini API, etc.) and translates responses back to the OpenAI format.
  * **Compression Pipeline:** Implement a lightweight local token compression algorithm (inspired by OmniRoute's RTK/Caveman engines) that strips repetitive or verbose output from payloads before translation to save 15-95% of tokens.

### Phase 9.7: The Unkillable Local Proxy (Foreground Service)
* **Objective:** Expose the unified routing and translation engine to the system via `localhost`.
* **Tasks:**
  * Create an Android Foreground Service with a persistent notification ("AI Manager Active on Port 8080").
  * Implement a lightweight local HTTP server (NanoHTTPD or Ktor Server) listening on `localhost:8080`.
  * Pipe incoming OpenAI-compatible requests from the local server directly into the Phase 9.6 Translation & Phase 9.5 Routing pipeline.

### Phase 9.8: GGUF File Management & SAF Integration
* **Objective:** Allow the user to select local `.gguf` models securely.
* **Tasks:**
  * Add a "Local GGUF Model" provider option in the AI Manager Panel.
  * Implement the Android Storage Access Framework (SAF) file picker so users can select pre-downloaded `.gguf` files from their device.
  * Persist the file URI permissions securely in the app.

### Phase 9.9: llama.cpp JNI Wrapper Foundation
* **Objective:** Set up the C++ integration for local inference.
* **Tasks:**
  * Configure CMake and the NDK build in `app/build.gradle.kts`.
  * Import a lightweight JNI wrapper for `llama.cpp` (using architecture similar to SmolChat-Android).
  * Compile the native libraries specifically for `arm64-v8a` to optimize for the target devices.

### Phase 9.10: Local Inference Engine Initialization
* **Objective:** Load the model into memory safely.
* **Tasks:**
  * Build the Kotlin bridging classes to initialize the `llama.cpp` engine with the selected `.gguf` file.
  * Enforce strict memory limits (context sizes, layer offloading logic) to prevent Out-Of-Memory (OOM) crashes on 3GB RAM limits.

### Phase 9.11: End-to-End Routing Validation
* **Objective:** Connect the cloud and local systems seamlessly.
* **Tasks:**
  * Update the Agent Shifter to intercept requests destined for the "Local GGUF" provider.
  * Route these specific requests to the in-memory Phase 9.10 engine rather than making a network call.
  * Test the seamless fallback: e.g., Cloud API -> Rate Limited -> Fallback to Local GGUF Model.

### Phase 9.12: Chat Rating Integration
* **Objective:** Allow users to rate AI responses to feed the Model Rater Tab.
* **Tasks:**
  * Add Thumbs Up and Thumbs Down buttons to AI message bubbles in the Chat View.
  * On click, log the rating to the `ModelRating` table, associating it with the specific model that generated the response.
