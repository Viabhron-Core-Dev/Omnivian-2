* 2026-08-12T13:13:00-07:00
* Request: Shrink the Chat input box model picker pill to show only initials (e.g. "O / G") so it doesn't push the Send button off the screen with long model IDs.
* Touched: app/src/main/java/com/example/ui/chat/ChatScreen.kt
* Action:
  * Modified the `Agent/Model Selector Pill` row in `ChatScreen.kt` to extract the initials of the `providerId` and `modelId` (e.g., `openai/gpt-4o` -> `O / G`).
  * Assigned the initials to a small, fixed-size pill string that renders in place of the full raw string.
  * Preserved the `DropdownMenu` so the user can still read the full model names when the pill is tapped.
* Verification: Built successfully.
