2026-08-08T14:11:00-07:00
Requested: 
Mark Firebase Python Sandbox as on-hold (conceptually, no blueprint edit). Audit repo against planned Tools Settings fixes (Search, Expandable MCP categories, Permission toggles, JS Sandbox). Discuss only.
Touched: None (Discussion only).
Done: 
- Acknowledged Firebase Python Sandbox is paused.
- Checked `SettingsPlaceholders.kt` for `ToolsSettingsContent`.
- Current Repo State: `ToolsSettingsContent` is a static placeholder with a simple column and two hardcoded `ListItem` elements ("File System Tool", "Shell Tool"). It lacks state management, search, categories, and dropdowns.
- Listed required fixes for future implementation:
  1. Add a `Tool` data model and ViewModel to handle dynamic tool lists that refresh on MCP connection.
  2. Implement an `OutlinedTextField` search bar at the top to filter the tool list.
  3. Build an accordion-style expandable UI for categories, forcing "On-Device" (Edit, PR, Issue, JS Sandbox, etc.) to sort to the top.
  4. Replace the static trailing text with an interactive DropdownMenu to toggle permissions ("Always Ask", "Use Freely", "No Permission").
  5. Add the "JS Sandbox" as a core local tool capable of saving user/AI-generated scripts.
Verified: Not tested.
Deviation: None.
Issue: None.
