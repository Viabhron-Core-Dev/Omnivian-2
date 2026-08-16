2026-08-08T10:48:00-07:00
Requested: 
Discuss adding the ability to adjust the order of priority for AI models per chat thread, check repo state, and list required fixes. (No coding/blueprint updates).
Touched: None (Discussion only).
Done: 
- Checked the repository state: `AgentSettingsBottomSheet.kt` has a static UI placeholder for "Model Order" but no actual logic. `ThreadSettingsScreen.kt` only stores a single `model` string in `WorkspaceConfigEntity`. `FallbackChainEntity` exists in Room but isn't wired to individual chat threads.
- Listed required fixes:
  1. **Data Layer Update:** Modify `WorkspaceConfigEntity` to link to a `FallbackChainEntity` (or store a JSON list of priorities) rather than a single `model` string.
  2. **UI Implementation:** Build an interactive drag-and-drop or selection UI (replacing the static placeholder in `AgentSettingsBottomSheet`) to let users pick and order models from the Phase 9 directory.
  3. **Database Binding:** Save the user's custom model order into the Room Database (`FallbackChainDao`).
  4. **Engine Integration:** Ensure the Phase 9 routing engine dynamically reads this thread-specific chain instead of a global default when the agent executes tasks.
Verified: Not tested.
Deviation: None.
Issue: None.
