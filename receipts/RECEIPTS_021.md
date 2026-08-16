2026-08-05T17:23:00-07:00
* Requested: Remove artefacts from dropdown, move file revert button to a small button after AI reply with diff icon, fix bottom navigation bar items (Thread Settings, Export, Remix, OmniRoute Dashboard) and make them squarer and shorter.
* Files touched: `ChatScreen.kt`, `WorkspaceActionsBottomSheet.kt`
* Action: Removed "List of Artefacts" from Chat dropdown. Added Restore and Diff icons below `AiMessage`. Updated `WorkspaceActionsBottomSheet` layout to use a single row of 4 square buttons (`80.dp`, `RoundedCornerShape(12.dp)`) mapped to "Thread Settings", "Export", "Remix", "OmniRoute Dashboard".
* Verification: Local build pending.
* Deviation: None.
* Known issue/Follow-up: None.
