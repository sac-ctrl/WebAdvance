package com.cylonid.nativealpha.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.util.PermissionsManager
import com.cylonid.nativealpha.util.PermissionsManager.Permission

@Composable
fun PermissionsScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var permissionsStatus by remember { mutableStateOf(PermissionsManager.getAllPermissionsStatus(context)) }
    // Track which permissions the user has already been asked for during this
    // session. Combined with an unchanged "denied" status after a request and
    // a `shouldShowRationale == false` reading, this lets us detect a
    // permanently-blocked permission ("Don't ask again") and switch its action
    // button to "Open in Settings". A plain set is fine here — every call
    // site that mutates it also calls `refresh()`, which updates
    // `permissionsStatus` and drives recomposition.
    val attempted = remember { mutableSetOf<String>() }

    val refresh: () -> Unit = {
        permissionsStatus = PermissionsManager.getAllPermissionsStatus(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> refresh() }

    LaunchedEffect(Unit) { refresh() }

    val grantedCount = permissionsStatus.values.count { it }
    val totalCount = permissionsStatus.size
    val progress = if (totalCount > 0) grantedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "perm-progress")

    val grouped = remember(permissionsStatus) {
        Permission.values()
            .groupBy { it.category }
            .toList()
            .sortedBy { it.first.ordinal }
    }

    fun statusFor(p: Permission): PermissionsManager.Status {
        val granted = permissionsStatus[p] ?: false
        return when {
            granted -> PermissionsManager.Status.GRANTED
            attempted.contains(p.id) && activity?.let { !PermissionsManager.shouldShowRationale(it, p) } == true ->
                PermissionsManager.Status.BLOCKED
            else -> PermissionsManager.Status.DENIED
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun launchOverlaySettings() {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    fun launchAllFilesSettings() {
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    fun requestRuntime(p: Permission) {
        attempted.add(p.id)
        try {
            permissionLauncher.launch(arrayOf(p.androidPermission))
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    fun requestAllMissing() {
        val missing = PermissionsManager.getMissingRuntimePermissions(context)
        if (missing.isEmpty()) return
        missing.forEach { attempted.add(it.id) }
        try {
            permissionLauncher.launch(missing.map { it.androidPermission }.toTypedArray())
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // ─── Header ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BgDark, BgDeep)))
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(listOf(GradVioletStart.copy(0.5f), Color.Transparent)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, null, tint = GradVioletEnd, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permissions", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "$grantedCount of $totalCount granted",
                            color = TextMuted, fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = refresh, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GradVioletEnd,
                    trackColor = CardBorder
                )
            }
        }

        // ─── Action row ───────────────────────────────────────────────────
        val missingRuntime = remember(permissionsStatus) {
            PermissionsManager.getMissingRuntimePermissions(context).size
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { requestAllMissing() },
                modifier = Modifier.weight(1f).height(46.dp),
                enabled = missingRuntime > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GradVioletEnd,
                    disabledContainerColor = CardSurface,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (missingRuntime > 0) "Grant All ($missingRuntime)" else "All Granted",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = { openAppSettings() },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("App Settings", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ─── Grouped permission list ──────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            grouped.forEach { (category, perms) ->
                item(key = "header_${category.name}") {
                    CategoryHeader(category, perms.count { permissionsStatus[it] == true }, perms.size)
                }
                items(items = perms, key = { it.id }) { permission ->
                    PermissionCard(
                        permission = permission,
                        status = statusFor(permission),
                        onAction = {
                            when {
                                permission == Permission.SYSTEM_ALERT_WINDOW -> launchOverlaySettings()
                                permission == Permission.MANAGE_STORAGE -> launchAllFilesSettings()
                                permission.specialAccess -> openAppSettings()
                                statusFor(permission) == PermissionsManager.Status.BLOCKED -> openAppSettings()
                                else -> requestRuntime(permission)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(refresh, 600)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: PermissionsManager.Category, granted: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(GradVioletEnd, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            category.displayName.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
            "$granted / $total",
            color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionCard(
    permission: Permission,
    status: PermissionsManager.Status,
    onAction: () -> Unit
) {
    val granted = status == PermissionsManager.Status.GRANTED
    val blocked = status == PermissionsManager.Status.BLOCKED
    val accent = when {
        granted -> StatusActive
        blocked -> StatusBg
        else -> ErrorRed
    }
    val tintBg = accent.copy(alpha = 0.13f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(tintBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconFor(permission.iconKey),
                        null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        permission.displayName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        permission.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
                StatusPill(status)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                permission.explanation,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            if (!granted) {
                Spacer(Modifier.height(12.dp))
                val (label, icon) = when {
                    permission.specialAccess -> "Open Settings" to Icons.Default.OpenInNew
                    blocked -> "Open in Settings" to Icons.Default.OpenInNew
                    else -> "Grant Permission" to Icons.Default.Lock
                }
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (blocked) StatusBg else GradVioletEnd,
                        contentColor = if (blocked) BgDeep else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(icon, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: PermissionsManager.Status) {
    val (label, color, icon) = when (status) {
        PermissionsManager.Status.GRANTED -> Triple("Granted", StatusActive, Icons.Default.CheckCircle)
        PermissionsManager.Status.BLOCKED -> Triple("Blocked", StatusBg, Icons.Default.Block)
        PermissionsManager.Status.DENIED -> Triple("Off", ErrorRed, Icons.Default.Cancel)
    }
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun iconFor(key: String): ImageVector = when (key) {
    "image" -> Icons.Default.Image
    "video" -> Icons.Default.VideoLibrary
    "audiofile" -> Icons.Outlined.Audiotrack
    "download" -> Icons.Default.Download
    "folder" -> Icons.Default.Folder
    "camera" -> Icons.Default.PhotoCamera
    "mic" -> Icons.Default.Mic
    "volume" -> Icons.Default.VolumeUp
    "location" -> Icons.Default.LocationOn
    "place" -> Icons.Default.Place
    "notifications" -> Icons.Default.Notifications
    "overlay" -> Icons.Default.Layers
    "wifi" -> Icons.Default.Wifi
    else -> Icons.Default.Lock
}

