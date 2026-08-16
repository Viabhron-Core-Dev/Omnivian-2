# Phase 5: Code Editor Updates & Local PR/Issue System

## Overview
This phase expands the Code Editor to support local Issues and Pull Requests. These features are designed as discrete tools (similar to `read_file` or `edit_file`) that can be triggered manually by the user or autonomously by Agents (Phase 11/12). 

This creates a collaborative environment where a user can open a PR for an agent to review, or an agent can open a PR for the user (or another agent) to merge.

## UI Additions

### 1. File Explorer Tabs
- **Location**: Top of the right-side File Explorer drawer.
- **Tabs**:
  1. **File Tree**: The standard repository file viewer.
  2. **Issues**: A list of active local issues (user-created or agent-created).
  3. **Pull Requests**: A list of proposed code changes pending review.

### 2. Context Menu Actions (File Tree)
- **Location**: The 3-dot menu on any file or folder in the "File Tree" tab.
- **New Actions**:
  - `Create Issue`: Open a dialog to define a problem or feature request linked to the specific file/folder.
  - `Create PR`: Propose a change based on a copy of the selected file/folder.

### 3. Editor "Save as PR" (Main Code View)
- **Location**: Inside the main Code Editor view when a file is open.
- **Functionality**: When a user modifies a file in the editor, instead of a standard "Save" (which immediately overwrites the file), they can click **"Save as Pull Request"**. 
- **Mechanism**: This captures the diff of the modified editor state versus the original file and generates a new PR entry in the "Pull Requests" tab. An agent can then be pinged to review or approve it.

## Tool Infrastructure Mapping
These UI actions map directly to underlying engine functions that will be exposed to agents as tools:
- `create_issue(title, body, target_file)`
- `create_pull_request(title, diff, target_file)`
- `review_pull_request(pr_id, feedback, action)`
- `merge_pull_request(pr_id)`
