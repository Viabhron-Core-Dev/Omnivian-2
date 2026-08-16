2026-08-08T11:18:00-07:00
Requested: 
Check the repository against the Phase 9.4 plan (Token Checker & Contextual AI Panel) and discuss the required fixes without coding, building, or updating the blueprint.
Touched: None (Discussion only).
Done: 
- Checked the repository state:
  - `ChatScreen.kt` currently has a hardcoded `TokenUsageBar` (45000 / 128000) rendered globally under the TopAppBar.
  - The Chat's 3-dot menu (`MoreVert`) exists but lacks an option to open the Token Panel.
  - There is no calculation logic for rate limiting windows (e.g., Requests Per Minute - RPM or Tokens Per Minute - TPM).
- Listed required fixes for Phase 9.4:
  1. **Calculation Logic:** Build a `TokenManager` or update `AiManagerViewModel` / `MetricsDao` to query recent `TokenUsageEntity` and `RequestLogEntity` entries within the last 60 seconds to calculate real-time RPM/TPM.
  2. **UI Implementation:** Create an `AiTokenPanelBottomSheet` that displays active models, token health (progress bars), remaining limits, and estimated costs.
  3. **Menu Integration:** Add an "AI Token Panel" item to the 3-dot `DropdownMenu` in `ChatScreen` that triggers the new bottom sheet.
  4. **Data Binding:** Replace the hardcoded `TokenUsageBar` in the Chat UI with real values derived from the database, or move it entirely into the new contextual bottom sheet.
Verified: Not tested.
Deviation: None.
Issue: None.
