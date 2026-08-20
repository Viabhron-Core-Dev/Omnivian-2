# Blueprint: OmniRoute / AI Studio Mobile IDE

## 1. Core Architecture & Infrastructure
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Dual-Server Isolation**: 
  - **AI Proxy Server**: A full Node.js server environment (similar to Termux execution) running OmniRoute strictly in the foreground. It is tied to the app's lifecycle, providing internal AI agents a resilient gateway to LLMs. Full Node.js support is mandatory due to the complexity and security requirements of the proxy.
  - **User App Server**: A separate local web server (NanoHTTPD) used exclusively for previewing the active thread's generated web apps. It spins up on demand via a top-bar "Play" button and shuts down when closed. 
  - *Network Binding*: Both servers bind exclusively to `localhost` to ensure strict isolation and prevent external network exposure unless explicitly authorized.
- **App Lifecycle & Execution**: Chat threads and the embedded proxy run strictly in the foreground. The currently opened chat thread always has execution priority.
- **Agentic Engine**: A multi-agent system (Architect, Coder, Reviewer) for building, debugging, and auditing code. Supports local AI models (via OmniRoute) alongside cloud providers.
- **Orchestration & State Management**: The Android app acts primarily as a UI frontend and orchestrator. It manages file I/O, UI state, and process lifecycles, delegating heavy API routing to OmniRoute and reasoning to the Agentic Engine.
- **Android App as Tool Executor**: The Android shell handles the hard native work for tools and integrations:
  - **Auth & API calls**: Managing OAuth tokens for Google Drive, communicating with the Firebase SDK (Python Sandbox for tools), making GitHub API calls.
  - **Local Execution**: Running the local JS Sandbox.
  - **File Operations**: Natively opening, reading, compressing, and decompressing files, PDFs, and PPTs.
  - **Default MCP Providers**: GitHub, GDrive, and Firebase are available as default MCP connections.
  - **Native Tools**: Features like search are executed natively by the app.
- **Extensible Tool Infrastructure**: The core infrastructure includes a built-in tool system from the start. This allows tools (built-in, Firebase Python, Drive, GitHub MCPs) to be registered at the app level, ready to be utilized by the Agent logic once it is integrated.
  - **Tool Permissions & Security**: Includes explicit user-approval hooks for sensitive tool executions (e.g., file deletion, external network requests) to ensure safety prior to agent automation.
- **Memory/Context**: Modular memory architecture for persistent context retention.
- **Log Keeper**: An always-on, shell-level logging system built from the start. Accessible via a Floating Action Button (FAB) anchored to the bottom-left corner of the screen. It strictly captures error types, component failures, timestamps, stack traces, and crash states from the AI Agents and OmniRoute, but strictly filters out ALL passwords or credentials. Includes a master on/off switch and time filters. Instead of pruning older logs, it exports them to the device's Download folder before clearing the active log state.

## 2. Global UI Layout & Navigation
- **Global Sidebar (Left Drawer)**: Contains the following list of items:
  1. **New Chat**: Top action to create a new thread.
  2. **Artifacts**: Dedicated page for saved artifacts from chats. All chat artifacts are temporary unless saved here. Ultimately all artifacts data will be remembered.
  3. **Design**: Placeholder for now (to be added after backend).
  4. **List of Chats (Repos)**: The multi-thread workspaces list.
  5. **Global Settings**: At the bottom.
- **Global App Settings (The "Library")**: Accessed via the sidebar. Structured as a simple list of items, where tapping each item opens its own dedicated page:
  - **Skills**: Add/edit/delete specific agent capabilities.
  - **Tools & Plugins**: Manage standard tools and Plugins (a hybrid of skills, tools, and MCP).
  - **Memory & Artifacts**: Global memory configuration and saved artifacts.
  - **System Instructions**: Global guardrails and rules.
  - **Built Agents**: Library of custom-configured agents.
  - **Model Context Protocol (MCP)**: Configuration for MCP integrations (GitHub and Firebase Python included as default providers; Cloudflare MCP to be checked later).
  - **Google Drive Archive**: Configure Google Drive integration. Old chats and workspaces (repos older than 10) are automatically compressed into ZIP files and saved in a designated Google Drive folder.
- **Fixed Bottom Navigation**: A pill-shaped bar fixed at the absolute bottom of the screen (not floating). It switches between **Chat** and **Code** views, accompanied by a 3-dot menu for workspace actions. The 3-dot menu contains the following options:
  - **Thread Settings**: Access the active thread's configurations.
  - **Export**: Export the workspace as a ZIP file or quick push to GitHub.
  - **Remix**: Copy the repository and all integrations (agents, skills, settings) into a new workspace, clearing chat history and the GitHub repo link.
  - **AI Manager Token Panel**: Access the integrated proxy web interface.
- **AI Manager Token Panel**: An integrated WebView pointing to the embedded proxy's local web interface to manage API keys, routing, analytics, and connections to **local AI models** (e.g., Ollama, on-device models, LM Studio). OmniRoute natively handles on-device AI inference and routing.

## 3. Workspace Views (Thread Specific)
### A. Chat View (Google AI Studio Style)
- **Top Bar (Agent Switcher)**: Tap the active Agent to open an Agent Card and customize:
  - **Model Order**: Fallback chains (e.g., try Claude, then GPT-4).
  - **System Prompts**: Specific rules for the active agent.
  - **Schedules/Reactions**: Post-task automated actions (e.g., auto-lint after coding).
- **Chat Input**: A scrollable (not infinitely expandable) input box to ensure the bottom text is always visible. It features a "+" button for uploading attachments (`.txt`, `.zip`, `.pdf`, code files), and an **Agent/Model Selector Pill** built directly into the input bar to quickly switch agents or models before sending.
- **Chat Interface & History**:
  - Beneath the user's sent message, a detailed list of actions and files modified, added, or deleted is displayed.
  - **File Bottom Sheet**: Tapping any changed file in the chat history opens it in a bottom sheet view for quick inspection (similar to the Artifacts UI).
  - **Native File Revert**: The app's native file system records a history of file changes independently of the AI. After an AI reply, the UI provides a file diff view and a button to revert the workspace back to that exact state using the local app-level file history.
- **Thread Settings Page**: A dedicated settings page for the active thread, styled with horizontal pill-shaped tabs at the top (like Google AI Studio):
  1. **Universal**: Thread-level tools, skills, MCPs, plugins, and guardrails accessible to all agents.
  2. **Agents**: Manage the list of active agents in the thread.
  3. **Versions**: Access version history/snapshots for the workspace.
  4. **Secrets**: Thread-specific API keys and secrets.
  5. **GitHub**: Configure GitHub integration specific to this thread for pushing to repositories.

### B. Code View (Acode Style)
- **File Tree (Right / End Drawer)**: Slide-out drawer on the right side showing repository structure, avoiding collision with the global left sidebar. Includes a search bar. The project root, folders, and files all have their own 3-dot context menus for file operations (create, delete, rename). Tapping a file opens it in the main editor *without* closing the drawer.
- **Main Editor**: A lightweight, standard text editor acting as the main view for inspecting or tweaking code.
  - **Live Generation View**: When an agent is writing code, the editor switches to a read-only live view (or displays a structured loading state) so the user can watch the code being generated in real-time or understand that a process is running.

## 4. Agent Capabilities & Export
- **Parallel Agent Execution (Antigravity Architecture)**: The IDE supports a parallel agentic model where a master orchestrator spawns and coordinates sub-agents to perform tasks concurrently (e.g., searching docs, generating code, running lint tests). This allows non-blocking background operations, streaming progress back to the main UI.
- **File Readers & Analysis**: Built-in tools for agents to process user uploads.
- **Code Sandboxing**: Secure execution environments for tools and scripts:
  - **On-Device JS Sandbox**: For local, lightweight JavaScript execution.
  - **Cloud Firebase Python Sandbox**: [ON HOLD] Temporary, secure Python environment for heavy tool execution.
- **Artifacts**: Support for generating standalone small web apps. Artifacts have the capability to embed AI logic directly within them.
  - **Design Studio (UI Map Artifact)**: A specialized visual artifact accessible from the main sidebar. Allows users to create clickable UI interface maps (e.g., "tap note card -> settings screen"). These preview web apps are highly customizable (adjustable button sizes, fonts, colors). The tool divides designs into screens/menus and exports them as well-structured JSON, code, or images (to prevent AI hallucination) to serve as exact UI blueprints for building native Kotlin shells in chat.
- **Export & Deployment**:
  - **GitHub Integration**: Push directly to remote repositories using a PAT or OAuth App, replicating Google AI Studio's export functionality.
  - **ZIP Export**: Package the local workspace into a `.zip` for manual extraction.

## 6. CI/CD & Build Pipeline
- **GitHub Actions Integration**: The Android APK is strictly built using a GitHub Actions workflow upon pushing to `main` or manual dispatch.
- **Workflow configuration**: Uses `ubuntu-latest`, JDK 21, and Gradle setup. It automatically generates a transient debug keystore (`keytool -genkey`) during the workflow to bypass local keystore credential risks and builds the APK using `gradle assembleDebug --no-daemon --no-configuration-cache`.

## 5. Development Phases
- **Phase 1 (Completed)**: Setup project foundation, UI skeleton (Sidebar, Fixed Bottom Navigation, Dual-tab layout), Local File System tracking, and the shell-level **Log Keeper**.
- **Phase 2 (Completed)**: Implement the NanoHTTPD preview server and the Extensible Tool Infrastructure (building the hooks and empty spaces for tools, skills, and MCPs).
- **Phase 3 (Completed)**: Replace Chat placeholders with real state (dynamic action history, message list, model picker, file attachment bottom sheet). Implement the **Agent Card (Top Bar)** UI and the **Chat Input UI** (scrollable input box with embedded Agent/Model Selector Pill). Implement the real **Global Sidebar UI** (New Chat, Artifacts, Design, List of Chats, Global Settings). Implement the 3-dot menu actions UI in the Fixed Bottom Navigation (Thread Settings, Export, Remix, AI Manager Token Panel). Update Chat top-right "Play" button to open an artifacts list placeholder.
- **Phase 4 (File System & Native Readers)**: Build the real File Explorer wired to the local Android file system. Implement Native File Readers & Operations (app-level handling for PDF, PPT, and ZIP compress/decompress).
- **Phase 5 (Code Editor)**: Implement the Code Editor (Acode Style). Connect it to the File Explorer to open, edit, and save real files. Include live generation view states and native file revert UI. Add tabs to the File Explorer drawer (File Tree, Issues, Pull Requests). Add local Issue and PR creation to the file/folder 3-dot context menu.
- **Phase 6 (Global Settings UI & App Expansion)**:
  - **Global Settings List**: Build the Global App Settings as a list of items routing to dedicated pages:
    - **Agents**: List of built agents (and ability to build new ones).
    - **Skills**: Where skills are added for the rest of the app to use.
    - **Tools**: List of tools with a permission toggle beside each name (Always Ask, Use Freely, No Permission).
    - **MCP**: List added custom MCPs. Include a FAB to add new ones. Built-in MCPs (GDrive, GitHub, Firebase) are present for the user to connect.
    - **Plugins**: User-made combinations of skills, tools, and MCPs to form a plugin.
    - **Integrations**: GitHub, Firebase, and GDrive more settings. These three are greyed out if not connected. Inside are extra required settings.
    - **Code Editor**: More settings for the code editor.
    - **Artifacts (Preview)**: Settings for artifacts preview.
    - **Library Management**: Settings for the new Library feature.
    - **Permissions**: System permissions used by the app.
    - **Font**: Typography settings.
    - **AI Manager Panel**: A professional, tabbed dashboard (Directory, Active Keys, Available Models, Token Counter, Model Rater) inspired by OmniRoute.
  - **Global Sidebar Addition (Library)**: Add a new element called "Library" to the Global Sidebar where all uploaded files are stored (synced to GDrive, similar to modern AI services).
  - **Chat Token Bar**: Add a small bar in the Chat view to monitor if token limits are exceeded.
- **Phase 7 (Thread Settings & Export UI)**:
  - **Thread Settings UI**: Build the dedicated Thread Settings page with Google AI Studio-style pill-shaped tabs (Universal, Agents, Versions, Secrets).
  - **Export Flow**: Wire up the 3-dot menu "Export" action to provide two distinct options: ZIP or GitHub.
  - **GitHub Export Bottom Sheet**: Build a focused bottom sheet (without pill tabs) specifically for committing changes. It must display the target repository/branch, a commit message text area, a list of explicitly changed files with their status (Modified, Added, Deleted), and a "Stage and commit all changes" button. This performs a targeted batch push of only the modified files, rather than a full zip upload.
- **Phase 8 (Artifacts & Previews)**: Implement Artifacts generation and management. Implement the **PWA Bottom Sheet Preview** to load PWAs and Artifacts from the Chat top-right list.
- **Phase 9 (Native AI Manager & Router)**: Implement a lightweight, pure-Kotlin local AI Gateway inspired by OmniRoute. Features a comprehensive provider directory (API-based, Free tiers, Local), advanced analytics (total request counts, token usage, and total cost estimation), dynamic routing strategies (priority lists, combo routing for cost/speed/quality optimization), a stable guided WebView for API key generation (Direct-to-Key), a Foreground Service proxy, and native on-device execution of `.gguf` models (via `llama.cpp` integration). Optimized for 3GB RAM Android Go. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases).
- **Phase 10 (Native Tool Executor)**: Implement Native Tool Executor Integrations (manage OAuth tokens for Google Drive, GitHub API calls, and native tools like Search).
- **Phase 11 (The Brain, Memory & Agent Ecosystem)**: Implement core Agent Logic and Diff Parsing. Connect them into the pre-built UI and tool infrastructure. Implement the Modular Memory Architecture for persistent context retention. Implement a multi-agent PR/Issue flow (Planner, Coder, Reviewer) controlled by optional thread/agent settings that utilizes local Issues/PRs like a GitHub MCP workflow to optimize token usage. Includes Phase 11.4 (Surgical Edit Tool): building a robust "Search & Replace" Block editor (`edit_file` tool) requiring `target_text` and `replacement_text` to enforce fast, token-efficient, and line-shift-safe local AI code modifications.
  - **Phase 11.5 (Conversational In-Chat Agent Builder)**: Describe a desired agent in chat to have the AI compile a structured Agent Manifest rendered as an **Interactive Agent Review Card** in the chat timeline (editable Name, Icon, Tag, System Instructions, Model, and Tool bindings) with a 1-tap "Save to Agent Library" action that persists to the Room DB with custom tags.
  - **Phase 11.6 (Complex Executable Agents & Tool/Sandbox Binding)**: Support Hermes/Claude/OpenAI-style complex executable agents that go beyond static prompt instructions by binding Phase 10 Sandboxes (PRoot Linux, JS, Shell) and MCP tools, with structured function calling schemas (`<tool_call>`/JSON) and multi-step execution loops.
- **Phase 12 (Antigravity Orchestration)**: Build the Parallel Agent Execution (Antigravity) orchestrator and Sync logic for handling concurrent sub-agent tasks and background operations, including the multi-agent PR/Issue flow. Integrates Antigravity CLI logic as the "AI Brain" to dynamically distribute tasks across Phase 9 providers (e.g., automatically routing background tasks to free-tier providers to save costs).
- **Phase 13 (Advanced Cloud Integration & CI/CD)**: Wire up advanced cloud capabilities (GitHub full export, Drive Archive, Artifact embeddings). Configure the CI/CD GitHub Actions Build Pipeline. (Note: Firebase Python Sandbox is on hold).
- **Phase 14 (Design Studio)**: Build the Design Studio / UI Map Artifact tool for generating UI reference blueprints.
