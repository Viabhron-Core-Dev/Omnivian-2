2026-08-07T13:58:30-07:00
Requested: 
Wire the extracted data from the New Chat Dialog into a persistent database so the custom Agent configurations are actually remembered by the thread.
Touched: app/src/main/java/com/example/engine/db/WorkspaceConfigEntity.kt, app/src/main/java/com/example/engine/db/WorkspaceConfigDao.kt, app/src/main/java/com/example/engine/db/AppDatabase.kt, app/src/main/java/com/example/ui/OmniRouteApp.kt, app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt
Done: 
- Created `WorkspaceConfigEntity` and `WorkspaceConfigDao` to persist thread configuration via Room.
- Added `WorkspaceConfigEntity` to `AppDatabase` and bumped its version to 2 with a destructive migration fallback.
- Updated `OmniRouteApp.kt` to save the selected New Chat configurations into Room upon creation via `db.workspaceConfigDao().saveConfig(...)`.
- Updated `OmniRouteApp.kt` to pass the active `chatSessionId` down into `ThreadSettingsScreen`.
- Refactored `ThreadSettingsScreen.kt` and `UniversalSettingsContent` to fetch the configurations from the database and populate the inputs dynamically, while syncing changes back into Room.
Verified: local build only (Gradle compiled successfully).
Deviation: None.
Issue: None.
