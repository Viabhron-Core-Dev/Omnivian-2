# Phase 5: Code Editor (Completed)

## Overview
Phase 5 introduces the Code Editor (Acode Style) and integrates it with the File Explorer. It allows users to view, edit, save, and manage code files directly from the UI. It also introduces native file history engine to allow reverting changes locally.

## Features Implemented
* **Code Editor UI (`CodeScreen`, `TextViewer`)**:
  * An editable text field using `BasicTextField` that supports viewing and modifying file contents.
  * **Top Bar Controls**:
    * Save Button
    * Copy to Clipboard Button
    * Download Button (Placeholder integration)
    * Server Start/Stop Button
    * File Explorer Drawer Toggle
  * **3-Dots Menu Options**:
    * Find & Replace (Placeholder)
    * Go to Line (Placeholder)
    * Check Syntax Error (Placeholder)
    * Toggle Line Wrap (Implemented via `horizontalScroll` and `Modifier.fillMaxWidth()` changes)
    * File History (Revert)
  * **Line Wrapping**: Text wraps for view if enabled, otherwise scrolls horizontally.

* **Native File Revert UI & History Engine (`FileHistoryEngine`, `FileRevertDialog`)**:
  * Captures a timestamped `.txt` copy of the file into a hidden `.history_filename` folder every time `saveRevision()` is called before saving.
  * A dialog displays the history list with timestamps and sizes.
  * Revert functionality replaces the current file with the selected history state and reloads the editor.

* **Live Generation View State (`CodeEditorState.isLiveGeneration`)**:
  * Built-in state to lock the editor (read-only mode) while an AI agent is generating or streaming code changes to the file.

### Phase 5.5: Editor Advanced Features
* **Find & Replace**: A dialog to find strings and replace them, with match case support.
* **Go to Line**: A dialog to jump the cursor to a specific line number.
* **Simple Syntax Check**: A lightweight bracket/quote matching parser to find unclosed tokens without invoking the full compiler.
