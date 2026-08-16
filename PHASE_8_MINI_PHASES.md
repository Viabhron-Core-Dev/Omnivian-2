# Phase 8: Dynamic Live UI, Two-Tier Artifacts, 3D WebGL & Smart Folding Engine
## Granular Mini-Phases Breakdown (Phases 8.1 – 8.8)

This document provides a comprehensive, fine-grained breakdown of **Phase 8 (Artifacts & Previews)** into 8 focused, incremental, and easily verifiable mini-phases, including explicit AI tool bindings and bidirectional action loops.

---

### Master Architecture Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                PHASE 8 EXECUTION PIPELINE                                    │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.1: SMART FOLDING ENGINE & ACTIVE TURN STATE MACHINE                            │
│ - Active Turn Rule: Triggering user prompt + latest AI reply always unfolded by default     │
│ - Historic Turn Collapsing: Past conversation turns default to compact summary badges       │
│ - Unfolded-Load-Only Lifecycle: Memory, WebViews & recomposition suspended when folded      │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.2: VIEWPORT CONTROLS & BATCH FOLD/UNFOLD ACTIONS                               │
│ - TopBar Action Menu: "Unfold All" (global session) and "Fold All History"                 │
│ - Viewport-Aware Action: "Unfold All on Screen" reading `LazyListState.layoutInfo`          │
│ - Smooth Scroll & Anchor Preservation: Prevents visual jumps during bulk expand/collapse    │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.3: MULTI-FORMAT ARTIFACT CLASSIFIER & BADGING SYSTEM                           │
│ - Smart Content Classifier: Automatically identifies artifact categories:                   │
│   • 3D_SCENE (Three.js / WebGL / React Three Fiber / Canvas)                                │
│   • PRESENTATION_DECK (Reveal.js / HTML5 Slide Decks / Pitch Decks)                         │
│   • DATA_VISUALIZATION (Chart.js / ApexCharts / D3 / Mermaid diagrams / SVGs)               │
│   • STANDALONE_APP (Single-page React / Tailwind / JS / HTML apps)                          │
│   • INLINE_MICRO_UI (`json:ui` forms, calculators, sliders, checklists)                     │
│ - Rich Badging: Material 3 chip indicators with icons and interactive launch triggers       │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.4: INLINE DECLARATIVE UI PROTOCOL & PARSER (`json:ui`)                         │
│ - Declarative Schema Protocol: Standardized JSON for in-bubble interactive micro-apps       │
│ - Streaming Schema Tolerance: Resilient parsing that handles partial JSON during generation │
│ - Component Hierarchy: Containers (Card, Column, Row), Inputs (Text, Switch, Slider)       │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.5: NATIVE JETPACK COMPOSE INLINE UI RENDERER                                   │
│ - Dynamic Compose Component Tree: Renders native M3 widgets with zero WebView overhead      │
│ - Local State Engines: Interactive Calculators, Task Checklists, Dynamic Forms              │
│ - Live Bidirectional Callbacks: In-bubble button clicks emit action events back to AI       │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.6: HARDWARE-ACCELERATED 3D & PRESENTATION RUNTIME                              │
│ - WebGL2 & Touch-Orbit 3D Runtime: Smooth interactive rendering for Three.js in WebView    │
│ - Presentation Deck Player: Slide-to-slide touch swipe gestures, fullscreen presenter mode │
│ - Resource Throttler: Auto-suspends JS `requestAnimationFrame` loops when folded or hidden │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.7: STANDALONE PREVIEW SUITE, CONSOLE BRIDGE & EXPORT                           │
│ - Console Log Bridge: Streams JavaScript `console.log` into OmniRoot's native `LogKeeper`   │
│ - View Modes: Instant toggle between Code Inspector, Live Preview, and Split View         │
│ - One-Tap Export: Save to Workspace, Export as standalone HTML bundle or ZIP                │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ MINI-PHASE 8.8: AI TOOL BINDINGS, MCP INTEGRATION & BIDIRECTIONAL ACTION PROTOCOL           │
│ - Tool Registry Bindings: `render_inline_ui`, `create_3d_artifact`, `create_presentation`  │
│ - Provider Protocol Translation: Cross-mapped for Local GGUF and Cloud APIs (Gemini/Claude)│
│ - Bidirectional Tool-Result Loop: UI form submissions routed back as structured tool inputs │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Detailed Specifications per Mini-Phase

### Mini-Phase 8.1: Smart Folding Engine & Active Turn State Machine

- **Objective:** Eliminate UI stutter and memory bloat on mobile by rendering full content only for active context.
- **Active Turn Rule:**
  - When a user sends a prompt, the user message item sets `isFolded = false`.
  - The responding AI message streaming tokens sets `isFolded = false`.
  - All preceding turns are automatically set to `isFolded = true` (unless manually expanded).
- **Unfolded-Load-Only Pattern:**
  - Expensive composables (code highlighters, interactive widgets, WebViews) are enclosed in:
    ```kotlin
    if (!message.isFolded) {
        FullMessageBody(...)
    } else {
        FoldedSummaryBadge(...)
    }
    ```

---

### Mini-Phase 8.2: Viewport Controls & Batch Fold/Unfold Actions

- **Objective:** Give the user global and viewport-level control over reading density.
- **TopBar Dropdown Actions:**
  - **"Unfold All"**: Sets `isFolded = false` across all messages in the current session.
  - **"Fold All History"**: Sets `isFolded = true` for all messages except the current active turn.
  - **"Unfold All on Screen"**: Inspects `lazyListState.layoutInfo.visibleItemsInfo` and sets `isFolded = false` strictly for items currently intersecting the screen.
- **Scroll Anchor Preservation:**
  - Prevents erratic viewport jumps when multiple messages resize simultaneously during bulk folding/unfolding.

---

### Mini-Phase 8.3: Multi-Format Artifact Classifier & Badging System

- **Objective:** Categorize AI-generated output into distinct media types and show rich, descriptive badges.
- **Classifier Engine:**
  - Analyzes code blocks and tags to determine:
    1. `3D_SCENE`: HTML/JS importing Three.js, Babylon.js, WebGL `<canvas>`, or React Three Fiber.
    2. `PRESENTATION_DECK`: Reveal.js, Marp, or CSS slide deck structures with slide markers.
    3. `DATA_VISUALIZATION`: Chart.js, ApexCharts, D3.js, Mermaid diagrams, or standalone SVGs.
    4. `STANDALONE_APP`: Full HTML5/Tailwind/React single-page applications.
    5. `INLINE_MICRO_UI`: ````json:ui ... ```` declarative schemas for in-bubble micro-apps.
- **Rich Status Badges:**
  - Displays icon-accented chips when folded (e.g. `[🎮 3D Model • Three.js]`, `[📊 Slide Deck • 8 Slides]`, `[📈 Chart • Chart.js]`, `[🧮 Interactive Calculator]`).

---

### Mini-Phase 8.4: Inline Declarative UI Protocol & Parser (`json:ui`)

- **Objective:** Define a lightweight declarative JSON UI schema for conversational micro-apps.
- **JSON Schema Structure:**
  ```json
  {
    "type": "card",
    "title": "Loan Calculator",
    "children": [
      { "type": "textField", "id": "amount", "label": "Principal ($)", "value": "10000" },
      { "type": "slider", "id": "rate", "label": "Interest Rate (%)", "min": 1, "max": 15, "value": 5 },
      { "type": "button", "id": "calc_btn", "label": "Calculate", "action": "submit" }
    ]
  }
  ```
- **Streaming Tolerance:**
  - Parser gracefully ignores unclosed brackets or trailing commas while tokens are streaming in real-time.

---

### Mini-Phase 8.5: Native Jetpack Compose Inline UI Renderer

- **Objective:** Render live, native Material 3 widgets directly in the chat bubble with zero web overhead.
- **Dynamic Component Tree (`DynamicUiRenderer.kt`):**
  - Recursively maps JSON elements to native Compose widgets (`OutlinedTextField`, `Slider`, `Switch`, `Button`, `FilterChip`, `Card`, `Checkbox`).
- **Built-in Micro-App State Engines:**
  - **Live Calculator:** Numeric keypad updating display state locally.
  - **Task & Todo List:** Checkboxes with immediate check toggles.
  - **Dynamic Forms:** Field validation and local state aggregation.
- **Bidirectional Event Callback:**
  - Tapping an action button invokes `onAction(id, payload)` to send structured results back to the AI session.

---

### Mini-Phase 8.6: Hardware-Accelerated 3D & Presentation Runtime

- **Objective:** Provide a fast, hardware-accelerated runtime for 3D graphics, slide decks, and rich visualizations.
- **Sandboxed WebGL2 WebView (`ArtifactPreviewModal.kt`):**
  - Configures WebGL2 hardware acceleration, DOM storage, and touch-orbit drag gestures for 3D scenes.
- **Interactive Slide Deck Mode:**
  - Fullscreen presenter view with slide counters, left/right keyboard or swipe navigation, and slide transitions.
- **Resource & Battery Throttler:**
  - Automatically pauses WebGL animation loops and JS timers when the modal is closed or the message is folded.

---

### Mini-Phase 8.7: Standalone Preview Suite, Console Bridge & Export

- **Objective:** Finalize developer inspection tools, debugging bridges, and artifact persistence.
- **JavaScript Console Bridge:**
  - Captures `console.log`, `console.warn`, and `console.error` from WebViews and pipes them directly into OmniRoot's native `LogKeeper`.
- **Tri-Mode Viewer:**
  - Seamless toggle between:
    1. **Live Preview** (Interactive 3D / Deck / App)
    2. **Code Inspector** (Syntax-highlighted source code)
    3. **Split View** (Side-by-side on tablets/foldables)
- **One-Tap Workspace Export:**
  - Save generated artifacts into the active workspace directory or export as standalone `.html` / `.zip` bundles.

---

### Mini-Phase 8.8: AI Tool Bindings, MCP Integration & Action Protocol

- **Objective:** Expose first-class tool definitions to the AI model so it can deterministically trigger and update rich artifacts and receive user interaction events.
- **Tool Definitions in `AppToolRegistry`:**
  - `render_inline_ui(schema)`: Explicit tool for generating native Compose in-bubble UI components.
  - `create_3d_artifact(framework, scene_code)`: Explicit tool for creating 3D WebGL scenes.
  - `create_presentation(theme, slides_json)`: Explicit tool for creating Reveal.js slide decks.
  - `update_active_artifact(artifact_id, diff/content)`: Tool to update an existing live artifact in real-time.
- **Cross-Model Protocol Translation (`TranslationEngine.kt`):**
  - Converts tool declarations into OpenAI/Anthropic/Gemini function schemas for cloud providers, and into ChatML tool schemas for local GGUF models.
- **Bidirectional Tool-Result Loop:**
  - User interactions with rendered UI (form submits, button taps) format into `tool_result` messages that feed directly back into the AI context for multi-step workflows.

---

## Mini-Phases Verification & Acceptance Matrix

| Mini-Phase | Key Verification Criterion |
| :--- | :--- |
| **8.1** | Active user prompt + AI reply remain unfolded; historic turns collapse into summary badges. |
| **8.2** | "Unfold All", "Fold History", and "Unfold on Screen" actions expand/collapse the list smoothly without scroll jumps. |
| **8.3** | Classifier tags 3D models, slide decks, charts, and inline UI with distinct icons and badges. |
| **8.4** | `json:ui` code blocks stream and parse safely without crashing on partial JSON. |
| **8.5** | AI generates a calculator or form; user can type, slide, and tap natively inside the chat bubble. |
| **8.6** | Three.js 3D models rotate via touch gestures; Reveal.js slides swipe smoothly in fullscreen mode. |
| **8.7** | JS `console.log` appears in native LogKeeper; artifacts can be exported as standalone files. |
| **8.8** | AI models (Local & Cloud) deterministically call UI tools; button clicks and form submits loop back to the AI. |
