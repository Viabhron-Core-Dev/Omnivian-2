2026-08-08T00:30:30-07:00
Requested: 
Discuss adding tabs (File tree, issues, pull request) to the file explorer drawer. Discuss adding PR/Issue creation to the file/folder 3-dots menu. Discuss multi-agent system (planner, PR agent, reviewer) acting like a PR flow to save tokens.
Touched: None (Discussion only).
Done: 
- Analyzed `FileExplorer.kt` and `CodeScreen.kt`. Found no tabs or PR/Issue options currently present.
- Outlined plan for Code Editor Part: Add `TabRow` to `FileExplorer` with "File Tree", "Issues", and "Pull Requests" tabs. Add "Create Issue" and "Create PR" to the `DropdownMenuItem` list in `FileTreeNodeView`.
- Outlined plan for Agent Phase (Phase 11/12): Implement an agentic workflow where a Planner outlines the change (creating an Issue), a Coder creates a Pull Request with the proposed changes, and a Reviewer evaluates the PR before merging. This separates responsibilities and optimizes token usage.
Verified: Not tested.
Deviation: None.
Issue: None.
