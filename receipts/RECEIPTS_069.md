2026-08-07T13:45:00-07:00
Requested: 
1. Add a New Chat options window to prompt for Thread Name, App Type, Model, Integrations, and System Instructions.
2. Collapse all messages in the chat thread except the very last one.
3. Reduce the height of the 3-dot Workspace Actions Bottom Sheet to roughly 1/4 of the screen.
Touched: app/src/main/java/com/example/ui/OmniRouteApp.kt, app/src/main/java/com/example/ui/chat/NewChatDialog.kt, app/src/main/java/com/example/ui/chat/ChatScreen.kt, app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt
Done: 
- Created `NewChatDialog.kt` with fields for Thread Name, Application Type, AI Model, Skills/Tools, and Instructions. Integrated it into `OmniRouteApp.kt` so it shows when `onNewChat` is clicked.
- Updated `ChatScreen.kt`'s `LazyColumn` and message composables (`UserMessage`, `AiMessage`, `AppActionMessage`, `ActionHistoryCard`) to accept an `isLastMessage` boolean, defaulting to collapsed if it's not the last message.
- Shrunk grid item size to 80.dp and reduced vertical paddings in `WorkspaceActionsBottomSheet.kt` to make it visually shorter (approx 1/4 of the screen height).
Verified: local build only (Gradle compiled successfully).
Deviation: None.
Issue: None.
