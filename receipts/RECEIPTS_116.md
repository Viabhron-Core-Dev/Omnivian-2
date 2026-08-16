* 2026-08-12T12:38:00-07:00
* Request: Fix the "Refresh from Providers" button which was silently failing and not loading any models into the Model Explorer.
* Touched: app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt
* Action:
  * Migrated the `refreshModels()` coroutine to `Dispatchers.IO` to prevent the `NetworkOnMainThreadException` crash caused by OkHttp's synchronous `.execute()`.
  * Fixed the race condition on `activeKeys.first()` by querying `apiKeyDao.getAllKeys().first()` directly from the database instead of relying on the uninitialized UI StateFlow.
  * Encapsulated the fallback model injection logic into a `suspend fun applyFallbacks()` and invoked it in both the `!response.isSuccessful` path and the `catch` block to guarantee fallback models are always populated even if the API is offline, failing, or using a non-standard endpoint (like Anthropic).
* Verification: Built successfully.
