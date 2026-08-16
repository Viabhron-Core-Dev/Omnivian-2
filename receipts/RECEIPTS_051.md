2026-08-07T04:08:00-07:00
* Requested: Fix the bottom navigation bar going under the Android system navigation bar.
* Files touched: `app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt`
* Action: Added `.navigationBarsPadding()` to the `Row` modifier in `FixedBottomNav` to respect system window insets.
* Verification: Verified via local build (`gradle compileDebugKotlin`).
* Deviation: None.
* Known issue/Follow-up: None.
