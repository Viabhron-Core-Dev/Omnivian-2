2026-08-05T16:58:30-07:00
* Requested: Update Action History Refinement to be tracked by app not ai, and render as a standalone list of actions.
* Files touched: `ChatScreen.kt`
* Action: Updated `ChatMessage` and `ActionHistoryCard` to separate App Action messages (`MessageRole.APP_ACTION`) from AI messages (`MessageRole.AI`). App Actions now render standalone log cards tracking what tools/actions were done by the AI (e.g., Searched Workspace, Read file, etc.).
* Verification: Local build pending.
* Deviation: None.
* Known issue/Follow-up: None.
