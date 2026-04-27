# WAOS - Web App Operating System (Android)

## Project Overview
Android application (Kotlin/Jetpack Compose) that turns websites into native-looking apps. Evolved from "Native Alpha" into a full Web App Operating System dashboard.

## Architecture

### Technology Stack
- **Language**: Kotlin + Java (hybrid)
- **UI**: Jetpack Compose (Material 3) with dark WAOS theme
- **Navigation**: Compose Navigation (`androidx.navigation:navigation-compose:2.8.9`)
- **DI**: Hilt (Dagger)
- **Database**: Room (SQLite) — `waos_database`, version 5
- **Background**: WorkManager
- **Build**: Gradle + Kotlin KAPT

### Key Architecture Decisions
- **Single Activity**: `MainActivity` hosts `NavHost` with Compose navigation
- **Per-app process isolation**: Every web app launches into a dedicated Android process (`:webapp_0`..`:webapp_7`) via `WebAppRouter.kt`. `App.java` reads the per-slot appId from SharedPreferences and applies a unique `WebView.setDataDirectorySuffix("waos_app_<appId>")` so cookies, localStorage, IndexedDB, cache and service workers are NEVER shared between apps. Each app keeps its own persistent login.
- **Slot recycling**: When an appId mapped to a slot differs from the slot's current appId, `WebAppRouter` kills the slot's process so the next launch starts fresh with the correct per-app data directory.
- **Legacy multi-sandbox**: Older `:web_sandbox_0..7` processes are still supported for backward compatibility (per-slot, not per-app).
- **Session export/import**: `SessionManager.kt` exports/imports AES-256-GCM encrypted session snapshots.
- **MVVM**: ViewModels + Room Flows + Hilt injection

## Project Structure

```
app/src/main/
├── kotlin/com/cylonid/nativealpha/
│   ├── MainActivity.kt              # Nav host (dashboard → add → settings)
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── MainDashboardScreen.kt   # Advanced glassmorphism UI
│   │   │   ├── AddWebAppScreen.kt       # Add/edit web app form
│   │   │   ├── SettingsScreen.kt        # Settings screen
│   │   │   └── WebViewScreen.kt         # WebView browser screen
│   │   ├── theme/
│   │   │   ├── Color.kt             # WAOS dark cyan/violet theme
│   │   │   ├── Theme.kt             # WAOSTheme (dark-first)
│   │   │   └── Type.kt              # Typography
│   │   └── WebViewActivity.kt       # WebView as separate Activity (Intent-launched)
│   ├── viewmodel/
│   │   ├── MainViewModel.kt         # Dashboard state + sample data seeding
│   │   ├── AddWebAppViewModel.kt    # Add/edit form state + saveWebApp()
│   │   └── ...other VMs
│   ├── data/
│   │   ├── AppDatabase.kt           # Room DB v5
│   │   ├── WebAppDao.kt             # CRUD for web apps
│   │   └── Converters.kt           # Type converters (Date, Bitmap, Status, etc.)
│   ├── model/
│   │   └── WebApp.kt                # Main entity with 50+ properties
│   ├── repository/
│   │   └── WebAppRepository.kt      # Data access layer
│   └── service/
│       └── FloatingWindowService.kt # Floating overlay windows
└── java/com/cylonid/nativealpha/    # Legacy Java/Kotlin code
    ├── WebViewActivity.java          # Template for sandboxed processes
    └── waos/                         # Legacy WAOS XML-based UI (being phased out)
```

## Navigation Routes
- `"dashboard"` → `MainDashboardScreen`
- `"add_webapp"` → `AddWebAppScreen` (new)
- `"edit_webapp/{webAppId}"` → `AddWebAppScreen` (edit mode)
- `"settings"` → `SettingsScreen`
- WebView launched via `Intent(context, WebViewActivity::class.java)` with `"webAppId"` extra

## Theme
Dark WAOS theme with:
- Background: `BgDeep = Color(0xFF060912)`
- Primary accent: `CyanPrimary = Color(0xFF00E5FF)`
- Secondary: `VioletSecondary = Color(0xFF9C7DFF)`
- Cards: `CardSurface = Color(0xFF141C2E)`

## Key Features
- **Advanced Dashboard**: Glassmorphism cards, animated gradient background, category filter chips, grid/list toggle
- **Sample Data**: Auto-seeds 8 sample apps on first launch (Google, YouTube, Gmail, WhatsApp, Reddit, GitHub, Twitter, LinkedIn)
- **Full Navigation**: All buttons wired up (FAB adds app, grid/list toggle, settings, context menu with ALL features)
- **Sort & Group**: Modal sort/group sheet with real-time app reordering
- **Status Indicators**: Pulsing animated status dots (Active=green, Background=yellow, Error=red)
- **WebView Per-App**: Each app opens in isolated Kotlin WebViewActivity (registered in manifest)
- **Floating Windows**: Apps can be floated as chat-head overlays (permission check + FloatingWindowService)
- **Download Manager**: Per-app download history (DownloadHistoryActivity with EXTRA_DOWNLOAD_APP_ID)
- **Credential Vault**: Encrypted credential storage with biometric unlock (CredentialVaultActivity)
- **Clipboard Manager**: Per-app clipboard history (ClipboardManagerActivity with EXTRA_CLIPBOARD_APP_ID)
- **Universal Crash Logger**: GlobalCrashHandler intercepts ALL crashes → saves to SharedPreferences → shows CrashActivity with full trace + copy button; crash history viewable in Settings
- **Context Menu**: Long-press any app card → Quick Actions (Open, Float, Edit) + Features row (Downloads, Clipboard, Credentials) + Delete

## Build Notes
- `build.gradle` uses custom Gradle tasks to generate 8 legacy WebView activity copies at build time (the new per-app isolation uses `WebViewActivity0..7` instead)
- Database version 5 — any schema changes need a migration
- Min SDK: 28 (Android 9), Target: 35
- **Signed release**: hardcoded keystore at `app/release.jks` (alias `my-key`, password `Sh@090609`); `app/build.gradle` references it directly so `./gradlew :app:assembleStandardRelease` produces a signed APK with no extra setup. A `keystore.properties` file at the repo root can override these values locally.

## CI / GitHub Actions
- `.github/workflows/debug-apk.yml` — builds `assembleStandardDebug` on every push to main/master/dev/develop and on tags; uploads the APK as an artifact AND publishes a GitHub Release tagged `v<run>-debug` (marked as pre-release) with detailed release notes.
- `.github/workflows/release-apk.yml` — builds signed `assembleStandardRelease` on every push to main/master/dev/develop and on tags; publishes a GitHub Release tagged `v<run>-release` with the signed APK and detailed release notes.
- `.github/workflows/BuildDebug-Apk.yml` — deprecated, manual-trigger only.

## Recent Changes (NexWeb OS rename + WebView upgrades)
- **Rename**: App is now "NexWeb OS" (`strings.xml: app_name`, `app_name_plus`).
- **Launcher icon**: New PNG generated for all `mipmap-*hdpi/native_alpha.png` densities + `drawable-nodpi/nexweb_logo.png`. Adaptive icon (`mipmap-anydpi-v26/native_alpha.xml`) wraps `drawable/nexweb_logo_foreground.xml` with a 22% inset bitmap so the launcher mask never crops the artwork.
- **Persistent WebView history**: Entire back/forward stack is Parcel-marshalled to `filesDir/waos_history/app_<id>.bundle` (helpers in `WebViewActivity` companion: `saveWebViewHistory` / `restoreWebViewHistory` / `clearWebViewHistory`). Restored before first `loadUrl` (so Back works after process kill) and persisted on every `onPageFinished` and `DisposableEffect.onDispose`.
- **Page History UI**: 3-dot → "Page History" opens `WaosPageHistoryDialog` listing the WebView's `copyBackForwardList` entries with Back/Forward jump buttons and Clear History.
- **Export Session**: After encryption to `filesDir/waos_sessions/<id>/...`, the `.waos` file is also copied to `Downloads/WAOS/<AppName>/Sessions/` and registered in the in-app downloader (`DownloadManager.registerLocalFile`) so it appears like a Chrome download.
- **Import Session**: Replaced the "import latest" hack with a proper SAF `OpenDocument` picker; the chosen file is copied to cache then handed to `viewModel.requestSessionImport`.
- **WhatsApp blob downloads**: Fixed JS interface call from `window.waosDownloadBlob('$filename', base64)` to `window.waosDownloadBlob.downloadBlob('$filename', base64)` so blob:// downloads no longer buffer indefinitely.
- **Favicon on dashboard cards**: `MainDashboardScreen.WebAppIcon` renders the website favicon via Coil `SubcomposeAsyncImage`. Falls back to Google's s2 favicon service (`https://www.google.com/s2/favicons?sz=128&domain=<host>`) and to the gradient/emoji on error. The first successful page load auto-saves the favicon URL to `WebApp.iconUrl` via `viewModel.updateFaviconIfNeeded()`.
- **Add to Home Screen**: long-press → "Add to Home Screen" composes a 192×192 launcher icon (favicon centered + small NexWeb badge in the top-right corner with a white ring) via `HomeShortcutCreator`, then calls `ShortcutManagerCompat.requestPinShortcut`. Tapping the shortcut launches `WebAppLauncherActivity` (a tiny `Theme.NoDisplay` exported trampoline) which routes through `WebAppRouter` so the app opens in its dedicated `:webapp_N` sandboxed process.
- **Chrome-parity downloads**: `DownloadManager.downloadFile` now accepts `userAgent`, `contentDisposition`, `explicitMimeType` and `referer`, and automatically forwards the WebView's cookie jar (`CookieManager.getInstance().getCookie(url)`) so authenticated downloads (Drive, intranet, paywalled CDNs) succeed exactly like Chrome. Filenames resolve via `URLUtil.guessFileName(url, contentDisposition, mimeType)` for proper Content-Disposition support, with collision-safe `name (1).ext` suffixing. `data:` URLs are decoded inline (system DownloadManager can't fetch them). Blob URLs are pulled via in-page XHR → base64 → `waosDownloadBlob.downloadBlobWithMime`. Image long-press uses an authenticated `XMLHttpRequest({ withCredentials: true })` first and falls back to the cookie-aware system path via the `__waosImageFallback` JS bridge if XHR is blocked. A `BroadcastReceiver` registered for `ACTION_DOWNLOAD_COMPLETE` updates the in-app download row to COMPLETED/FAILED and runs `MediaScannerConnection.scanFile` so files appear in the gallery/file manager. Removed the duplicate `enqueue` that previously fired from `WebViewClientWithDownload.onDownloadStart`, so each click now produces exactly one download in the unified `Downloads/WAOS/<App>/<Type>/` location.

## Bug Fixes Applied
1. **FloatingWindowService crash (Android 14+)**: Added `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` permissions; set `android:foregroundServiceType="specialUse"` on both FloatingWindowService declarations; updated `startForeground()` to pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+.
2. **White screen**: WebView `factory` now stores ref via `webViewRef`; `LaunchedEffect(webApp?.url)` loads URL only when non-null to fix blank-URL race condition.
3. **Settings not persisted**: `SettingsViewModel` fully rewritten with `SharedPreferences` backing (file: `waos_settings`); all toggles load on init and write immediately on change.
4. **Settings toggles broken**: `SettingsScreen` wired every toggle to its ViewModel setter; backup buttons show loading state and result Toast.
5. **BackupService AES crash**: Removed broken AES encryption (10-byte key was invalid); now saves/restores plain JSON to `externalFilesDir/backups/`.
6. **Missing service declaration**: Added `com.cylonid.nativealpha.service.FloatingWindowService` to AndroidManifest (used by WebViewActivity but was undeclared).
7. **Desktop user agent**: `AddWebAppViewModel.saveWebApp()` now correctly saves desktop UA string when `useDesktopUserAgent` is true; `loadForEdit()` detects and restores it.
8. **shouldRefresh loop**: Added `clearRefreshFlag()` to `WebViewViewModel`; update block now calls it after `reload()` to prevent infinite refresh loop.

## Dependencies Added
- `androidx.navigation:navigation-compose:2.8.9` — Compose navigation
- `io.coil-kt:coil-compose:2.7.0` — Image loading (for future favicon support)
