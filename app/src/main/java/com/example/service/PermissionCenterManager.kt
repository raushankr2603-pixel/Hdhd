package com.example.service

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class PermissionKind {
    RUNTIME_DANGEROUS,
    SPECIAL_OS_INTENT,
    SYSTEM_SERVICE_TOGGLE
}

data class CapabilityPermissionStatus(
    val id: String,
    val title: String,
    val subtitle: String,
    val permissionName: String,
    val kind: PermissionKind,
    val isGranted: Boolean,
    val actionButtonText: String
)

class PermissionCenterManager(private val context: Context) {

    fun getAllRuntimePermissionStrings(): List<String> {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        return list
    }

    fun isRuntimePermissionGranted(perm: String): Boolean {
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        if (AetherAccessibilityService.instance != null) return true
        return try {
            val expectedComponent = ComponentName(context, AetherAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            enabledServices.contains(expectedComponent.flattenToString()) ||
                enabledServices.contains(expectedComponent.flattenToShortString())
        } catch (e: Exception) {
            false
        }
    }

    fun canWriteSystemSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isDndPolicyGranted(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun isNotificationListenerEnabled(): Boolean {
        if (AetherNotificationListenerService.isConnected.value) return true
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledPackages.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun isUsageStatsGranted(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun inspectAllCapabilities(): List<CapabilityPermissionStatus> {
        val results = mutableListOf<CapabilityPermissionStatus>()

        // 1. Accessibility Service (Screen Reading, Taps, Clicks, Scrolls, Multi-Step UI Automation)
        val accEnabled = isAccessibilityServiceEnabled()
        results.add(
            CapabilityPermissionStatus(
                id = "accessibility_service",
                title = "Accessibility Service (Live Screen Vision & Gestures)",
                subtitle = "Read screen hierarchy, perform taps, clicks, scrolls & autonomous cross-app steps",
                permissionName = "android.permission.BIND_ACCESSIBILITY_SERVICE",
                kind = PermissionKind.SYSTEM_SERVICE_TOGGLE,
                isGranted = accEnabled,
                actionButtonText = if (accEnabled) "Configured" else "Enable in Accessibility"
            )
        )

        // 2. Modify System Settings (WRITE_SETTINGS)
        val writeSettings = canWriteSystemSettings()
        results.add(
            CapabilityPermissionStatus(
                id = "write_settings",
                title = "Modify System Settings (WRITE_SETTINGS)",
                subtitle = "Control global OS screen brightness, display timeout, and system toggles",
                permissionName = Manifest.permission.WRITE_SETTINGS,
                kind = PermissionKind.SPECIAL_OS_INTENT,
                isGranted = writeSettings,
                actionButtonText = if (writeSettings) "Granted" else "Grant Write Settings"
            )
        )

        // 3. Microphone / Voice Recognition (RECORD_AUDIO)
        val micGranted = isRuntimePermissionGranted(Manifest.permission.RECORD_AUDIO)
        results.add(
            CapabilityPermissionStatus(
                id = "record_audio",
                title = "Microphone & Live Voice Stream (RECORD_AUDIO)",
                subtitle = "Continuous voice-activated commands, wake-word macros & Gemini/GPT Live audio",
                permissionName = Manifest.permission.RECORD_AUDIO,
                kind = PermissionKind.RUNTIME_DANGEROUS,
                isGranted = micGranted,
                actionButtonText = if (micGranted) "Granted" else "Grant Mic"
            )
        )

        // 4. Camera & Hardware Torch (CAMERA)
        val camGranted = isRuntimePermissionGranted(Manifest.permission.CAMERA)
        results.add(
            CapabilityPermissionStatus(
                id = "camera_vision",
                title = "Camera Vision & Torch Actuator (CAMERA)",
                subtitle = "Visual environment scanning, QR device sync pairing & flashlight hardware",
                permissionName = Manifest.permission.CAMERA,
                kind = PermissionKind.RUNTIME_DANGEROUS,
                isGranted = camGranted,
                actionButtonText = if (camGranted) "Granted" else "Grant Camera"
            )
        )

        // 5. Display Over Other Apps (SYSTEM_ALERT_WINDOW)
        val overlayGranted = canDrawOverlays()
        results.add(
            CapabilityPermissionStatus(
                id = "system_overlay",
                title = "Display Over Other Apps (SYSTEM_ALERT_WINDOW)",
                subtitle = "Floating Aether Live Voice & Vision HUD while using third-party applications",
                permissionName = Manifest.permission.SYSTEM_ALERT_WINDOW,
                kind = PermissionKind.SPECIAL_OS_INTENT,
                isGranted = overlayGranted,
                actionButtonText = if (overlayGranted) "Granted" else "Grant Overlay"
            )
        )

        // 6. Do Not Disturb & Audio Policy (ACCESS_NOTIFICATION_POLICY)
        val dndGranted = isDndPolicyGranted()
        results.add(
            CapabilityPermissionStatus(
                id = "dnd_policy",
                title = "Do Not Disturb & Audio Policy (ACCESS_NOTIFICATION_POLICY)",
                subtitle = "Toggle Silent Mode, Vibrate Mode, and DND profiles via voice macros",
                permissionName = Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                kind = PermissionKind.SPECIAL_OS_INTENT,
                isGranted = dndGranted,
                actionButtonText = if (dndGranted) "Granted" else "Grant DND Access"
            )
        )

        // 7. Notification Listener Service (BIND_NOTIFICATION_LISTENER_SERVICE)
        val notifListener = isNotificationListenerEnabled()
        results.add(
            CapabilityPermissionStatus(
                id = "notification_listener",
                title = "Notification Listener & Reader Service",
                subtitle = "Read incoming alerts aloud and trigger autonomous workflows from notifications",
                permissionName = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                kind = PermissionKind.SYSTEM_SERVICE_TOGGLE,
                isGranted = notifListener,
                actionButtonText = if (notifListener) "Enabled" else "Enable Listener"
            )
        )

        // 8. Usage Stats & Foreground App Telemetry (PACKAGE_USAGE_STATS)
        val usageGranted = isUsageStatsGranted()
        results.add(
            CapabilityPermissionStatus(
                id = "usage_stats",
                title = "App Usage & Foreground Process Access (PACKAGE_USAGE_STATS)",
                subtitle = "Detect active foreground apps and optimize cross-application workflows",
                permissionName = Manifest.permission.PACKAGE_USAGE_STATS,
                kind = PermissionKind.SPECIAL_OS_INTENT,
                isGranted = usageGranted,
                actionButtonText = if (usageGranted) "Granted" else "Grant Usage Access"
            )
        )

        // 9. Unrestricted Battery / Background Execution (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        val batteryUnrestricted = isIgnoringBatteryOptimizations()
        results.add(
            CapabilityPermissionStatus(
                id = "battery_optimization",
                title = "Unrestricted Background Execution (Ignore Doze)",
                subtitle = "Keep Aether Live Voice Partner running 24/7 in background without throttling",
                permissionName = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                kind = PermissionKind.SPECIAL_OS_INTENT,
                isGranted = batteryUnrestricted,
                actionButtonText = if (batteryUnrestricted) "Unrestricted" else "Allow Background"
            )
        )

        // 10. Bluetooth & Cross-Device Mesh (BLUETOOTH_CONNECT / SCAN)
        val btGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isRuntimePermissionGranted(Manifest.permission.BLUETOOTH_CONNECT) &&
                isRuntimePermissionGranted(Manifest.permission.BLUETOOTH_SCAN)
        } else true
        results.add(
            CapabilityPermissionStatus(
                id = "bluetooth_mesh",
                title = "Bluetooth & Cross-Device Mesh (BLUETOOTH_CONNECT & SCAN)",
                subtitle = "Connect and control nearby phones, smart home hubs & desktop companions",
                permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH,
                kind = PermissionKind.RUNTIME_DANGEROUS,
                isGranted = btGranted,
                actionButtonText = if (btGranted) "Granted" else "Grant Bluetooth"
            )
        )

        // 11. Location & Smart Home Geofencing (ACCESS_FINE_LOCATION)
        val locGranted = isRuntimePermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            isRuntimePermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        results.add(
            CapabilityPermissionStatus(
                id = "fine_location",
                title = "Smart Home Geofencing & Wi-Fi Discovery (ACCESS_FINE_LOCATION)",
                subtitle = "Trigger home/away voice macros and scan local Wi-Fi/Matter bridges",
                permissionName = Manifest.permission.ACCESS_FINE_LOCATION,
                kind = PermissionKind.RUNTIME_DANGEROUS,
                isGranted = locGranted,
                actionButtonText = if (locGranted) "Granted" else "Grant Location"
            )
        )

        // 12. Post Notifications (POST_NOTIFICATIONS)
        val postNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isRuntimePermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else true
        results.add(
            CapabilityPermissionStatus(
                id = "post_notifications",
                title = "Live Agent Alerts & Foreground Status (POST_NOTIFICATIONS)",
                subtitle = "Display live background session controls and autonomous task alerts",
                permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "POST_NOTIFICATIONS",
                kind = PermissionKind.RUNTIME_DANGEROUS,
                isGranted = postNotifGranted,
                actionButtonText = if (postNotifGranted) "Granted" else "Grant Alerts"
            )
        )

        return results
    }

    fun launchSpecialPermissionIntent(capabilityId: String): String {
        return try {
            val intent = when (capabilityId) {
                "accessibility_service" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                "write_settings" -> Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                "system_overlay" -> Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                "dnd_policy" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                "notification_listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                "usage_stats" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                "battery_optimization" -> Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                else -> Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened Android System Settings for [$capabilityId]"
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                "Opened Android System Settings"
            } catch (_: Exception) {
                "Unable to launch system settings panel on this device"
            }
        }
    }

    fun openAppDetailsSettings(): String {
        return launchSpecialPermissionIntent("app_details")
    }
}
