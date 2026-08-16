2026-08-08T11:21:00-07:00
Requested: 
Implement Phase 9.4 (Token Checker & Contextual AI Panel)
Touched: 
- app/src/main/java/com/example/engine/db/AiManagerDaos.kt
- app/src/main/java/com/example/ui/chat/TokenPanelViewModel.kt
- app/src/main/java/com/example/ui/chat/AiTokenPanelBottomSheet.kt
- app/src/main/java/com/example/ui/chat/ChatScreen.kt
Done: 
- Updated `MetricsDao` with queries to calculate `getTokensUsedSince` and `getRequestCountSince` for rate limits.
- Created `TokenPanelViewModel` which uses a 1-second interval ticker to track real-time TPM (Tokens Per Minute) and RPM (Requests Per Minute) over the last 60 seconds.
- Implemented `AiTokenPanelBottomSheet` with visual progress bars indicating quota health (turns red at 80%) and total estimated cost.
- Integrated the new panel into `ChatScreen`'s 3-dot dropdown menu.
- Removed the old hardcoded `TokenUsageBar`.
Verified: local build only (compile_applet passed).
Deviation: None.
Issue: None.
