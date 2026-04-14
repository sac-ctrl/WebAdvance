package com.cylonid.nativealpha.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cylonid.nativealpha.ui.theme.*

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_ID = "crash_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WAOSTheme {
                CrashScreen(
                    onRestart = {
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun CrashScreen(onRestart: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val crashes = remember { CrashLogStorage.getCrashLogs(context) }
    val latest = crashes.firstOrNull()
    var showFullTrace by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0A0A), Color(0xFF060912))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF4444).copy(0.3f), Color.Transparent)
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, Color(0xFFFF4444).copy(0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.BugReport,
                    contentDescription = "Crash",
                    tint = Color(0xFFFF4444),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "App Crashed",
                color = Color(0xFFFF4444),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "WAOS encountered an unexpected error",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            if (latest != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Crash Details", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = CardBorder)

                    CrashDetailRow("Time", latest.timestamp)
                    CrashDetailRow("Error", latest.message)
                    CrashDetailRow("Thread", latest.threadName)
                    CrashDetailRow("Version", latest.appVersion)

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Stack Trace",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { showFullTrace = !showFullTrace },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (showFullTrace) "Hide" else "Show",
                                color = VioletSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showFullTrace,
                        enter = expandVertically() + fadeIn()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0A0F1A))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                latest.stackTrace,
                                color = Color(0xFFFF7777),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    val fullCrashReport = buildString {
                        appendLine("=== WAOS Crash Report ===")
                        appendLine("Time: ${latest.timestamp}")
                        appendLine("Version: ${latest.appVersion}")
                        appendLine("Thread: ${latest.threadName}")
                        appendLine("Error: ${latest.message}")
                        appendLine()
                        appendLine("Stack Trace:")
                        append(latest.stackTrace)
                    }

                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("WAOS Crash Report", fullCrashReport))
                            Toast.makeText(context, "Crash report copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBorder,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Full Crash Report", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (crashes.size > 1) {
                    Text(
                        "${crashes.size} total crashes saved — view all in Settings",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No crash details available", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDeep),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restart WAOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Text("Close", fontSize = 15.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CrashDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(64.dp)
        )
        Text(
            value,
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
    }
}
