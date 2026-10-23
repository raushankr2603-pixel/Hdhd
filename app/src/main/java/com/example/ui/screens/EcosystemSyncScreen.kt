package com.example.ui.screens

import android.app.Activity
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AetherViewModel
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary

@Composable
fun EcosystemSyncScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val brightness by viewModel.brightnessPercent.collectAsStateWithLifecycle()
    val volume by viewModel.volumePercent.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val smartDevices by viewModel.smartDevices.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val e2eePayload by viewModel.e2eeSyncPayload.collectAsStateWithLifecycle()

    var appSearchQuery by remember { mutableStateOf("") }
    var newNodeName by remember { mutableStateOf("") }
    var newNodeCategory by remember { mutableStateOf("SMART_HOME") }
    var newNodeEndpoint by remember { mutableStateOf("http://192.168.1.60:8123/api/webhook") }
    var syncPassphrase by remember { mutableStateOf("AetherOS-Quantum-Vault-2026") }

    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) installedApps.take(8)
        else installedApps.filter {
            it.appName.contains(appSearchQuery, ignoreCase = true) ||
                it.packageName.contains(appSearchQuery, ignoreCase = true)
        }.take(12)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Phone System Settings Hardware Controller
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
                        Column {
                            Text(
                                text = "PHONE HARDWARE & SYSTEM SETTINGS CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Direct OS Telemetry & Hardware Actuators",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.openWriteSettingsPermission() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OS Perms", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Screen Brightness: $brightness%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = brightness.toFloat(),
                        onValueChange = { viewModel.updateBrightness(it.toInt(), activity) },
                        valueRange = 5f..100f,
                        modifier = Modifier.testTag("brightness_slider")
                    )

                    Text(
                        text = "System Media Volume: $volume%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { viewModel.updateVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag("volume_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isTorchOn,
                            onClick = { viewModel.toggleTorch() },
                            label = { Text(if (isTorchOn) "Torch ON" else "Torch OFF") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FlashlightOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("torch_toggle_chip")
                        )
                        AssistChip(
                            onClick = { viewModel.setRingerProfile("NORMAL") },
                            label = { Text("Ringer Normal") }
                        )
                        AssistChip(
                            onClick = { viewModel.setRingerProfile("VIBRATE") },
                            label = { Text("Vibrate Mode") }
                        )
                        AssistChip(
                            onClick = {
                                viewModel.deviceControl.openSystemSettingsPanel(Settings.ACTION_WIFI_SETTINGS)
                            },
                            label = { Text("Wi-Fi Panel") }
                        )
                        AssistChip(
                            onClick = {
                                viewModel.deviceControl.openSystemSettingsPanel(Settings.ACTION_BLUETOOTH_SETTINGS)
                            },
                            label = { Text("Bluetooth Panel") }
                        )
                    }
                }
            }
        }

        // 2. Smart Home Ecosystem & Cross-Platform Companion Mesh
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SMART HOME, OTHER PHONES, DESKTOP & BROWSER EXTENSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantumEmerald
                    )
                    Text(
                        text = "Unified Cross-Platform Device & Smart Home Bridge",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "SMART_HOME" to "Smart Home",
                            "PHONE_NODE" to "Phone Device",
                            "DESKTOP_APP" to "Desktop App",
                            "BROWSER_EXT" to "Browser Ext"
                        ).forEach { (cat, label) ->
                            FilterChip(
                                selected = newNodeCategory == cat,
                                onClick = { newNodeCategory = cat },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newNodeName,
                        onValueChange = { newNodeName = it },
                        label = { Text("Device / Node Name (e.g. Office Smart Lock or MacBook Pro)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newNodeEndpoint,
                        onValueChange = { newNodeEndpoint = it },
                        label = { Text("Local IP / Matter / WebSocket Bridge Endpoint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.addSmartOrCompanionDevice(
                                name = newNodeName,
                                category = newNodeCategory,
                                protocol = "E2EE Mesh / REST Bridge",
                                endpointUrl = newNodeEndpoint
                            )
                            newNodeName = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_ecosystem_node_button")
                    ) {
                        Text("Connect Smart Home or Cross-Platform Node")
                    }
                }
            }
        }

        // 3. Connected Smart Home & Companion Nodes List
        items(smartDevices, key = { it.id }) { device ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = when (device.category) {
                                "SMART_HOME" -> Icons.Default.Home
                                "DESKTOP_APP" -> Icons.Default.LaptopMac
                                "BROWSER_EXT" -> Icons.Default.Extension
                                "PHONE_NODE" -> Icons.Default.PhoneAndroid
                                else -> Icons.Default.Devices
                            },
                            contentDescription = null,
                            tint = if (device.powerState) CyberCyan else TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${device.category} • ${device.stateLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QuantumEmerald
                            )
                            Text(
                                text = "${device.protocol} • ${device.endpointUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = device.powerState,
                        onCheckedChange = { viewModel.toggleSmartDevicePower(device) }
                    )
                }
            }
        }

        // 4. End-to-End Encrypted (E2EE) Cross-Device Sync Vault
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "END-TO-END ENCRYPTED (AES-256-GCM) SYNC ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Sync Macros & Providers Across Desktop, Browser Ext & Phones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = syncPassphrase,
                        onValueChange = { syncPassphrase = it },
                        label = { Text("Zero-Knowledge E2EE Passphrase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.generateE2EESyncBundle(syncPassphrase) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_e2ee_bundle_button")
                    ) {
                        Text("Generate & Broadcast AES-256-GCM Sync Bundle")
                    }

                    if (e2eePayload.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Encrypted Ciphertext Bundle (Verified for Desktop & Browser Extension):",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                        Text(
                            text = e2eePayload.take(180) + "...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 5. Third-Party Installed Application Manager
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "THIRD-PARTY APPLICATION CONTROLLER (${installedApps.size} APPS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        label = { Text("Search installed apps to launch or automate...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    filteredApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.launchInstalledApp(app) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
