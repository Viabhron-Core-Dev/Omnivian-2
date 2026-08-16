2026-08-08T00:51:30-07:00
Requested: 
Clarify that the Code Editor UI additions (Tabs for Files/Issues/PRs and context menu options) should be implemented now, while the Multi-Agent (Planner/Reviewer) logic belongs to Phase 11/12.
Touched: None (Discussion only).
Done: 
- Clarified the separation of concerns.
- Code Editor Phase (Now): Add TabRow to `FileExplorer.kt` for "Files", "Issues", and "Pull Requests". Add "Create Issue" and "Create PR" to the file/folder 3-dot menus to allow users to generate local issues/PRs. 
- Agent Phase (Phase 11): Build the multi-agent logic (Planner, Coder, Reviewer) that will utilize these local Issues/PRs like a GitHub MCP workflow, controlled by optional thread settings.
Verified: Not tested.
Deviation: None.
Issue: None.
