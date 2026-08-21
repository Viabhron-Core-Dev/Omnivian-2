# Phase 10: Tool Calling Engine, Multi-Sandboxes, Failover Routing, Document Parsers & MCP Ecosystem

## 1. Architectural Vision & Integration with Current App

Phase 10 transforms OmniRoot from a dual Cloud/Local LLM interface into a **fully autonomous agentic execution runtime**. The tool ecosystem integrates directly into the existing architecture without breaking previous phases:

```
                                  [ChatScreen.kt / Workspace]
                                               │
                                               ▼
                              [Agent Prompt + Tool Registry Schemas]
                                               │
                       ┌───────────────────────┴───────────────────────┐
                       ▼                                               ▼
              [Cloud Proxy Router]                           [Local GGUF Engine]
           (OmniRootProxyServer.kt)                           (LlamaEngine.kt)
                       │                                               │
                       └───────────────────────┬───────────────────────┘
                                               │ Emits Tool Call(s) (JSON / XML)
                                               ▼
                                  [Tool Calling Orchestrator]
                                               │
               ┌───────────────────────┬───────┴───────────────┬───────────────────────┐
               ▼                       ▼                       ▼                       ▼
     [Execution Sandboxes]    [Workspace & Files]    [Documents & Archives]    [MCP Client Bridge]
     • JS Sandbox (V8)        • Surgical edit_file   • Zip / Unzip Engine      • JSON-RPC 2.0 (SSE)
     • Shell Process Runner   • Recursive grep/tree  • PDF Text (PdfRenderer)  • Dynamic tools/list
     • PRoot Linux Rootfs     • Read/Write/Delete    • PPTX/DOCX Parser        • Remote & Local MCP
               │                       │                       │                       │
               └───────────────────────┴───────┬───────────────┴───────────────────────┘
                                               │
                                               ▼
                                 [Interactive Artifact Viewer]
                                 • 3D GLB/glTF (<model-viewer> / Three.js)
                                 • Live HTML5 / WebGL / Canvas Preview
                                               │
                                               ▼
                                  [LogKeeper Telemetry Hub]
                                  • Full tool timing, args & stdout logs
                                               │
                                               ▼
                              [ChatMessage(role = Tool) Injected]
                                               │
                                               ▼
                            [LLM Synthesizes Final Chat Turn]
```

---

## 2. Comprehensive Tool & Sandbox Inventory

### A. Execution & Compute Sandboxes

#### 1. Isolated JavaScript Sandbox (`JsSandboxTool`)
* **How It Works**: Uses Android Jetpack's official `androidx.javascriptengine:javascriptengine` (or QuickJS embedded C/JNI engine).
* **Isolation**: Zero filesystem access, zero network access, zero Android context exposure.
* **Capabilities**:
  * Execute pure mathematical calculations, data formatting, algorithmic simulations, and JSON transformations.
  * Captures `console.log` output and return values.
  * Enforces a 5,000ms hard timeout and 64MB memory heap cap to prevent runaway loops.

#### 2. Native Scoped Process Runner (`ShellProcessTool`)
* **How It Works**: Wraps Java `ProcessBuilder` tightly scoped to `/data/user/0/shura.omnivian/files/workspace`.
* **Security & Sanitization**:
  * Sanitizes `PATH` and environment variables.
  * Prevents directory traversal outside the active workspace directory.
  * Streams non-blocking `stdout` and `stderr` asynchronously with a 15-second execution deadline.

#### 3. Zero-Root PRoot Linux Environment (`PRootLinuxTool`)
* **How It Works**: Leverages user-space `ptrace` system call interception (`proot`) bundled into native library storage.
* **Architecture**:
  * Unpacks an ultra-lightweight rootfs (such as Alpine Linux ~5MB compressed) inside private app storage.
  * Intercepts `chroot`, `mount`, and `setuid` syscalls in userland—requiring **zero root permissions**.
* **Capabilities**:
  * Run **Python 3** scripts (`numpy`, `pandas`, `sympy`, math, file analysis).
  * Run **Node.js** and CLI tools (`bash`, `git`, `curl`, `awk`, `sed`).
  * Package manager support (`apk add`) for dynamic toolchain installation.

---

### B. Workspace & File System Tools (Deterministic JSON Architecture)

Following the **Google AI Studio deterministic tool model**, all workspace modifications are executed via high-speed, zero-RAM Kotlin native JSON tools rather than unstable shell commands (`sed`, `cat`, `awk`). This eliminates shell escaping syntax bugs, prevents token bloat, and provides sub-millisecond execution on low-memory devices (Android 15 Go / 3GB RAM):

1. **`view_file(path, startLine, endLine)`**:
   * **Token Optimization**: Supports precise 1-indexed line slicing (`startLine` to `endLine`) to prevent dumping massive files into context.
   * Boundary checks with automatic capping to maximum safe slice size.
2. **`edit_file(path, targetContent, replacementContent)`**:
   * **Surgical Search-and-Replace**: Finds the exact `targetContent` substring in memory and swaps it for `replacementContent`.
   * **Uniqueness Validation**: Fails safely if `targetContent` matches 0 or >1 occurrences, forcing the model to provide unique anchor context and eliminating accidental code corruption.
   * **Token Cost**: Only costs ~100–300 tokens per edit instead of regenerating entire multi-thousand-line files.
3. **`multi_edit_file(path, replacementChunks)`**:
   * Accepts an array of non-contiguous `{targetContent, replacementContent}` pairs to perform multiple surgical updates in a single LLM turn.
4. **`create_file(path, content, overwrite)`**:
   * UTF-8 safe write with automatic parent directory creation and overwrite safety guards.
5. **`delete_file(path)`** & **`move_file(sourcePath, destPath)`**:
   * Guarded filesystem manipulation with path traversal prevention.
6. **`list_dir(path, recursive)`**:
   * Formatted directory tree with file sizes and directory indicators.
7. **`grep_search(query, path, filePattern)`**:
   * Fast recursive substring and regex search across workspace source files, skipping binary and build directories without spawning terminal processes.

---

### C. Archive & Document Manipulation Tools

#### 1. Archive Engine (`ArchiveTool`)
* **Native Zip / Unzip**:
  * `create_zip(sourceDir, outputZipPath)`: Streams files into `.zip` archives via `ZipOutputStream`.
  * `extract_zip(zipPath, destinationDir)`: Extracts with Path Traversal / Zip Slip protection (validating target canonical paths).
  * `list_zip(zipPath)`: Inspects archive hierarchy and byte sizes without extracting to disk.

#### 2. Document Text Parsers (`DocumentParserTool`)
* **PDF Text Extractor**: Uses Android native `android.graphics.pdf.PdfRenderer` and stream text extraction to extract structural text and page layouts for LLM context.
* **Office Document Parsers (PPTX / DOCX)**:
  * Word (.docx) and PowerPoint (.pptx) are OpenXML zip archives.
  * Uses lightweight `XmlPullParser` on `word/document.xml` and `ppt/slides/slide*.xml` to extract paragraphs, headings, slide notes, and tables with zero heavy third-party bloat.

---

### D. 3D & Interactive Artifact Viewer

When agents create 3D assets or interactive web prototypes, OmniRoot provides live visual execution:

1. **3D Model Viewer (`<model-viewer>` / Three.js Bridge)**:
   * Embedded hardware-accelerated WebView composable leveraging Google's `<model-viewer>`.
   * Live preview of `.glb` and `.gltf` 3D models with touch rotation, pinch-zoom, HDR lighting, and wireframe toggle.
2. **Interactive HTML5 / WebGL / Canvas Preview**:
   * Renders live interactive artifacts generated by the agent (charts, canvas simulations, diagrams) with bidirectional communication.

---

### E. Web Search & Reader Mode Tools

1. **`web_search(query, maxResults)`**:
   * Scrapes structured search results (Title, Snippet, URL) via lightweight DuckDuckGo HTML / SearXNG endpoints.
2. **`web_fetch(url)`**:
   * Performs an asynchronous HTTP GET, parses DOM via Jsoup, removes ads/boilerplate, and returns clean Markdown article text for LLM ingestion.

---

### F. Model Context Protocol (MCP) Client Architecture

OmniRoot acts as a native **MCP Host / Client**, allowing users to connect external and local tool servers:

1. **Transport**: Implements **JSON-RPC 2.0 over SSE (Server-Sent Events) + HTTP POST** and **WebSocket**.
2. **Lifecycle & Protocol Methods**:
   * `initialize`: Negotiates protocol version and client capabilities.
   * `tools/list`: Dynamically queries available tools and schemas from the MCP server, registering them in OmniRoot's `ToolRegistry`.
   * `tools/call`: Dispatches LLM tool call arguments to the MCP server and receives structured outputs.
   * `resources/read`: Reads external context (e.g. SQLite database tables, GitHub repositories, local file systems).
3. **Integration**: MCP tools appear alongside native tools seamlessly in the agent prompt schemas.

---

## 3. Provider Failover Chains & Intelligent Auto-Routing (`FallbackChainRouter`)

To ensure uninterrupted execution when LLMs encounter rate limits, context overflows, or downtime:

### A. Fallback Chain Definition & Order
* Users or workspaces configure an ordered priority list of providers/models (e.g. `Groq (Llama 3.3 70B)` $\rightarrow$ `Gemini 1.5 Flash` $\rightarrow$ `OpenRouter` $\rightarrow$ `Local SmolLM2 GGUF`).
* If the primary model fails on a regular prompt or during a tool calling turn, the `FallbackChainRouter` automatically dispatches the identical request state to the next candidate in the chain.

### B. Trigger Conditions for Failover:
1. **HTTP 429 (Rate Limit / Quota Exceeded)**
2. **HTTP 5xx (Provider Server Downtime or Gateway Timeout)**
3. **Context Length Overflow (Prompt + Schemas exceed model max tokens)**
4. **Network Timeout / Connection Failure**

### C. Visual Indication in Chat UI:
* A compact **Fallback Badge** appears inside the AI message bubble indicating which model fulfilled the response (e.g., `⚡ Auto-routed via Gemini 1.5 Flash (Fallback)`).
* The failover event, error reason, and transition latency are logged to `LogKeeper.log("FallbackRouter", ...)`.

---

## 4. Chat UI Interactive Tool Execution Card (`ToolCallBubble`)

Tool calls in `ChatScreen` render as dedicated, interactive cards within the turn:

1. **Header & Status**:
   * Displays the tool icon, name, and real-time state:
     - ⏳ `Executing...` (with subtle shimmer / progress indicator)
     - ✅ `Completed (120ms)`
     - ❌ `Failed (Error details)`
2. **Collapsible Arguments & Output**:
   * Tap to expand/collapse input parameters and captured standard output (`stdout`/`stderr`) with code formatting.
3. **Manual Approval Mode (`requiresConfirmation = true`)**:
   * For sensitive tools (e.g. file deletions, shell execution), the card pauses the agentic loop and presents **"Allow Once"**, **"Always Allow"**, or **"Reject"** buttons.

---

## 5. Universal Agentic Tool Calling Loop

```kotlin
// 1. Tool Interface Definition
interface AgentTool {
    val name: String
    val description: String
    val parametersSchema: JSONObject
    val requiresConfirmation: Boolean
    suspend fun execute(args: JSONObject, context: ToolExecutionContext): ToolResult
}

// 2. Orchestration Cycle in Chat Execution
suspend fun runAgenticTurn(userPrompt: String, threadSettings: ChatSettingsEntity) {
    var currentIteration = 0
    val maxIterations = 10

    while (currentIteration < maxIterations) {
        // Step A: Format active tools for current LLM provider (OpenAI tools JSON or ChatML XML)
        val response = router.dispatchWithFallback(currentMessages, tools = ToolRegistry.getActiveTools())

        // Step B: Detect tool calls
        if (response.toolCalls.isEmpty()) {
            // No tool calls -> Final response reached
            emitAiResponse(response.text)
            break
        }

        // Step C: Execute tool calls in parallel or sequence
        for (toolCall in response.toolCalls) {
            LogKeeper.log("ToolExecutor", "Start", "Executing ${toolCall.name} with args: ${toolCall.arguments}")
            
            // Check for manual confirmation if required
            if (toolCall.requiresConfirmation && !isPreApproved(toolCall.name)) {
                val userApproved = requestUserApproval(toolCall)
                if (!userApproved) {
                    currentMessages.add(ChatMessage(role = MessageRole.TOOL, toolCallId = toolCall.id, text = "User rejected tool execution."))
                    continue
                }
            }

            val result = ToolRegistry.get(toolCall.name).execute(toolCall.arguments)
            LogKeeper.log("ToolExecutor", "Complete", "Finished ${toolCall.name} in ${result.durationMs}ms")

            // Step D: Append tool result message to conversation history
            currentMessages.add(ChatMessage(role = MessageRole.TOOL, toolCallId = toolCall.id, text = result.output))
        }

        currentIteration++
    }
}
```

---

## 6. Telemetry & Safety Architecture

1. **Permission Control**:
   * Safe tools (`view_file`, `list_dir`, `web_search`, `js_sandbox`) execute automatically.
   * Destructive tools (`delete_file`, `shell_exec`, `proot_exec`) can be set to "Always Allow" or "Ask Before Running" in Thread/Agent Settings.
2. **Full LogKeeper Visibility**:
   * Every tool schema generation, parse event, sandbox execution timing, stdout/stderr payload, and error is logged in `LogKeeper`.

---

## 7. Token Efficiency & Low-End Device Architecture (Android 15 Go / 3GB RAM)

To guarantee flawless execution on low-memory Android Go devices without triggering Android's Low Memory Killer (LMK):

1. **Deterministic In-Memory Tool Tier (Primary)**:
   * 95%+ of operations (`view_file`, `edit_file`, `multi_edit_file`, `create_file`, `list_dir`, `grep_search`, `js_sandbox`) run natively inside Kotlin memory.
   * **RAM Cost**: 0 MB extra process overhead.
   * **Latency**: < 1 millisecond.
   * **Token Cost**: Ultra-compact payloads via line slicing and exact chunk replacement, preventing context bloat.
2. **On-Demand PRoot Alpine Linux (Secondary)**:
   * Minimal Alpine rootfs (~3.5MB download, ~15MB disk).
   * Spawns on-demand only when running compiled code or Python/Node scripts, and terminates immediately after execution to release memory back to the Android OS.
   * **RAM Cost**: ~15MB–25MB peak memory cap.

