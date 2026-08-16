* 2026-08-12T12:04:00-07:00
* Request: Stop hardcoding model names and fetch them dynamically using standard /v1/models endpoint for each provider to build an agnostic router architecture.
* Touched: app/src/main/java/com/example/engine/db/AiModelEntity.kt, app/src/main/java/com/example/engine/db/AiManagerDaos.kt, app/src/main/java/com/example/engine/db/AppDatabase.kt, app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt, app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt, app/src/main/java/com/example/engine/omniroute/service/OmniRouteProxyServer.kt
* Action:
  * Database: Created `AiModelEntity` and `AiModelDao` to cache model lists per provider. Bumped `AppDatabase` version to 5.
  * Fetcher: Updated `AiManagerViewModel` with `refreshModels()` that queries `/models` endpoint using OkHttp for active providers. Includes fallback lists just in case standard model list APIs aren't supported (like Anthropic).
  * UI: Added "Refresh from Providers" button to `ModelsTab` in `AiManagerPanelScreen`. `availableModels` now flows dynamically from `aiModelDao` in the format `{providerId}/{modelId}`.
  * Proxy: Updated `OmniRouteProxyServer` to parse `{providerId}/{modelId}` from incoming requests, map the base URL directly from provider ID, and pass the exact model ID transparently to the target provider.
* Verification: Built successfully. Dynamic fetching architecture is ready.
