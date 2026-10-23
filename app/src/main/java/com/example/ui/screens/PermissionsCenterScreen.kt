package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.PermissionKind
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionsCenterScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val capabilities by viewModel.capabilitiesList.collectAsStateWithLifecycle()
    val isDaemonRunning by viewModel.isBackgroundDaemonRunning.collectAsStateWithLifecycle()
    val capturedNotifications by viewModel.capturedNotifications.collectAsStateWithLifecycle()
    val screenVision by viewModel.screenVision.collectAsStateWithLifecycle()

    var textInjectionSample by remember { mutableStateOf("Hello from AetherOS Autonomous Agent") }

    // Automatically re-check permission statuses whenever user returns from Android System Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val grantedCount = results.values.count { it }
        viewModel.refreshPermissionsStatus()
        viewModel.postToast("Runtime Permissions updated ($grantedCount granted)")
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissionsStatus()
    }

    val grantedTotal = capabilities.count { it.isGranted }
    val totalCount = capabilities.size.coerceAtLeast(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Master Permissions & OS Capabilities Summary Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CyberCyan, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FULL SYSTEM PERMISSIONS & OS ACCESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "$grantedTotal / $totalCount Capabilities Active",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.refreshPermissionsStatus() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("refresh_permissions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh status",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { (grantedTotal.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) },
                        color = QuantumEmerald,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val allRuntime = viewModel.permissionCenter.getAllRuntimePermissionStrings()
                                multiPermissionLauncher.launch(allRuntime.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumEmerald,
                                contentColor = Color(0xFF002114)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("grant_all_runtime_permissions_button")
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant All Runtime Perms")
                        }

                        OutlinedButton(
                            onClick = { viewModel.openAppSystemSettings() },
                            modifier = Modifier.testTag("open_app_info_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsApplications,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("App Info")
                        }
                    }
                }
            }
        }

        // 2. Continuous Background Live Foreground Daemon Toggle
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralViolet.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "24/7 BACKGROUND LIVE VOICE & VISION DAEMON",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuralViolet
                        )
                        Text(
                            text = "AetherLiveForegroundService (Gemini Live / GPT Voice Style)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Keeps the voice partner & screen automation daemon alive while you use other apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isDaemonRunning,
                        onCheckedChange = { viewModel.toggleBackgroundLiveDaemon(it) },
                        modifier = Modifier.testTag("background_daemon_switch")
                    )
                }
            }
        }

        // 3. Accessibility Service & Screen Gesture / Text Injection Control Suite
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ACCESSIBILITY & AUTONOMOUS UI CONTROL SUITE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Text(
                                    text = if (screenVision.isServiceConnected) "Connected to OS Window Manager"
                                    else "In-App Bridge Active (Tap to Enable Global OS Service)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.launchSpecialPermission("accessibility_service") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_accessibility_settings_btn")
                        ) {
                            Text("Accessibility Settings", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Last Gesture / Action: ${screenVision.lastGestureLog}",
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantumEmerald
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Extended Global Actions & Gesture Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.performScreenTap(540f, 1150f) },
                            label = { Text("Tap (540, 1150)") }
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenScroll("DOWN") },
                            label = { Text("Scroll Down") }
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenScroll("UP") },
                            label = { Text("Scroll Up") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("BACK") },
                            label = { Text("Global Back") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("HOME") },
                            label = { Text("Global Home") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("RECENTS") },
                            label = { Text("Recents") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("NOTIFICATIONS") },
                            label = { Text("Notifications") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("QUICK_SETTINGS") },
                            label = { Text("Quick Settings") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("SCREENSHOT") },
                            label = { Text("Take Screenshot") }
                        )
                        AssistChip(
                            onClick = { viewModel.performGlobalNav("LOCK_SCREEN") },
                            label = { Text("Lock Screen") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInjectionSample,
                            onValueChange = { textInjectionSample = it },
                            label = { Text("Accessibility Auto-Type Text into Focused Field") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.performTextInputInjection(textInjectionSample) }
                        ) {
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inject")
                        }
                    }
                }
            }
        }

        // 4. All 12 Runtime & Special OS Permissions Checklist
        item {
            Text(
                text = "ALL SYSTEM PERMISSIONS & SPECIAL CAPABILITIES (${capabilities.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        items(capabilities, key = { it.id }) { cap ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (cap.isGranted) QuantumEmerald.copy(alpha = 0.5f) else BorderSubtle,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (cap.isGranted) QuantumEmerald else AmberAlert,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cap.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cap.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cap.permissionName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (cap.kind == PermissionKind.RUNTIME_DANGEROUS) {
                                singlePermissionLauncher.launch(cap.permissionName)
                            } else {
                                viewModel.launchSpecialPermission(cap.id)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cap.isGranted) ObsidianSurfaceVariant else CyberCyan,
                            contentColor = if (cap.isGranted) QuantumEmerald else Color(0xFF001F24)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("perm_action_${cap.id}")
                    ) {
                        Text(cap.actionButtonText, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 5. Captured Notifications Stream (from AetherNotificationListenerService)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = QuantumEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "LIVE NOTIFICATION INTERCEPTOR STREAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantumEmerald
                                )
                                Text(
                                    text = "Read & Automate Cross-App Notifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.launchSpecialPermission("notification_listener") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Listener Access", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    capturedNotifications.take(5).forEach { notif ->
                        Surface(
                            color = ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${notif.title} (${notif.packageName})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = notif.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
