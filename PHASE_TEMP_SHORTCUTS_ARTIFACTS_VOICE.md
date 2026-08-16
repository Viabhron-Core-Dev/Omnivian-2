# Temporary Implementation Phase: Quick Work Plan
## Dynamic Shortcuts, Artifacts CRUD, Mini PWA Loader, Audio Settings with Model Import, Quick Chat Home Widget & AI ColorNote System

---

### 1. Overview & Architectural Goals
This temporary phase focuses on high-impact, lightweight user-facing features that expand OmniRoot's ecosystem without adding heavy binary bloat:
1. **Dynamic App Launcher Shortcuts Registry** (for Launcher long-press menus and external Sidebar dock apps).
2. **Global Sidebar Chat Thread Actions** (3-dots overflow menu to pin chats to App Shortcuts, rename, and delete).
3. **Artifacts Management & Full CRUD** in the Global Sidebar (Create, View, Edit, Delete, and Pin to Shortcuts).
4. **Offline Mini PWA Dist Zip Loader** (import and serve full offline single-page apps via `WebViewAssetLoader`).
5. **Audio Settings & Zero-Bloat Voice Pipeline with Post-Install STT Model Importer** (Android default SpeechRecognizer + custom Whisper/Sherpa model importer).
6. **Claude-Style Quick Chat Home Screen Widget** (ephemeral chat box with 1-tap Voice & Camera shortcuts and tap-to-save persistence).
7. **AI Voice Note System & Default "ColorNote"-Style Artifact** (long-press voice button $\rightarrow$ STT to AI $\rightarrow$ structured sticky note cards in default Notes artifact).

---

### 2. Granular Mini-Phases Execution Map

```
Mini-Phase T.1: Room Persistence Layer & Dynamic App Shortcuts Bridge
     │  (ArtifactEntity, ArtifactDao, AppShortcutManager, Sidebar 3-dots actions)
     ▼
Mini-Phase T.2: Artifacts Screen & Full CRUD in Global Sidebar
     │  (Artifacts list, Create, View, Edit, Delete, Pin/Unpin, LogKeeper)
     ▼
Mini-Phase T.3: Offline Mini PWA Loader (`dist.zip` Importer)
     │  (Zip safe extractor, WebViewAssetLoader virtual domain server, PWA launcher)
     ▼
Mini-Phase T.4: Global Audio Settings & Voice Pipeline
     │  (AudioSettingsScreen, native SpeechRecognizer STT, TTS engine, model import hook)
     ▼
Mini-Phase T.5: AI Voice Note Engine & Default ColorNote Artifact
     │  (Long-press mic trigger, AI structured extraction, 2-column ColorNote UI)
     ▼
Mini-Phase T.6: Claude-Style Quick Chat Home Screen Widget & Deep Link Router
        (AppWidgetProvider, ephemeral session generator, 1-tap Voice/Camera, tap-to-save)
```

---

### 3. Detailed Mini-Phase Implementation Specifications

#### **Mini-Phase T.1: Room Persistence Layer & Dynamic App Shortcuts Bridge**
* **Objective**: Establish foundational Room persistence for artifacts and populate the Android Launcher long-press dynamic shortcuts list for both Artifacts and Chat Threads (accessible to external sidebars/docks via `LauncherApps.getShortcuts()`).
* **Implementation Details**:
  - **Room Database Schema**:
    - `ArtifactEntity`: (`id`, `workspaceId`, `title`, `type` [`HTML_PWA`, `SVG`, `CODE`, `MARKDOWN`, `COLOR_NOTES`], `content`, `entryPath`, `isPinned`, `createdAt`, `updatedAt`).
    - `ArtifactDao`: Queries for pinned artifacts, list all by workspace, insert, update, delete.
    - Increments `AppDatabase` version with migration.
  - **`AppShortcutManager.kt`**:
    - `syncShortcuts(context)`: Collects all pinned `ArtifactEntity` items and pinned `ChatSession` threads.
    - Limits total active dynamic shortcuts to Android's OS quota (max 15).
    - Publishes deep-link intents:
      - `omniroot://artifact/{id}` for pinned artifacts.
      - `omniroot://chat/{sessionId}` for favorite chat threads.
    - Sets dynamic icons and labels.
  - **Global Sidebar Chat 3-Dots Menu**:
    - In `GlobalSidebar.kt`, add an overflow 3-dots icon (`Icons.Default.MoreVert`) on each chat thread row.
    - Dropdown options:
      - 📌 **Pin to App Shortcuts / Unpin from Shortcuts**: Toggles dynamic shortcut presence via `AppShortcutManager`.
      - ✏️ **Rename Thread**: Fast in-place rename dialog.
      - 🗑️ **Delete Thread**: Deletes session messages and removes active shortcut.
* **LogKeeper Telemetry**:
  - `LogKeeper.log("AppShortcuts", "Sync", "Synced N active dynamic shortcuts")`
  - `LogKeeper.log("GlobalSidebar", "ThreadAction", "Action: PIN/RENAME/DELETE for $sessionId")`

---

#### **Mini-Phase T.2: Artifacts Screen & Full CRUD in Global Sidebar**
* **Objective**: Complete in-app management of agent-generated artifacts and custom web apps with full CRUD and sandboxed viewing.
* **Implementation Details**:
  - **`ArtifactsScreen.kt` (Accessible via Global Sidebar $\rightarrow$ Artifacts)**:
    - 📋 **List View**: Displays all saved artifacts with type badges (`HTML_PWA`, `SVG`, `CODE`, `MARKDOWN`, `COLOR_NOTES`) and pin state.
    - ➕ **Create**: Manually create text/HTML/Markdown artifacts.
    - 👁️ **View**: Fullscreen sandboxed viewer with responsive framing.
    - ✏️ **Edit**: In-app code editor for live tweaking of code and text.
    - 🗑️ **Delete**: Deletes database entry and cleans up extracted files with confirmation.
    - 📌 **Pin to Shortcuts**: 1-tap toggle updating dynamic launcher shortcuts.
* **LogKeeper Telemetry**:
  - `LogKeeper.log("Artifacts", "Create/Edit/Delete", "Artifact $id ($title)")`

---

#### **Mini-Phase T.3: Offline Mini PWA Loader (`dist.zip` / `build.zip` Importer)**
* **Objective**: Import full client-side web application build bundles (React, Vue, Vite, HTML5) and run them locally offline.
* **Implementation Details**:
  - File picker in `ArtifactsScreen.kt` allowing users to import `.zip` bundles.
  - Extracts bundle to internal storage (`/files/artifacts/pwa_{id}/`) with path traversal / Zip Slip protection.
  - Automatically identifies `index.html` and extracts metadata (title, icons) from `manifest.json`.
  - Serves files via AndroidX `WebViewAssetLoader` under `https://appassets.androidplatform.net/assets/pwa_{id}/index.html` to eliminate CORS and `file:///` restrictions.
  - Adds the imported PWA to the Artifacts list with 1-tap launcher shortcut support.
* **LogKeeper Telemetry**:
  - `LogKeeper.log("PwaLoader", "ImportSuccess", "PWA $title loaded from zip ($fileCount files)")`

---

#### **Mini-Phase T.4: Global Audio Settings & Voice Pipeline (0 MB APK Base Footprint)**
* **Objective**: Global audio settings with 0 MB APK base footprint and post-install offline model import.
* **Implementation Details**:
  - **Global Settings $\rightarrow$ Audio Page (`AudioSettingsScreen.kt`)**:
    - **Engine Selection**:
      - 🟢 **Android System SpeechRecognizer (Default / 0 MB)**: Built-in OS engine with zero app bloat.
      - 🔵 **Custom Offline Model (Imported)**: Select imported Whisper / Sherpa / ONNX models.
    - **Model Import Action**:
      - "Import STT Model (.bin / .onnx)" file picker $\rightarrow$ stores in `/files/audio_models/`.
      - Displays active model file size and RAM profile.
    - **TTS Voice Engine Configuration**:
      - Voice selector, speech pitch slider, and speech rate slider.
  - **Microphone Dictation Hook**:
    - Tap to speak in `ChatScreen` input bar with live text streaming.
* **LogKeeper Telemetry**:
  - `LogKeeper.log("AudioSettings", "EngineSwitch", "Active engine: $engineName")`
  - `LogKeeper.log("Voice", "STTComplete", "Dictation duration: ${ms}ms")`

---

#### **Mini-Phase T.5: AI Voice Note Engine & Default ColorNote Artifact (`ColorNotesViewer.kt`)**
* **Objective**: Long-press voice dictation that transforms spoken speech into categorized, high-contrast sticky note cards.
* **Implementation Details**:
  - **Voice Action Hook**:
    - Long-press on the microphone button in `ChatScreen` or Home Widget triggers **Voice Note Mode**.
    - Transcribes speech via active STT engine $\rightarrow$ dispatches transcript to fast AI structuring pipeline.
    - AI extracts: `title`, `color` (amber, pastel blue, mint green, coral rose, lavender, slate), `items` (bullet points or checklist with checkboxes), and `raw_memo`.
  - **Default System Artifact ("My Notes")**:
    - Built-in default artifact (`id = "system_default_notes"`, `type = "COLOR_NOTES"`, `isPinned = true`).
    - Auto-appends the structured note card into this artifact's payload.
  - **ColorNote-Style UI Component**:
    - 2-column staggered masonry grid of vibrant sticky post-it cards.
    - Interactive checklist toggle (strike-through animations), lined paper memo view, color picker, and instant text search.
    - Offline fallback: If LLM is unavailable, saves raw transcript immediately as a quick yellow memo so speech is never lost.
* **LogKeeper Telemetry**:
  - `LogKeeper.log("VoiceNote", "AIStructured", "Generated note '$title' [Color: $color]")`

---

#### **Mini-Phase T.6: Claude-Style Quick Chat Home Widget & Deep Link Router**
* **Objective**: Floating quick chat launcher widget on the Android home screen with instant voice/camera entry and tap-to-save persistence.
* **Implementation Details**:
  - **Widget UI (`QuickChatWidgetProvider.kt`)**:
    - Compact card displaying **OmniRoot Quick Chat** header.
    - Two prominent action buttons: **[ 🎙️ Voice ]** and **[ 📷 Camera ]**.
  - **Interaction Flows**:
    - **Tap Body**: Creates an ephemeral session (`temp_widget_{timestamp}`) and cold-starts `MainActivity`.
    - **Tap 🎙️ Voice**: Opens app directly in dictation mode (`omniroot://chat/temp_widget_{timestamp}?auto_voice=true`).
    - **Tap 📷 Camera**: Opens app directly with image attachment picker / camera (`omniroot://chat/temp_widget_{timestamp}?auto_camera=true`).
  - **In-Chat Conversion ("Tap Name to Save")**:
    - Tapping the temporary thread title inside the top bar converts the ephemeral session into a permanent, named thread in Room DB.
* **LogKeeper Telemetry**:
  - `LogKeeper.log("Widget", "ColdStart", "Launched via widget action: $action")`
  - `LogKeeper.log("Widget", "SessionSaved", "Converted temp session to permanent thread")`
