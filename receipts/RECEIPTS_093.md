2026-08-08T12:26:00-07:00
Requested: 
Implement the fix to wire the "AI Token Panel" in the Workspace Actions bottom sheet.
Touched: 
- app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt
- app/src/main/java/com/example/ui/OmniRouteApp.kt
Done: 
- Removed the static `AlertDialog` placeholder (`showOmniRouteDialog`) in `WorkspaceActionsBottomSheet`.
- Added an `onTokenPanelClick` callback parameter to the bottom sheet.
- Hooked up the callback in `OmniRouteApp` to toggle a new `showTokenPanel` state.
- Rendered `com.example.ui.chat.AiTokenPanelBottomSheet` conditionally at the `OmniRouteApp` scaffold root level, so the panel can now be summoned globally from the bottom navigation bar.
Verified: local build only (compile_applet passed).
Deviation: None.
Issue: None.
