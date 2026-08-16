# OmniRoot Evolution Roadmap: Integration into Master Blueprint Phases

This document details the exact integration of Kai 9000-inspired capabilities, advanced artifact systems, and secure on-device infrastructure directly mapped into the master **`BLUEPRINT.md`** development phases (Phases 8, 10, 11, and 13).

---

## 1. Master Phase Mapping Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ PHASE 8: ARTIFACTS & PREVIEWS (Mini-Phases 8.1 – 8.8)                                       │
│ 👉 Dynamic Interactive Live UI (Two-Tier Artifacts & Smart Folding)                         │
│    • Standalone Fullscreen/Modal Artifacts: Web pages, PWAs, full-screen micro-apps         │
│    • Inline Interactive Artifacts: In-bubble buttons, forms, tables, calculators, sliders   │
│    • Unfolded Load Only: Dynamic state/recomposition renders only when container is expanded │
│    • Active Turn Default: The latest AI reply AND triggering user prompt remain unfolded    │
│    • Viewport Controls: "Unfold All" (global) and "Unfold All on Screen" (viewport only)    │
│    • Hardware 3D & Decks: Three.js WebGL2 orbit touch controls & Reveal.js slide decks      │
│    • AI Tool Bindings: `render_inline_ui`, `create_3d_artifact`, `create_presentation`      │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ PHASE 10: NATIVE TOOL EXECUTOR & MCP REGISTRY                                               │
│ 👉 Sandboxed Linux Environment & Developer CLI Agents (Tool Structure)                      │
│    • Unified Tool/MCP Binding: `run_sandbox_command` executable by Local & Cloud AI         │
│    • Rootless Alpine/Debian userland via PRoot (~3 MB download, no root required)           │
│    • 1-Tap Package Installers: `bash`, `curl`, `wget`, `git`, `jq`, `python3`, `nodejs`     │
│    • Developer CLI Runners: Claude Code, OpenCode, Grok CLI + Embedded Interactive Terminal│
│ 👉 Android System Tools & Permission Guardrails                                             │
│    • System tools: Native Text-to-Speech (TTS), Push Notifications, CalendarContract Events  │
│    • Explicit Permission Dialogs: [Allow Once] | [Always Allow for Workspace] | [Deny]     │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ PHASE 11: THE BRAIN, MEMORY & SKILL LIBRARY                                                 │
│ 👉 Soul & Self-Reinforcing Memory System ("Soul Skills")                                    │
│    • Dedicated "Soul Skills" folder in the global Skills Library                             │
│    • Application Scope Toggles: [Global (All Chats)] | [Workspace Specific] | [Ignored]     │
│    • In-Chat Skill Creation: Save multi-step workflows & prompt templates to Skill Library     │
│    • Self-Reinforcing Memory Loop: Extract chat facts & promote high-confidence learnings    │
└──────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────────────────────────────────────┐
│ PHASE 13: STORAGE SECURITY & ARCHITECTURE                                                   │
│ 👉 Local Conversation Storage with Hardware-Backed Encryption                               │
│    • SQLCipher 256-bit AES database encryption for all Room tables (Messages, Artifacts)    │
│    • Android Keystore Hardware Module (TEE / StrongBox) for master encryption key management│
│    • Strict Zero-Cloud Data Leakage guarantee for offline GGUF local workspaces             │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Phase-by-Phase Technical Specifications

### [PHASE 8] Artifacts & Previews: Live UI, 3D Graphics & Tool Bindings (8.1 – 8.8)

Refer to **`PHASE_8_MINI_PHASES.md`** for the detailed breakdown across:
- **8.1**: Smart Folding Engine & Active Turn State Machine
- **8.2**: Viewport Controls & Batch Fold/Unfold Actions
- **8.3**: Multi-Format Artifact Classifier (3D, Slide Decks, Charts, Standalone Apps, Inline UI)
- **8.4**: Inline Declarative UI Protocol & Parser (`json:ui`)
- **8.5**: Native Jetpack Compose Inline UI Renderer
- **8.6**: Hardware-Accelerated 3D WebGL & Presentation Runtime
- **8.7**: Standalone Preview Suite, Console Bridge & Export
- **8.8**: AI Tool Bindings, MCP Integration & Bidirectional Action Protocol

---

### [PHASE 10] Native Tool Executor: Linux Sandbox, Developer CLI & System Tools

1. **Rootless Linux Sandbox (`PRoot` + Alpine mini-rootfs):**
   - Packaged/downloadable ~3.5 MB Alpine `aarch64`/`x86_64` rootfs.
   - Uses `ptrace()` system call hooking via `proot` to run native Linux executables inside app storage.
   - 1-Tap Package Sheet: Installs `python3`, `pip`, `nodejs`, `npm`, `git`, `jq`, `curl`, and `bash`.

2. **Developer CLI Agents & Terminal:**
   - Run CLI assistants such as `@anthropic-ai/claude-code`, OpenCode, or Python scripts.
   - Interactive ANSI Terminal emulator tab embedded into the workspace tools panel.

3. **Android System Integrations & Permission Guardrails:**
   - **Text-to-Speech (TTS):** Integrated `android.speech.tts.TextToSpeech` for hands-free audio listening.
   - **System Tools:** Notifications, Calendar event scheduler (`CalendarContract`), and Web Scraping.
   - **Permission Gate:** Any tool touching the Linux sandbox, file modifications, or device sensors triggers a modal permission prompt: `Allow Once`, `Always Allow`, or `Deny`.

---

### [PHASE 11] The Brain & Memory: Soul Skills Engine

1. **Soul Skills Directory in Skill Library:**
   - Skills Library features a primary **"Soul Skills"** category.
   - User-configurable toggles per soul trait:
     - `Applied to All Chats (Global Soul)`
     - `Workspace / Chat Specific`
     - `Ignored (Disabled)`
2. **Self-Reinforcing Memory Promotion:**
   - Background extraction identifies key facts, preferences, and recurring instructions.
   - High-frequency memories are promoted into structured Soul Skills with full user visibility and editability.

---

### [PHASE 13] Storage Security: Encrypted Database & Keystore

1. **SQLCipher Room Integration:**
   - 256-bit AES full-database encryption for conversations, prompt templates, memories, and artifacts.
2. **Android Keystore System:**
   - Master passphrase/key generated securely inside hardware TEE/StrongBox.
   - Automatic key derivation without plain-text storage.
