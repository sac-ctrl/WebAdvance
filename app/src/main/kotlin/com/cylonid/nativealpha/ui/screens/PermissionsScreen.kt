package com.cylonid.nativealpha.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.util.PermissionsManager

@Composable
fun PermissionsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var permissionsStatus by remember {
        mutableStateOf(PermissionsManager.getAllPermissionsStatus(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Refresh permission status after request completes
        permissionsStatus = PermissionsManager.getAllPermissionsStatus(context)
    }

    // Refresh permissions when screen is composed
    LaunchedEffect(Unit) {
        permissionsStatus = PermissionsManager.getAllPermissionsStatus(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BgDark, BgDeep)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { 
                    permissionsStatus = PermissionsManager.getAllPermissionsStatus(context)
                }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Brush.radialGradient(listOf(GradVioletStart.copy(0.4f), Color.Transparent)),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, null, tint = GradVioletEnd, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Permissions", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Manage app permissions", color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        // Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = GradVioletEnd,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Grant permissions to enable features like downloads, screenshots, and file access.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Permissions List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(PermissionsManager.Permission.values().toList()) { permission ->
                val isGranted = permissionsStatus[permission] ?: false

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isGranted) 
                                Modifier.border(1.dp, Color(0xFF4CAF50).copy(0.3f), RoundedCornerShape(12.dp))
                            else 
                                Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGranted) Color(0xFF4CAF50).copy(0.05f) else CardSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isGranted) Color(0xFF4CAF50).copy(0.15f) else Color(0xFFf44336).copy(0.15f),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFf44336),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            permission.id.replace("_", " ").uppercase(),
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            if (isGranted) "✓ Granted" else "Not Granted",
                                            color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFf44336),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    permission.description,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            
                            if (!isGranted) {
                                Button(
                                    onClick = {
                                        activity?.let { act ->
                                            when (permission) {
                                                PermissionsManager.Permission.SYSTEM_ALERT_WINDOW -> {
                                                    try {
                                                        val intent = Intent(
                                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                            Uri.fromParts("package", context.packageName, null)
                                                        )
                                                        context.startActivity(intent)
                                                        android.widget.Toast.makeText(context, "Enable overlay permission in settings", android.widget.Toast.LENGTH_SHORT).show()
                                                        // Refresh after short delay
                                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                            permissionsStatus = PermissionsManager.getAllPermissionsStatus(context)
                                                        }, 500)
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Could not open settings", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                PermissionsManager.Permission.MANAGE_STORAGE -> {
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
                                                        android.widget.Toast.makeText(context, "Enable storage permissions in settings", android.widget.Toast.LENGTH_SHORT).show()
                                                        // Refresh after short delay
                                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                            permissionsStatus = PermissionsManager.getAllPermissionsStatus(context)
                                                        }, 500)
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Could not open settings", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                else -> {
                                                    try {
                                                        permissionLauncher.launch(arrayOf(permission.androidPermission))
                                                        android.widget.Toast.makeText(context, "Requesting ${permission.id}...", android.widget.Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Could not request permission", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GradVioletEnd
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        when (permission) {
                                            PermissionsManager.Permission.SYSTEM_ALERT_WINDOW,
                                            PermissionsManager.Permission.MANAGE_STORAGE -> "Open Settings"
                                            else -> "Request"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Button
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GradVioletEnd),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open App Settings", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
    }
}
