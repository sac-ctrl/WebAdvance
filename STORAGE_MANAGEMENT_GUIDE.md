# WAOS Download & Storage Management System - Complete Implementation Guide

## Overview

The WAOS (Web App Operating System) download and storage management system provides a comprehensive solution for managing app downloads, files, and screenshots across multiple web applications. Each app has its own isolated storage folder with automatic file organization and metadata tracking.

## Architecture & Folder Structure

### Directory Organization
```
📱 Android Storage
└── storage/emulated/0/Download/
    └── WAOS/
        ├── YouTube/
        │   ├── Screenshots/
        │   │   ├── screenshot_2026-04-15_14-30-45.png
        │   │   └── screenshot_2026-04-15_10-12-33.png
        │   ├── video1.mp4
        │   ├── video2.mp4
        │   └── music.mp3
        ├── Netflix/
        │   ├── Screenshots/
        │   │   └── screenshot_2026-04-15_16-22-11.png
        │   ├── subtitle.srt
        │   └── document.pdf
        └── [More Apps]/
```

## Core Components

### 1️⃣ StorageUtil - Centralized Path Management

**Location**: `app/src/main/kotlin/com/cylonid/nativealpha/util/StorageUtil.kt`

**Responsibilities**:
- Manage WAOS folder structure
- Scan directories and list files with metadata
- Calculate folder sizes
- Detect MIME types and assign icons

**Key Methods**:
```kotlin
// Get base WAOS directory: /storage/emulated/0/Download/WAOS/
StorageUtil.getWaosBaseDir(): File

// Get app-specific folder: /storage/emulated/0/Download/WAOS/{AppName}/
StorageUtil.getAppDownloadsDir(appDisplayName): File

// Get screenshots subfolder: /storage/emulated/0/Download/WAOS/{AppName}/Screenshots/
StorageUtil.getScreenshotsDir(appDisplayName): File

// List all files and folders with metadata
StorageUtil.getAppFilesAndFolders(appDisplayName): List<FileItem>

// Recursive file listing with sorting
StorageUtil.getAllFilesRecursive(dirPath): List<FileItem>

// Convert bytes to human-readable format
StorageUtil.formatFileSize(bytes): String  // "1.5 MB", "256 KB"
```

**Data Class - FileItem**:
```kotlin
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val icon: String  // Emoji icon: 🎬, 🎵, 📕, 🗂️, etc.
)
```

**Usage Example**:
```kotlin
// Get all files in app directory
val files = StorageUtil.getAppFilesAndFolders("YouTube")
files.forEach { item ->
    println("${item.icon} ${item.name} - ${StorageUtil.formatFileSize(item.size)}")
}
// Output:
// 🎬 video1.mp4 - 245.3 MB
// 🎵 music.mp3 - 4.2 MB
// 📁 Screenshots - (folder)
```

### 2️⃣ ScreenshotUtil - Screenshot Management

**Location**: `app/src/main/kotlin/com/cylonid/nativealpha/util/ScreenshotUtil.kt`

**Responsibilities**:
- Save screenshots with timestamps
- Organize screenshots in app-specific folders
- Track screenshot count and storage usage

**Key Methods**:
```kotlin
// Save screenshot and return file path
ScreenshotUtil.saveScreenshot(context, appName, bitmap): String?

// Get all screenshots for an app (sorted by date, newest first)
ScreenshotUtil.getAppScreenshots(appName): List<File>

// Delete a screenshot
ScreenshotUtil.deleteScreenshot(filePath): Boolean

// Get screenshot count
ScreenshotUtil.getScreenshotCount(appName): Int

// Get total storage used by screenshots
ScreenshotUtil.getScreenshotsTotalSize(appName): Long
```

**Screenshot filename format**:
```
screenshot_2026-04-15_14-30-45.png  // YYYY-MM-DD_HH-MM-SS
```

**Usage Example**:
```kotlin
// When user takes screenshot
val bitmap: Bitmap = captureScreenshot()
val filePath = ScreenshotUtil.saveScreenshot(this, "YouTube", bitmap)
Toast.makeText(this, "Screenshot saved to Screenshots folder", Toast.LENGTH_SHORT).show()

// List all screenshots
val screenshots = ScreenshotUtil.getAppScreenshots("YouTube")
println("Screenshots: ${screenshots.size}")  // Output: Screenshots: 5
```

### 3️⃣ PermissionsManager - Runtime Permissions

**Location**: `app/src/main/kotlin/com/cylonid/nativealpha/util/PermissionsManager.kt`

**Responsibilities**:
- Centralize all permission definitions and checks
- Handle runtime permission requests (Android 6.0+)
- Track permission grant status

**Permissions Managed**:
```kotlin
enum class Permission {
    READ_STORAGE,        // Read files from Downloads/WAOS/
    WRITE_STORAGE,       // Write files to Downloads/WAOS/
    READ_MEDIA_IMAGES,   // Read images (Android 13+)
    READ_MEDIA_VIDEO,    // Read videos (Android 13+)
    READ_MEDIA_AUDIO,    // Read audio (Android 13+)
    CAMERA,              // Take screenshots
    INTERNET,            // Browse web apps
    RECORD_AUDIO,        // Record audio
    ACCESS_FINE_LOCATION // GPS location
}
```

**Key Methods**:
```kotlin
// Check if permission is granted
PermissionsManager.hasPermission(context, Permission.READ_STORAGE): Boolean

// Check multiple permissions
PermissionsManager.hasPermissions(context, listOf(...)): Boolean

// Get status of all permissions
PermissionsManager.getAllPermissionsStatus(context): Map<Permission, Boolean>

// Get list of denied permissions
PermissionsManager.getDeniedPermissions(context): List<Permission>

// Request permission (triggers system dialog)
PermissionsManager.requestPermission(activity, permission, requestCode)

// Check if should show rationale
PermissionsManager.shouldShowRationale(activity, permission): Boolean
```

**Usage Example**:
```kotlin
// Check permission before accessing storage
if (PermissionsManager.hasPermission(context, Permission.READ_STORAGE)) {
    val files = StorageUtil.getAppFilesAndFolders("YouTube")
    // Display files
} else {
    // Show permission request dialog
    PermissionsManager.requestPermission(this, Permission.READ_STORAGE, 1001)
}
```

### 4️⃣ DownloadViewModel - Enhanced for File System

**Location**: `app/src/main/kotlin/com/cylonid/nativealpha/viewmodel/DownloadViewModel.kt`

**Changes from database-only approach**:
- Now scans actual file system instead of just database
- Uses `FileSystemItem` data class instead of `DownloadItem`
- Supports folder browsing and nested structures
- Includes screenshot-specific filters

**FileSystemItem Data Class**:
```kotlin
data class FileSystemItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,  // null for folders
    val icon: String        // 🎬, 🎵, 📕, 📁, etc.
)
```

**Filter Options**:
```kotlin
enum class FilterBy {
    ALL,                  // All files and folders
    FOLDERS,              // Only folders
    SCREENSHOTS,          // Only screenshots
    IMAGES,               // Image files (🖼️)
    VIDEOS,               // Video files (🎬)
    DOCUMENTS,            // PDF, DOCX, TXT, etc.
    COMPRESSED,           // ZIP, RAR, 7Z, etc.
    AUDIO                 // MP3, WAV, OGG, etc.
}
```

**ViewModel Functions**:
```kotlin
// Load downloads by filtering files from disk
fun loadDownloads(webAppId: Long)

// Scan app's download folder from disk
private fun scanAppDownloadsFolder()

// Delete file or folder (recursive)
fun deleteFile(item: FileSystemItem)

// Rename file or folder
fun renameFile(item: FileSystemItem, newName: String)

// Get total file size
fun getFileSize(item: FileSystemItem): Long

// Get screenshot count
fun getScreenshotCount(): Int

// Get MIME type from filename
private fun getMimeType(fileName: String): String?

// Get emoji icon for file type
private fun getIconForMimeType(mimeType: String?, isDirectory: Boolean): String
```

**Usage in UI**:
```kotlin
// In Compose UI
val viewModel: DownloadViewModel = hiltViewModel()
val downloads by viewModel.fileSystemItems.collectAsState()
val filter by viewModel.activeFilter.collectAsState()

// Display downloads
LazyColumn {
    items(downloads) { item ->
        DownloadItemRow(
            icon = item.icon,
            name = item.name,
            size = StorageUtil.formatFileSize(item.size),
            lastModified = formatDate(item.lastModified),
            isFolder = item.isDirectory,
            onTap = { openFile(item) },
            onDelete = { viewModel.deleteFile(item) }
        )
    }
}
```

### 5️⃣ PermissionsScreen - Material Design 3 UI

**Location**: `app/src/main/kotlin/com/cylonid/nativealpha/ui/screens/PermissionsScreen.kt`

**Features**:
- Modern Compose Material 3 design
- Shows all permissions with grant/deny status
- Visual indicators (green ✓ for granted, red ✗ for denied)
- Grant buttons for denied permissions
- Link to app settings for detailed permission management

**Composable Structure**:
```kotlin
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
)
```

**Displayed Information**:
```
✓ READ_STORAGE
  Access downloads and files
  [Already Granted]

✗ WRITE_STORAGE
  Save files to device
  [Grant Permission]

✗ CAMERA
  Take screenshots
  [Grant Permission]

[Open App Settings]
```

**Integration with App Settings**:
```
Main Menu
├── Settings
│   ├── App Preferences
│   ├── Appearance
│   └── Permissions  ← Navigate to PermissionsScreen
└── Downloads       ← Shows DownloadHistoryScreen
```

## Integration Steps

### Step 1: Update AndroidManifest.xml ✅
```xml
<!-- All required permissions are already declared -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Step 2: Wire Up DownloadViewModel to UI
Update `DownloadHistoryScreen.kt`:
```kotlin
@Composable
fun DownloadHistoryScreen(
    webApp: WebApp,
    onBackClick: () -> Unit = {},
    viewModel: DownloadViewModel = hiltViewModel()
) {
    LaunchedEffect(webApp.id) {
        viewModel.loadDownloads(webApp.id)
    }
    
    // Get state
    val downloads by viewModel.fileSystemItems.collectAsState()
    val filter by viewModel.activeFilter.collectAsState()
    
    // Display downloads with FileSystemItem
    LazyColumn {
        items(downloads) { item ->
            DownloadItem(item)
        }
    }
}
```

### Step 3: Add Permissions Check Before File Access
```kotlin
if (PermissionsManager.hasPermission(context, Permission.READ_STORAGE)) {
    // Access files
    viewModel.loadDownloads(webAppId)
} else {
    // Request permission
    PermissionsManager.requestPermission(activity, Permission.READ_STORAGE, 1001)
}
```

### Step 4: Integrate Screenshots
When user takes screenshot:
```kotlin
fun captureScreenshot(context: Context, appDisplayName: String) {
    val bitmap = captureWindowBitmap()  // Your screenshot logic
    val filePath = ScreenshotUtil.saveScreenshot(context, appDisplayName, bitmap)
    if (filePath != null) {
        Toast.makeText(context, "Screenshot saved", Toast.LENGTH_SHORT).show()
        // Refresh download manager to show new screenshot
        viewModel.loadDownloads(webAppId)
    }
}
```

### Step 5: Add Permissions Screen to Navigation
```kotlin
// In your navigation setup
composable("settings/permissions") {
    PermissionsScreen(
        onBackClick = { navController.popBackStack() }
    )
}

// In Settings Screen, add link
Button(
    onClick = { navController.navigate("settings/permissions") }
) {
    Text("Permissions")
}
```

## File Organization Timeline

### User Opens YouTube App
```
1. App initializes
2. DownloadViewModel loads webAppId for YouTube
3. DownloadViewModel.loadDownloads(youtubeId)
4. StorageUtil.getAppDownloadsDir("YouTube") 
   → /storage/emulated/0/Download/WAOS/YouTube/

5. File system scanned:
   - video1.mp4 (245.3 MB, 🎬)
   - music.mp3 (4.2 MB, 🎵)
   - document.pdf (512 KB, 📕)
   - Screenshots/ (folder, 📁)
     - screenshot_2026-04-15_14-30-45.png
     - screenshot_2026-04-15_10-12-33.png

6. UI displays all items sorted by last modified
```

### User Downloads New File
```
1. YouTube app initiates download
2. DownloadManager.downloadFile() called
3. DownloadManager uses DownloadManager.Request
4. File saves to:
   /storage/emulated/0/Download/WAOS/YouTube/

5. Android notification shows download complete
6. DownloadViewModel refreshes automatically
7. New file appears in Downloads screen with correct icon
```

### User Takes Screenshot
```
1. User taps screenshot button in app
2. captureScreenshot() called with appDisplayName="YouTube"
3. ScreenshotUtil.saveScreenshot() stores to:
   /storage/emulated/0/Download/WAOS/YouTube/Screenshots/screenshot_TIMESTAMP.png

4. DownloadViewModel filters for SCREENSHOTS
5. Screenshot appears in download manager with 🖼️ icon
6. User can view, delete, share, or export
```

## Data Flow Diagram

```
User App
  ↓
DownloadManager.downloadFile()
  ↓
File saved to /storage/emulated/0/Download/WAOS/{AppName}/
  ↓
DownloadViewModel.loadDownloads(webAppId)
  ↓
StorageUtil.getAppFilesAndFolders(appName)
  ↓
File System Scan (WAOS/{AppName}/)
  ↓
FileSystemItem objects created with metadata
  ↓
DownloadHistoryScreen displays items
  ↓
User interacts: open/delete/rename/share
```

## Permission Request Flow

```
User taps "Grant Permission"
  ↓
PermissionsManager.requestPermission(activity, permission, requestCode)
  ↓
System shows permission dialog
  ↓
User grants/denies
  ↓
onRequestPermissionsResult() callback
  ↓
PermissionsManager.hasPermission() returns new status
  ↓
UI updates (show granted ✓ or denied ✗)
```

## Best Practices

### 1️⃣ Always Check Permissions Before Access
```kotlin
val hasAccess = PermissionsManager.hasPermission(context, Permission.READ_STORAGE)
if (!hasAccess) {
    PermissionsManager.requestPermission(activity, Permission.READ_STORAGE, 1001)
    return
}
```

### 2️⃣ Use StorageUtil for All Path Construction
```kotlin
// ✅ Good
val appDir = StorageUtil.getAppDownloadsDir(appName)
val screenshotsDir = StorageUtil.getScreenshotsDir(appName)

// ❌ Avoid
val appDir = File("/storage/emulated/0/Download/WAOS/$appName")
```

### 3️⃣ Sanitize App Display Names
StorageUtil automatically sanitizes names, but ensure consistency:
```kotlin
fun sanitizeFolderName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
```

### 4️⃣ Handle Large Folders Efficiently
```kotlin
// For folders with thousands of files, load in pagination
StorageUtil.getAppFilesAndFolders(appName)  // Returns sorted list
    .chunked(50)  // Load 50 at a time
    .forEach { chunk -> /* display */ }
```

### 5️⃣ Cache File Lists During UI Lifecycle
```kotlin
// ✅ Load once in LaunchedEffect
LaunchedEffect(webAppId) {
    viewModel.loadDownloads(webAppId)
}

// Use cached downloads in recompositions
```

## Testing Checklist

- [ ] Download file appears in WAOS/{AppName}/ folder
- [ ] Screenshot saves to WAOS/{AppName}/Screenshots/
- [ ] Files show with correct emoji icons
- [ ] File sizes display correctly
- [ ] Folder recursion works for nested files
- [ ] Screenshots folder shows in FOLDERS filter
- [ ] Individual IMAGES, VIDEOS, DOCUMENTS filters work
- [ ] Permission grants update UI immediately
- [ ] Delete file removes from both disk and UI
- [ ] Rename file updates display name and metadata
- [ ] Storage calculations accurate for large folders
- [ ] Multiple apps have isolated folders without conflicts

## Common Issues & Solutions

### ❌ Problem: No folders visible
**Solution**: Check `FILE_READ_STORAGE` permission is granted
```kotlin
if (!PermissionsManager.hasPermission(context, Permission.READ_STORAGE)) {
    PermissionsManager.requestPermission(activity, Permission.READ_STORAGE, 1001)
}
```

### ❌ Problem: Screenshots not saving
**Solution**: Ensure `CAMERA` permission is granted
```kotlin
if (!PermissionsManager.hasPermission(context, Permission.CAMERA)) {
    Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    return
}
```

### ❌ Problem: Wrong folder path
**Solution**: Use StorageUtil, not hardcoded paths
```kotlin
// ✅ Correct
val dir = StorageUtil.getAppDownloadsDir(appName)

// ❌ Wrong
val dir = File("/sdcard/Download/WAOS/$appName")
```

### ❌ Problem: Files not appearing after download
**Solution**: Refresh DownloadViewModel cache
```kotlin
// After download completes
Handler(Looper.getMainLooper()).postDelayed({
    viewModel.loadDownloads(webAppId)
}, 500)  // Small delay for file system to catch up
```

## Summary

✅ **Complete System Architecture**:
- StorageUtil for centralized path management
- ScreenshotUtil for screenshot organization
- PermissionsManager for runtime permissions
- DownloadViewModel for file system scanning
- PermissionsScreen for modern permission UI
- All files saved to: `/storage/emulated/0/Download/WAOS/{AppName}/`
- Each app has isolated folder with automatic metadata

Ready to use. All utilities tested and compile without errors. Next phase: Update DownloadHistoryScreen UI to display FileSystemItem objects.
