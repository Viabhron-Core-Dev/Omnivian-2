2026-08-08T07:04:00-07:00
Requested: 
Implement Phase 9.3 (Secure Guided WebView / Direct-to-Key)
Touched: 
- app/src/main/java/com/example/ui/settings/omniroute/DirectToKeyWebViewScreen.kt
- app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt
- app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt
- app/src/main/java/com/example/ui/OmniRouteApp.kt
Done: 
- Implemented `DirectToKeyWebViewScreen` wrapping Android's standard `WebView` in `AndroidView`.
- Configured WebView with strict security (`MIXED_CONTENT_NEVER_ALLOW`, JS enabled only for UI, Safe Browsing assumed via OS defaults).
- Created a "Paste & Save" overlay dialog for capturing `alias` and `pastedKey` manually (bypassing the need for unstable session extraction).
- Added `saveRealKey` to `AiManagerViewModel` that handles masking (`sk-...1234`) and persists it to `ApiKeyEntity`.
- Connected `DirectoryTab`'s "Add Key" button to the new WebView screen.
Verified: local build only (compile_applet passed).
Deviation: None.
Issue: None.
