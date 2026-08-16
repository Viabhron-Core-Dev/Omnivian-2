2026-08-07T02:58:00-07:00
* Requested: Remove secrets from thread settings.
* Files touched: `app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt`
* Action:
  - Removed `SECRETS` from `ThreadSettingTab` enum.
  - Removed `ThreadSettingTab.SECRETS -> SecretsSettingsContent()` from the `when` expression.
  - Removed the `SecretsSettingsContent()` Composable function entirely.
* Verification: Verified via local build (`gradle compileDebugKotlin`).
* Deviation: None.
* Known issue/Follow-up: None.
