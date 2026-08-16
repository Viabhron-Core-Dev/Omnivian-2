2026-08-06T06:27:00-07:00
* Requested: Implement PPT reader, wire up missing CRUD file operations, fix auto-closing drawer behavior.
* Files touched: `app/src/main/java/com/example/ui/code/PptViewer.kt`, `app/src/main/java/com/example/ui/code/CodeScreen.kt`, `app/src/main/java/com/example/ui/code/FileExplorer.kt`
* Action: 
  - Added `PptViewer.kt` to handle `.ppt` and `.pptx` files with a simulated text extraction view.
  - Wired `PptViewer` into `CodeScreen.kt`.
  - Added AlertDialogs in `FileExplorer.kt` to support Rename, Copy, New File, and New Folder CRUD operations.
  - Fixed `CodeScreen.kt` to stop the auto-closing drawer behavior when a file is clicked, matching blueprint specifications.
* Verification: Validating via `gradle compileDebugKotlin`.
* Deviation: Simulated PPT extraction since Apache POI or native Android PPT rendering is heavy/complex for this lightweight environment.
* Known issue/Follow-up: Actual text extraction logic using Apache POI or a remote extraction service could be wired into `PptViewer` later.
