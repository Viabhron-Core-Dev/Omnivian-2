2026-08-07T00:44:00-07:00
* Requested: Strip Gemini-specific JSON serialization, replace with standard generic messaging format, send to local endpoint (prep for OmniRoute proxy), and pass chat history.
* Files touched: `app/src/main/java/com/example/ui/chat/ChatScreen.kt`, `app/src/main/java/com/example/ui/chat/OmniRouteClient.kt`
* Action:
  - Removed Gemini JSON models (`GeminiRequest`, `GeminiContent`, etc.) in `OmniRouteClient.kt`.
  - Implemented standard generic JSON models (`OmniRequest`, `OmniMessage`, `OmniResponse`).
  - Set default `baseUrl` to `http://localhost:8080/v1/chat/completions` (placeholder for local proxy).
  - Modified `generateContent` to accept `List<ChatMessage>` instead of a string, passing the full context/history.
  - Updated `ChatScreen.kt` to pass `chatMessages` (filtering out the current loading message) to `OmniRouteClient`.
* Verification: Verified via local build (`gradle compileDebugKotlin`).
* Deviation: None.
* Known issue/Follow-up: The actual OmniRoute proxy integration (running the proxy) is pending Phase 9.
