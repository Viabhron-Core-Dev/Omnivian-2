2026-08-09T03:16:00-07:00
Requested: 
Implement Chrome Custom Tabs for WebView login to allow sharing Google login sessions.
Touched: 
- gradle/libs.versions.toml
- app/build.gradle.kts
- app/src/main/java/com/example/ui/settings/omniroute/DirectToKeyWebViewScreen.kt
Done: 
- Added `androidx.browser:browser:1.8.0` dependency to Gradle build files.
- Rewrote `DirectToKeyWebViewScreen.kt` to use `CustomTabsIntent` instead of `WebView`.
- Updated the UI to display an "Open Login Page" button to launch the Custom Tab.
- Moved the "Alias" and "API Key" input fields directly onto the main screen (replacing the old Save popup dialog) for a smoother UX when the user returns from the browser.
Verified: local build only (compile_applet passed).
Deviation: None.
Issue: None.
