2026-08-08T01:17:30-07:00
Requested: 
Implement Code Editor changes: "Save as PR", File Explorer tabs, and 3-dot context menu for PR/Issue.
Touched: app/src/main/java/com/example/engine/db/WorkspaceIssueEntity.kt, app/src/main/java/com/example/engine/db/WorkspacePullRequestEntity.kt, app/src/main/java/com/example/engine/db/WorkspaceIssueDao.kt, app/src/main/java/com/example/engine/db/WorkspacePullRequestDao.kt, app/src/main/java/com/example/engine/db/AppDatabase.kt, app/src/main/java/com/example/ui/code/FileExplorer.kt, app/src/main/java/com/example/ui/code/CodeScreen.kt
Done: 
- Created Room entities `WorkspaceIssueEntity` and `WorkspacePullRequestEntity` along with their Daos. Added them to `AppDatabase` (bumped to v3).
- Added `TabRow` to `FileExplorer` with "Files", "Issues", and "PRs" tabs. Integrated simple local `IssuesTab` and `PullRequestsTab` using Room state collection.
- Added "Create Issue" and "Create PR" dialogs and dropdown options to `FileTreeNodeView`'s 3-dot menu.
- Added "Save as Pull Request" to `CodeScreen`'s editor dropdown menu, which saves the current editor content as a PR diff.
Verified: local build only (Gradle compiled successfully).
Deviation: None.
Issue: None.
