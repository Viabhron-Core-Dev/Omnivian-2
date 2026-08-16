2026-08-07T23:58:30-07:00
Requested: 
Discuss adding "Upload files" and "Upload/Extract Zip" to the File Explorer 3-dot menu.
Touched: None (Discussion only).
Done: 
- Analyzed `FileExplorer.kt` and `LocalFileManager.kt`.
- Outlined plan to add `rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents())` in `FileExplorer`.
- Documented changes needed in `FileTreeNodeView` to add the context menu options.
- Discussed URI content resolution and mapping zip extraction to `LocalFileManager.unzipFile()`.
Verified: Not tested.
Deviation: None.
Issue: None.
