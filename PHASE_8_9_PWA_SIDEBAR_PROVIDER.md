# Phase 8.9: PWA Storage Hub, Content Provider & Headless AI Provider for External Sidebar

## Master Architecture Overview

This document specifies the export contracts and implementation mini-phases to transform this application into a **PWA Generator, Storage Hub, and Local AI Provider** that an external Sidebar application can query and utilize securely over Android IPC (Inter-Process Communication).

---

### Master Architecture Pipeline

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 EXTERNAL SIDEBAR APP                                        │
│                                                                                             │
│  • Discovers mini apps via ContentResolver.query("content://com.example.provider.pwa/...") │
│  • Loads HTML/JS into local WebView via ContentResolver.openInputStream()                   │
│  • Binds to IAiInferenceService (AIDL) for headless prompt token streaming                  │
│  • Updates PWA settings & state via ContentResolver.update()                                │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │ (Protected by android:protectionLevel="signature")
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                          HOST AI STUDIO APP (THIS APPLICATION)                              │
│                                                                                             │
│  1. Artifacts (mini apps) Storage Hub (Room DB: artifacts table & filesDir/workspaces/)     │
│  2. Secure PwaContentProvider (Cursor streaming, ParcelFileDescriptor file pipe)            │
│  3. Headless AI Inference Service (IAiInferenceService AIDL Bound Service)                  │
│  4. In-App AI Generator & Editor (Chat / Code tabs, Topbar 3-dots Save/Fork controls)       │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Granular Mini-Phases Breakdown (8.9.1 – 8.9.5)

---

### MINI-PHASE 8.9.1: Artifact & PWA Schema Extension
**Focus**: Extend the existing `ArtifactEntity` and `ArtifactDao` to support complete PWA metadata and Cursor queries without breaking existing voice notes or HTML previews.

- **1. Database Schema Extension (`ArtifactEntity.kt`)**:
  - `iconUri: String? = null` — Local file path or data URI for the app launcher icon in the external sidebar.
  - `isLightweight: Boolean = false` — Flag indicating whether the mini app should open as a compact floating overlay vs. full panel in the sidebar.
  - `manifestJson: String? = null` — Web App Manifest payload (`display`, `theme_color`, `start_url`, `icons`, `name`).
  - `settingsJson: String? = null` — Synced user configuration, local states, or variables.
  - `version: Long = 1L` — Incremental version for cache invalidation across apps.
- **2. Cursor Queries (`ArtifactDao.kt`)**:
  - Add `@Query("SELECT * FROM artifacts WHERE type IN ('HTML', 'PWA', 'REACT') ORDER BY isPinned DESC, updatedAt DESC") fun getPwaArtifactsCursor(): Cursor`.
  - Add `@Query("SELECT * FROM artifacts WHERE id = :id") fun getArtifactCursorById(id: String): Cursor`.
- **3. PWA Bundle Standardization (`ArtifactWorkspaceManager.kt`)**:
  - Automatically create standard files in `filesDir/workspaces/artifact_<id>/`:
    - `index.html` (entry point)
    - `manifest.json` (web app manifest)
    - `settings.json` (user settings and state)
    - `app.js` / `style.css` (bundled assets)

---

### MINI-PHASE 8.9.2: Secure PWA Content Provider (`PwaContentProvider`)
**Focus**: Implement a high-performance, secure `ContentProvider` for querying mini apps and streaming assets to external client apps.

- **1. Provider Authority & Routing (`PwaContentProvider.kt`)**:
  - Authority: `com.example.provider.pwa`
  - Routes:
    - `content://com.example.provider.pwa/artifacts`
      - `query()`: Returns all mini apps via `ArtifactDao.getPwaArtifactsCursor()`.
      - `insert()`: Accepts `ContentValues` to register a new mini app and initialize its workspace.
    - `content://com.example.provider.pwa/artifacts/<id>`
      - `query()`: Returns metadata for a single mini app.
      - `update()`: Updates `settingsJson`, `manifestJson`, or `title`.
      - `delete()`: Removes database record and workspace directory.
    - `content://com.example.provider.pwa/artifacts/<id>/files/*`
      - `openFile(uri, mode)`: Resolves requested asset path under `filesDir/workspaces/artifact_<id>/`.
- **2. Asset Streaming & File Descriptor (`ParcelFileDescriptor`)**:
  - Streams HTML, JS, CSS, images, and fonts directly via `ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)`.
- **3. Path Traversal Defense**:
  - Canonical path validation: `file.canonicalPath.startsWith(artifactDir.canonicalPath)` prevents directory traversal (`../`) vulnerabilities.

---

### MINI-PHASE 8.9.3: Security, Permissions & Manifest Declarations
**Focus**: Ensure strict signature-based security and package visibility while avoiding home screen launcher shortcut clutter.

- **1. Signature Protection (`AndroidManifest.xml`)**:
  - Declare custom signature permission:
    ```xml
    <permission
        android:name="com.ai.app.permission.ACCESS_PWA_DATA"
        android:protectionLevel="signature" />
    ```
- **2. Provider Protection**:
  - Configure `PwaContentProvider`:
    ```xml
    <provider
        android:name=".engine.provider.PwaContentProvider"
        android:authorities="com.example.provider.pwa"
        android:exported="true"
        android:readPermission="com.ai.app.permission.ACCESS_PWA_DATA"
        android:writePermission="com.ai.app.permission.ACCESS_PWA_DATA"
        android:grantUriPermissions="true" />
    ```
- **3. Package Visibility (`<queries>`)**:
  - Declare external Sidebar app package name in `<queries>` to comply with Android 11+ package visibility rules.
- **4. No Home Screen Launcher Clutter**:
  - Artifacts and mini apps are discovered and launched exclusively through the `PwaContentProvider` and external Sidebar app; **no** Android home-screen launcher shortcuts or dynamic shortcut entries are created.

---

### MINI-PHASE 8.9.4: Headless AI Inference Service (AIDL Bound Service)
**Focus**: Expose an IPC Bound Service that allows external applications to stream AI inference tokens in the background without launching any UI screens.

- **1. AIDL Interface Contracts**:
  - `IAiCallback.aidl`:
    ```aidl
    package com.example.ai;

    oneway interface IAiCallback {
        void onToken(String token);
        void onComplete(String fullResponse, in Bundle metadata);
        void onError(int errorCode, String errorMessage);
    }
    ```
  - `IAiInferenceService.aidl`:
    ```aidl
    package com.example.ai;
    import com.example.ai.IAiCallback;

    interface IAiInferenceService {
        void streamPrompt(String modelId, String systemPrompt, String userPrompt, in Bundle options, IAiCallback callback);
        void cancelInference(String requestId);
        List<String> getAvailableModels();
    }
    ```
- **2. Service Implementation (`AiInferenceService.kt`)**:
  - Bound service (`android.app.Service`) protected by `android:permission="com.ai.app.permission.ACCESS_PWA_DATA"`.
  - Connects IPC prompt calls to the app's streaming LLM coroutine engine.
  - Emits real-time token chunks via `callback.onToken(token)` and signals completion with `callback.onComplete()`.
  - Supports non-blocking request cancellation via `cancelInference()`.

---

### MINI-PHASE 8.9.5: UI Controls & End-to-End Verification
**Focus**: Ensure bidirectional interaction between in-app editing and external client access.

- **1. In-App Editing Workflow**:
  - "Edit with AI" action in **Artifacts (mini apps)** list & viewer header switches active workspace directly to **Chat** and **Code** editor tabs.
  - Topbar 3-dots dropdown menu provides:
    - **Save to Artifact**: Syncs edits back to the existing artifact DB record.
    - **Fork / Save as New Artifact**: Clones workspace into a new standalone artifact entity.
    - **Save as Artifact (mini app)**: Saves regular chat HTML/PWA workspaces into the library.
- **2. External Client Verification**:
  - External Sidebar app queries `PwaContentProvider` to populate its mini app drawer.
  - External Sidebar app loads and executes mini app HTML/JS in an isolated WebView.
  - External Sidebar app sends prompts over AIDL and receives streaming tokens without UI disruption.

---

## Downstream Phase Synergies & Alignment

1. **Phase 9 (Native AI Manager & OmniRoute Router)**:
   - `AiInferenceService` leverages OmniRoute's model directory, local `llama.cpp` `.gguf` inference, and fallback routing for external client requests.
2. **Phase 10 (Native Tool Executor & Sandboxes)**:
   - Mini apps opened in the external Sidebar can be granted permissions to trigger native Android tools via the provider/AIDL channel.
3. **Phase 11 (The Brain & Memory)**:
   - Multi-agent PR/Issue workflows and surgical diff editing can be initiated headlessly by external tools.
4. **Phase 12 (Antigravity Orchestration)**:
   - Parallel agent tasks can generate and update mini apps in background threads, automatically dispatching ContentProvider change notifications to the Sidebar app.
