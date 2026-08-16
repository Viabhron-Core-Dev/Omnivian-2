# Receipts Log

2026-08-05T09:22:15-07:00
* Requested: Build Phase 1 and configure GitHub Actions Gradle workflow.
* Files touched: `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `metadata.json`, `app/build.gradle.kts`, `.github/workflows/build.yml`, `app/src/main/java/com/example/MainActivity.kt`, `app/src/main/java/com/example/utils/LogKeeper.kt`, `app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt`, `app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt`, `app/src/main/java/com/example/ui/chat/ChatScreen.kt`, `app/src/main/java/com/example/ui/code/CodeScreen.kt`, `app/src/main/java/com/example/ui/OmniRouteApp.kt`
* Action: Updated app ID and names for OmniRoute IDE. Added GitHub Actions workflow to build the APK using `gradle assembleDebug` on push to main or dispatch. Built the UI skeleton (Global Sidebar, Fixed Bottom Nav, Dual-tab layout for Chat and Code with dummy Code right-drawer). Created LogKeeper utility for shell-level logging.
* Verification: local build only (lint and compile).
* Deviation: Used simple `ModalNavigationDrawer` inside `CodeScreen` with `LocalLayoutDirection` inverted for RTL to position the file tree drawer on the right side.
* Known issue/Follow-up: File tree uses dummy paths, needs hooking up to local file system in future phases. LogKeeper needs to be integrated into actual engine layers.

2026-08-05T09:36:00-07:00
* Requested: Fix the Android APK GitHub actions build pipeline failing with AGP version requirements.
* Files touched: `/.github/workflows/build.yml`
* Action: Updated the setup-gradle action to v4, set gradle-version to 9.3.1, added `validate-wrappers: false`, and appended a step to upload the built APK as a workflow artifact.
* Verification: Not tested locally (GitHub Actions fix only).
* Deviation: None.
* Known issue/Follow-up: Ensure next workflows run correctly on `main` push.

2026-08-05T09:58:00-07:00
* Requested: Update app name to OmniVian and implement Google AI Studio chat UI/bottom nav layout.
* Files touched: `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `metadata.json`, `app/build.gradle.kts`, `app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt`, `app/src/main/java/com/example/ui/chat/ChatScreen.kt`
* Action: Updated app ID to `shura.omnivian` and names to `OmniVian`. Rewrote the bottom navigation to be a full-width Surface with a centered pill-shaped tab selector. Updated ChatScreen to include a rounded Surface input box (with no microphone icon) and Google AI Studio style message bubbles.
* Verification: local build only.
* Deviation: None.
* Known issue/Follow-up: Log Keeper UI will be built in a later phase as confirmed.
* 2026-08-14
* Implemented the SmolChat-style Local AI Engine architectural improvements:
* Created `LocalAiManager.kt` as a singleton to load and cache the `LlamaEngine` in memory. This "keep-alive" engine eliminates the massive 10-second overhead of re-loading the 300MB+ `.gguf` file into RAM on every single generation turn.
* Patched `ChatScreen.kt` to use `indexOfFirst { it.id == generatingMessage.id }` instead of `indexOf(generatingMessage)` to perfectly track state and fix the "only streams one word" bug caused by changing memory hashes in Compose.
* Updated `ChatScreen.kt` local bypass logic to use SmolLM's expected `ChatML` tokens (`<|im_start|>` and `<|im_end|>`) instead of naive newlines for improved agent output and memory context retention.
* Wired up `LogKeeper` telemetry inside `LocalAiManager.kt` and `ChatScreen.kt` to report engine load times, token streaming speed, and TPS directly to the debug panel.
* Verified successful compilation via `compile_applet`.
