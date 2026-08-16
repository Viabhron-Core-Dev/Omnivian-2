* 2026-08-10T05:40:00-07:00
* Request: Phase 9.7: The Unkillable Local Proxy (Foreground Service)
* Touched: app/src/main/AndroidManifest.xml, app/src/main/java/com/example/engine/server/PreviewServerManager.kt, app/src/main/java/com/example/ui/chat/ArtifactsListBottomSheet.kt, app/src/main/java/com/example/engine/omniroute/service/OmniRouteProxyServer.kt, app/src/main/java/com/example/engine/omniroute/service/OmniRouteProxyService.kt, app/src/main/java/com/example/MainActivity.kt
* Action: Updated PreviewServer port to 8081 to free up 8080. Added Foreground Service permissions to Manifest. Created OmniRouteProxyServer (NanoHTTPD) running on port 8080. Created OmniRouteProxyService to host the proxy as a Foreground Service with a persistent notification. Launched the service on app boot in MainActivity.
* Verification: Compiling now.
* Build completed successfully. Verified the fix locally.
* 2026-08-10T06:02:00-07:00
* Request: Fix Cleartext issue, Log Keeper, Chat Input Model Selector, and Ai Settings Models placeholder
* Touched: app/src/main/AndroidManifest.xml, app/src/main/java/com/example/ui/chat/OmniRouteClient.kt, app/src/main/java/com/example/ui/chat/ChatScreen.kt, app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt
* Action: Added `usesCleartextTraffic="true"` to Manifest. Integrated LogKeeper calls in OmniRouteClient. Replaced dummy clickable in ChatScreen with a DropdownMenu for model selection. Implemented `ModelsTab()` in AiManagerPanelScreen to replace the CenterTextTab placeholder. Fixed minor compiler errors.
* Verification: Compiling now.
* Build completed successfully. Verified locally.
* 2026-08-10T13:35:00-07:00
* Request: Phase 9.5 and wire the ViewModel to query the Room Database (ApiKeyDao). One step at a time. Implement.
* Touched: app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt, app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt, app/src/main/java/com/example/ui/chat/ChatScreen.kt, app/src/main/java/com/example/ui/chat/OmniRouteClient.kt, app/src/main/java/com/example/engine/omniroute/service/OmniRouteProxyServer.kt
* Action: Updated AiManagerViewModel to dynamically compute availableModels based on the active keys in the database. Updated ModelsTab and ChatScreen to use this dynamic list from the viewmodel. Updated OmniRouteClient to pass the selected model to the proxy server. Updated OmniRouteProxyServer to parse the requested model, query the corresponding active API key from the database, use TranslationEngine to format the request, forward it to the real provider endpoint (OpenAI, Gemini, Anthropic, OpenRouter, Groq, Together), and translate the response.
* Verification: Built successfully.
* 2026-08-10T13:35:30-07:00
* Request: Fix syntax errors in OmniRouteProxyServer introduced by shell script string interpolation (which stripped $ characters).
* Touched: app/src/main/java/com/example/engine/omniroute/service/OmniRouteProxyServer.kt
* Action: Overwrote the file directly via create_file to prevent variable interpolation issues.
* Verification: Compiling now.
* Build succeeded. Tested locally and active models fetch via proxy perfectly.
* 2026-08-11T14:40:00-07:00
* Request: Fix the blank bubble issue by handling 404 API errors properly and switching the Gemini API endpoint to the OpenAI-compatible version.
* Touched: app/src/main/java/com/example/engine/omniroute/service/OmniRouteProxyServer.kt
* Action: Updated the Gemini route to use `ProviderFormat.OPENAI` with the `generativelanguage.googleapis.com/v1beta/openai/chat/completions` endpoint. Added a check for `!response.isSuccessful` in the proxy to capture error messages, generating a valid `OmniResponse` containing the error details via Moshi to ensure proper JSON escaping, preventing blank bubbles.
* Verification: Verified that the project successfully compiled with `gradle :app:assembleDebug`.
