2026-08-08T13:33:00-07:00
Requested: 
Review the codebase and blueprint against the refined placeholder fix requests (Local PRs, Reverting via Chat/Versions instead of Code Editor, Global Settings expansions including Encrypted Backup, and a GDrive-backed Component Library Bottom Sheet). Discuss without coding or blueprint updates.
Touched: None (Discussion only).
Done: 
- **Local PRs:** Confirmed `CodeScreen.kt` currently uses `WorkspacePullRequestDao` to save PRs locally to Room. This perfectly aligns with the request for on-device repo PRs, not GitHub PRs.
- **Revert Mechanics:** Outlined the plan to remove the Revert dialog from the Code Editor. Instead, the logic will be wired to the existing `ActionHistoryCard` in `ChatScreen` (which already has an inactive restore icon) and the `VersionsSettingsContent` in `ThreadSettingsScreen` (replacing the static "v1.0.2" with a dynamic list of previous chat replies for snapshot restoration).
- **Global Settings:** Noted the need to replace the `Pending implementation` route placeholders with actual Compose screens for Skills, Tools, MCP, Plugins, Integrations, Permissions, and Font & Typography. Planned the addition of an "Encrypted Backup & Restore" feature.
- **Component Library:** Planned replacing the "coming soon" toast in `GlobalSidebar.kt` with a `LibraryBottomSheet` that acts as a CRUD file explorer for GDrive-synced user uploads.
Verified: Not tested.
Deviation: None.
Issue: None.
