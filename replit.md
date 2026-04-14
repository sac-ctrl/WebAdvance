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
- **Multi-process WebView**: Build-time code generation creates 8 sandboxed WebView processes (`:web_sandbox_0` to `:web_sandbox_7`)
- **Session Isolation**: Each web app gets isolated cookies/storage via `SessionManager.kt`
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
- **Full Navigation**: All buttons wired up (FAB adds app, grid/list toggle, settings, context menu with open/edit/delete)
- **Sort & Group**: Modal sort/group sheet with real-time app reordering
- **Status Indicators**: Pulsing animated status dots (Active=green, Background=yellow, Error=red)
- **WebView Per-App**: Each app opens in isolated WebView with separate session
- **Floating Windows**: Apps can be floated as chat-head style overlays
- **Download Manager**: Per-app download history
- **Credential Vault**: Encrypted credential storage with biometric unlock
- **Clipboard Manager**: Per-app clipboard history

## Build Notes
- `build.gradle` uses custom Gradle tasks to generate 8 WebView activity copies at build time
- Database version 5 — any schema changes need a migration
- Min SDK: 28 (Android 9), Target: 35

## Dependencies Added
- `androidx.navigation:navigation-compose:2.8.9` — Compose navigation
- `io.coil-kt:coil-compose:2.7.0` — Image loading (for future favicon support)
