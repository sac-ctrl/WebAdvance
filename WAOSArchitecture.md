# WAOS Architecture

## Overview
This repository is being extended from the existing Native Alpha WebView browser into a "Web App Operating System" (WAOS) architecture.
The new system is designed to support:
- Multi-webapp dashboard
- Independent per-app sessions
- Per-app downloads, clipboard, credentials, and floating windows
- Background refresh, notifications, and file viewer integration
- A modular UI and service layer for future feature expansion

## Core Modules

### waos.model
- `WaosApp.kt` — app metadata, session state, download folder, and per-app settings
- `WaosSession.kt` — session persistence and isolation data

### waos.ui
- `WaosDashboardActivity.kt` — advanced dashboard and app card browser
- `UniversalFileViewerActivity.kt` — file preview and viewer container
- `CredentialVaultActivity.kt` — encrypted credentials interface
- `ClipboardManagerActivity.kt` — per-app clipboard history UI
- `DownloadHistoryActivity.kt` — per-app download manager UI
- `FloatingWindowActivity.kt` — floating window host activity

### waos.service
- `FloatingWindowService.kt` — overlay service for floating windows and bubble launcher
- `BackgroundRefreshWorker.kt` — periodic refresh using WorkManager
- `WebViewSessionManager.kt` — session isolation manager for WebView containers

### waos.util
- `WaosConstants.kt` — intent extras and constant values
- `WaosLogger.kt` — logging helper for WAOS modules

## Resources
- `res/layout/activity_waos_dashboard.xml` — dashboard layout with app cards and action bar
- `res/layout/activity_waos_file_viewer.xml` — universal viewer shell
- `res/layout/activity_clipboard_manager.xml` — clipboard manager shell
- `res/layout/activity_credential_vault.xml` — credential vault shell
- `res/layout/activity_download_history.xml` — download history shell
- `res/layout/activity_floating_window.xml` — floating window host layout
- `res/layout/card_waos_app.xml` — advanced app card UI

## Integration Plan
1. Keep the existing `MainActivity` and `WebViewActivity` intact.
2. Add WAOS-specific activities and services for advanced feature support.
3. Use the existing `DataManager` model as a compatibility layer while introducing per-app WAOS state.
4. Add a floating overlay service to host the floating bubble and window manager.
5. Add a clean architecture boundary between feature UI and session management.

## Next Steps
- Implement per-app session persistence in `WebViewSessionManager`
- Add download interception and file viewer support in `DownloadHistoryActivity`
- Add credential storage and encryption in `CredentialVaultActivity`
- Add clipboard history and paste actions in `ClipboardManagerActivity`
- Add floating window creation and z-index management in `FloatingWindowService`
- Connect the UI to the existing WebView activity for app launch and session switching
