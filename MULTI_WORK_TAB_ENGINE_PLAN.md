# Multi-Work Tab & Multi-Engine Architecture Plan
> **CRITICAL EXECUTION NOTE**:
> This roadmap is strictly **post-current blueprint**. Implementation of these phases will begin **only after all currently scheduled blueprint phases (Phase 10 through completion) are fully finished and verified**.

---

## 🎯 Architectural Vision & Philosophy

1. **Lightweight Android-Native Editors**: Rather than bloated desktop ports, use clean, responsive native Android UI components (`Compose Canvas`, `RichText/Markdown`, `HorizontalPager`, `Compose DataGrid`, `Media3/ExoPlayer`, `AudioWaveform`).
2. **Deep AI Connectivity**: The AI handles the intelligent work (transformations, visual markup analysis, voice synthesis, automated cutting, code generation, formula execution).
3. **History & Variation Strip**: Every engine includes a bottom horizontal card strip for previous generations, pages, recorded takes, or draft revisions that can be tapped, compared, or sent back to AI context.
4. **Primary vs. Auxiliary Workspaces**: A session has a primary deliverable focus (e.g. Code), with auxiliary tabs (e.g. Canvas sketch or Data table) that provide active context feeds to the AI.

---

## 📋 Mini-Phases Breakdown (Easy & Incremental Steps)

---

### 🔹 Mini-Phase 1: Work Tab Container & Model Intent (Structural Shell)
* **Goal**: Evolve the UI navigation shell from "Code" to "Work" without breaking existing code editor functionality.
- [ ] **1.1**: Rename Bottom Navigation bar tab label from `Code` to `Work` (with code icon as default).
- [ ] **1.2**: Update `NewChatDialog` to include a **Primary Work Focus** dropdown:
  - 💻 `Code / App Engine` (Default)
  - 🎨 `Visual Canvas / Image`
  - 📝 `Document / Notes`
  - 📽️ `Presentation / Slides`
  - 📊 `Table / Data Grid`
  - 🎙️ `Audio / Voice Studio`
  - 🎬 `Video / Clips Studio`
- [ ] **1.3**: Update `AttachmentPickerBottomSheet` to feature fast mode-trigger action pills at the top (Gemini-style: *Canvas*, *Generate Image*, *Document*, *Slides*, *Data Table*, *Voice*, *Video*).
- [ ] **1.4**: Add modular placeholder screens / empty-state cards for non-code engines inside the Work view.

---

### 🔹 Mini-Phase 2: Work Tab Multi-Tab Management & Navigation
* **Goal**: Support multiple active tabs and quick switching inside the Work screen.
- [ ] **2.1**: Implement `WorkTabState` manager (tracking open tabs: `id`, `title`, `engineType`, `contentUri`, `isPrimary`).
- [ ] **2.2**: Add top tab-strip inside `WorkScreen` allowing users to switch between open tabs or close auxiliary tabs.
- [ ] **2.3**: Implement **Long-Press gesture** on the bottom navigation `Work` button to open a quick radial/popup tab switcher.
- [ ] **2.4**: Connect auxiliary tab contents into the AI chat context prompt as supplementary references.

---

### 🔹 Mini-Phase 3: Visual Canvas & Image Engine (Draw & Generate)
* **Goal**: A clean drawing pad and image generator with a bottom variation strip.
- [ ] **3.1**: Implement Compose Canvas drawing engine (`DrawBox` style: pen, highlighter, eraser, color palette, undo/redo).
- [ ] **3.2**: Add Zoom & Pan canvas container supporting background image overlays for markup.
- [ ] **3.3**: Implement **Bottom Variation Strip**: Horizontal thumbnail row showing past AI image generations and user sketch revisions.
- [ ] **3.4**: Implement "Send Canvas to AI" action (exports drawing/annotation as image attachment into the active chat).

---

### 🔹 Mini-Phase 4: Document Engine (Rich Text & Markdown Studio)
* **Goal**: An AI-assisted document and research notes editor.
- [ ] **4.1**: Build Markdown / WYSIWYG rich text editor with quick formatting toolbar (Bold, Italic, H1, H2, Bullets, Checklists, Code Blocks).
- [ ] **4.2**: Add **Bottom Document Revisions Strip**: Horizontal card strip of document drafts, AI summaries, and previous snapshots.
- [ ] **4.3**: Integrate AI quick actions: *Rewrite*, *Summarize*, *Fix Grammar*, *Translate*, and *Export to Markdown/PDF*.

---

### 🔹 Mini-Phase 5: Presentation Engine (Slide Deck Studio)
* **Goal**: An interactive Markdown-to-Slide deck editor and viewer.
- [ ] **5.1**: Build slide deck carousel viewer using Jetpack Compose `HorizontalPager`.
- [ ] **5.2**: Implement split/toggle view for editing slide markdown structure (Title, Body bullets, Visual layout style).
- [ ] **5.3**: Add **Bottom Slide Thumbnail Strip**: Numbered mini-slide cards with reorder, duplicate, and add slide controls.
- [ ] **5.4**: Add AI slide generator & formatting assistant ("Generate 5 slides on topic X", "Add speaker notes").

---

### 🔹 Mini-Phase 6: Table & Data Grid Engine (Spreadsheet Studio)
* **Goal**: A scrollable data grid editor with AI formula and transformation support.
- [ ] **6.1**: Implement scrollable monospace data grid with cell tap-to-edit, row/column addition, and CSV/JSON import/export.
- [ ] **6.2**: Add top formula/query bar for filtering and quick computations.
- [ ] **6.3**: Add **Bottom Sheets & Snapshots Strip**: Multi-sheet tabs and historical table transformation cards.
- [ ] **6.4**: Connect AI data assistant ("Calculate summary statistics", "Filter outliers", "Convert unformatted text to table").

---

### 🔹 Mini-Phase 7: Audio & Voice Engine (Waveform & Synthesizer)
* **Goal**: A lightweight audio recording, waveform trimming, and TTS generation studio.
- [ ] **7.1**: Integrate lightweight `Media3` audio playback and microphone recorder.
- [ ] **7.2**: Implement interactive audio waveform visualizer (scrub, range selection, cut/trim).
- [ ] **7.3**: Add **Bottom Audio Takes Strip**: Horizontal card row of recorded takes, synthesized TTS voices, and audio clips.
- [ ] **7.4**: Connect AI voice & transcription tools (Speech-to-Text transcript generator, AI voice synthesizer, audio cleaner).

---

### 🔹 Mini-Phase 8: Video & Clips Engine (Lightweight Trimmer & Timeline)
* **Goal**: A simple video viewport and clip trimmer with AI assist.
- [ ] **8.1**: Integrate `Media3 / ExoPlayer` video viewport with play/pause, scrub bar, and aspect ratio switcher (16:9, 9:16, 1:1).
- [ ] **8.2**: Implement simple timeline trimmer bar with start/end trim handles.
- [ ] **8.3**: Add **Bottom Video Scene Strip**: Horizontal cards showing video scenes, B-roll suggestions, and exported clips.
- [ ] **8.4**: Connect AI video tools (Automated subtitle generator, pause/silence detector, scene cut suggestions).

---

### 🔹 Mini-Phase 9: Cross-Engine Synergy & Unified Workspace Polish
* **Goal**: Seamless multi-engine workflows and comprehensive polish.
- [ ] **9.1**: Unify artifact drag-and-drop / insertion across all engines (e.g., drag a generated image from Image Canvas into a Presentation slide).
- [ ] **9.2**: Enable AI cross-referencing (e.g., Code Engine reading a database schema from the Table Engine while consulting a UI sketch from the Canvas Engine).
- [ ] **9.3**: Full offline persistence for all work engines via Room database and local filesystem.
- [ ] **9.4**: End-to-end testing and performance optimization across all tab engines.
