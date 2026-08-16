2026-08-08T13:56:00-07:00
Requested: 
Check current repo against planned fixes for the "Tools" Global Settings page (collapsible categories by MCP, starting with On-Device tools). Discuss without coding.
Touched: None (Discussion only).
Done: 
- Checked `SettingsPlaceholders.kt` for `ToolsSettingsContent`.
- Found that it currently uses a flat, static list of two placeholder tools ("File System Tool", "Shell Tool") and lacks any categorization or folding logic.
- Listed required fixes:
  1. Data structure to group tools by MCP categories (e.g., "On-Device (Local)", "GitHub", "Google Drive").
  2. Implement an expandable/collapsible UI (accordion style) for each category using state variables.
  3. Populate the "On-Device" category with tools like "Edit File", "Create PR", "Create Issue", "View File", etc.
  4. Add trailing dropdowns or permission toggles (e.g., "Always Ask", "Use Freely") to each tool item.
Verified: Not tested.
Deviation: None.
Issue: None.
