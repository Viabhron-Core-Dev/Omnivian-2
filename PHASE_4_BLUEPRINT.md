# Phase 4 Blueprint: File System & Native Readers

## Objective
Build the real File Explorer wired to the local Android application file system. Implement Native File Readers and File Operations to handle standard document types (PDF, PPT, ZIP) natively within the application.

## Sub-Phases / Mini-Phases

### Phase 4.1: Core Local File System Engine (Completed)
* **Task:** Implement `LocalFileManager` singleton or repository using standard Kotlin `java.io` / `java.nio` and Coroutines.
* **Features:**
  * Define the root workspace directory within the app's internal files (`context.filesDir`).
  * Recursive file listing and mapping into a structured tree model (`FileNode`).
  * CRUD operations: Create, Read (bytes/string), Update, Delete, Rename.
  * State management: Expose a `StateFlow` of the current directory tree for reactive UI updates.

### Phase 4.2: File Explorer UI Integration (Completed)
* **Task:** Connect the `LocalFileManager` to the File Tree UI (e.g., Acode Style Right Drawer or a dedicated full-screen explorer).
* **Features:**
  * Nested folder expanding/collapsing logic.
  * File icons based on extension (`.txt`, `.pdf`, `.zip`, `.kt`, etc.).
  * Context Menus (3-dot): Rename, Delete, Copy, Zip.
  * Interaction: Clicking a file routes it to the corresponding Native Reader, or later, the Code Editor (Phase 5).

### Phase 4.3: Native File Readers (PDF & Document Parsing) (Completed)
* **Task:** Build custom Compose screens or views to render and parse complex documents without leaving the app.
* **Features:**
  * **PDF Reader:** Utilize Android's `PdfRenderer` API to convert PDF pages to bitmaps and display them in a scrollable `LazyColumn`.
  * **Text/Code Viewer:** Fallback scrollable text surface for `.md`, `.kt`, `.json`, etc.
  * **PPT Extraction (Text):** Implement lightweight Apache POI (if feasible) or a basic binary text-extraction heuristic to allow the AI to read presentation content.

### Phase 4.4: ZIP Operations (Compress & Decompress) (Completed)
* **Task:** Implement ZIP archive handling to allow packaging workspaces and extracting user uploads.
* **Features:**
  * `ZipUtils` helper using `java.util.zip.ZipInputStream` / `ZipOutputStream`.
  * Extract a `.zip` file into a new folder within the app's workspace.
  * Compress a folder into a `.zip` file for export.
  * UI integration: "Extract Here" and "Compress to Zip" actions in the file context menu.

### Phase 4.5: Chat & Tool Integration (Completed)
* **Task:** Wire the real file system into the existing AI Chat and Tool infrastructure.
* **Features:**
  * The "+" attachment bottom sheet in Chat pulls from the real `LocalFileManager` rather than placeholder lists. Android File Pickers implemented for images and files.
  * GitHub repo importer added to download branches directly into the workspace root.
  * AI Tools are granted read access to attached files to provide context-aware responses via `TextExtractor` (lightweight text parser).
