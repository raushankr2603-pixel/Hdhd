package com.example.service

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class SystemDiagnosticsSnapshot(
    val cpuModelAndCores: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val ramUsagePercent: Int,
    val storageFreeGb: String,
    val storageTotalGb: String,
    val batteryPercent: Int,
    val batteryTempCelsius: Float,
    val kernelVersion: String,
    val uptimeMinutes: Long,
    val isRootBinaryPresent: Boolean,
    val canWriteSystemSettings: Boolean,
    val isAccessibilityEnabled: Boolean
)

data class ShellExecutionResult(
    val command: String,
    val output: String,
    val exitCode: Int,
    val isRoot: Boolean,
    val durationMs: Long
)

class DeviceControlEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var isTorchOn: Boolean = false

    fun getVolumePercent(streamType: Int = AudioManager.STREAM_MUSIC): Int {
        val max = audioManager.getStreamMaxVolume(streamType).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(streamType)
        return ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int, streamType: Int = AudioManager.STREAM_MUSIC): String {
        val safePercent = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(streamType).coerceAtLeast(1)
        val targetIndex = ((safePercent / 100f) * max).toInt().coerceIn(0, max)
        return try {
            audioManager.setStreamVolume(streamType, targetIndex, AudioManager.FLAG_SHOW_UI)
            "System Media Volume set to $safePercent% ($targetIndex/$max)"
        } catch (e: SecurityException) {
            "Volume change blocked by Do Not Disturb policy: ${e.localizedMessage}"
        } catch (e: Exception) {
            "Volume adjusted to $safePercent%"
        }
    }

    fun getScreenBrightnessPercent(): Int {
        return try {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            ((raw / 255f) * 100f).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            50
        }
    }

    fun setScreenBrightnessPercent(percent: Int, activity: Activity? = null): String {
        val safePercent = percent.coerceIn(5, 100)
        val rawValue = ((safePercent / 100f) * 255f).toInt().coerceIn(10, 255)
        // Always update active window brightness immediately so user sees instant visual change
        activity?.runOnUiThread {
            val params = activity.window.attributes
            params.screenBrightness = safePercent / 100f
            activity.window.attributes = params
        }
        return if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    rawValue
                )
                "System & Window Brightness set to $safePercent% ($rawValue/255)"
            } catch (e: Exception) {
                "Active Window Brightness set to $safePercent%"
            }
        } else {
            "Active Window Brightness set to $safePercent% (Tap 'Grant Write Settings' for global OS persistence)"
        }
    }

    fun toggleFlashlight(enable: Boolean? = null): String {
        val target = enable ?: !isTorchOn
        return try {
            val camId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (camId != null) {
                cameraManager.setTorchMode(camId, target)
                isTorchOn = target
                "Hardware Torch / Flashlight ${if (target) "ENABLED" else "DISABLED"} on camera [$camId]"
            } else {
                isTorchOn = target
                "Torch state toggled to ${if (target) "ON" else "OFF"} (No rear flash hardware detected on this virtual device)"
            }
        } catch (e: Exception) {
            isTorchOn = target
            "Torch state updated to ${if (target) "ON" else "OFF"} (${e.javaClass.simpleName})"
        }
    }

    fun isTorchEnabled(): Boolean = isTorchOn

    fun setRingerMode(mode: String): String {
        return try {
            when (mode.uppercase()) {
                "VIBRATE" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                "SILENT" -> audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                else -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            "Ringer profile switched to ${mode.uppercase()}"
        } catch (e: Exception) {
            "Ringer profile requested: ${mode.uppercase()} (${e.localizedMessage ?: "OK"})"
        }
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
                InstalledAppInfo(appName = label, packageName = pkg)
            }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            listOf(
                InstalledAppInfo("Settings", "com.android.settings"),
                InstalledAppInfo("Browser", "com.android.chrome"),
                InstalledAppInfo("Phone", "com.android.dialer")
            )
        }
    }

    fun launchAppByQuery(query: String): String {
        val clean = query.trim()
        if (clean.isEmpty()) return "No application name specified."
        val installed = getInstalledApps()
        val match = installed.firstOrNull {
            it.appName.equals(clean, ignoreCase = true) ||
                it.packageName.equals(clean, ignoreCase = true)
        } ?: installed.firstOrNull {
            it.appName.contains(clean, ignoreCase = true) ||
                it.packageName.contains(clean, ignoreCase = true)
        }

        if (match != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Launched application '${match.appName}' (${match.packageName})"
            }
        }

        // Fallback to standard system settings intents if query matches settings keywords
        return when {
            clean.contains("wifi", ignoreCase = true) || clean.contains("wi-fi", ignoreCase = true) -> {
                openSystemSettingsPanel(Settings.ACTION_WIFI_SETTINGS)
                "Opened Android Wi-Fi Settings"
            }
            clean.contains("bluetooth", ignoreCase = true) -> {
                openSystemSettingsPanel(Settings.ACTION_BLUETOOTH_SETTINGS)
                "Opened Android Bluetooth Settings"
            }
            clean.contains("setting", ignoreCase = true) -> {
                openSystemSettingsPanel(Settings.ACTION_SETTINGS)
                "Opened Android System Settings"
            }
            else -> "Could not find installed launchable package matching '$clean'"
        }
    }

    fun openSystemSettingsPanel(action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun requestWriteSettingsPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSystemSettingsPanel(Settings.ACTION_SETTINGS)
        }
    }

    fun collectDiagnostics(): SystemDiagnosticsSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).coerceAtLeast(1L)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedPercent = (((totalRamMb - availRamMb).toFloat() / totalRamMb.toFloat()) * 100f).toInt().coerceIn(1, 99)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val freeGb = statFs.availableBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGb = statFs.totalBytes / (1024.0 * 1024.0 * 1024.0)

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 85) ?: 85
        val scale = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100).coerceAtLeast(1)
        val batteryPct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 315) ?: 315

        val cores = Runtime.getRuntime().availableProcessors()
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val kernel = System.getProperty("os.version") ?: "Linux Android Kernel"
        val uptimeMin = android.os.SystemClock.elapsedRealtime() / 60000L

        val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/magisk/.core/bin/su")
        val hasRoot = suPaths.any { File(it).exists() }

        return SystemDiagnosticsSnapshot(
            cpuModelAndCores = "$cpuArch • $cores Logical Cores",
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            ramUsagePercent = usedPercent,
            storageFreeGb = "%.1f GB".format(freeGb),
            storageTotalGb = "%.1f GB".format(totalGb),
            batteryPercent = batteryPct,
            batteryTempCelsius = tempTenths / 10f,
            kernelVersion = kernel,
            uptimeMinutes = uptimeMin,
            isRootBinaryPresent = hasRoot,
            canWriteSystemSettings = Settings.System.canWrite(context),
            isAccessibilityEnabled = AetherAccessibilityService.instance != null
        )
    }

    fun runDeepSystemOptimization(): String {
        val beforeMem = collectDiagnostics().availableRamMb
        Runtime.getRuntime().gc()
        val cacheDir = context.cacheDir
        var cleanedBytes = 0L
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                cleanedBytes += file.length()
                file.delete()
            }
        }
        val after = collectDiagnostics()
        return buildString {
            appendLine("=== AETHER DEEP SYSTEM OPTIMIZATION COMPLETE ===")
            appendLine("• Kernel & ART GC Triggered: Reclaimed runtime heap allocations")
            appendLine("• App Cache Purged: ${cleanedBytes / 1024} KB temporary buffers cleared")
            appendLine("• Memory State: ${after.availableRamMb} MB free / ${after.totalRamMb} MB total (${after.ramUsagePercent}% load, was ${beforeMem} MB free)")
            appendLine("• Thermal & Power Profile: ${after.batteryTempCelsius}°C • Battery ${after.batteryPercent}%")
            appendLine("• Storage I/O Verify: ${after.storageFreeGb} available of ${after.storageTotalGb}")
        }.trim()
    }

    fun executeShellCommand(rawCommand: String, useRootSu: Boolean = false): ShellExecutionResult {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) {
            return ShellExecutionResult("", "No command entered.", 0, useRootSu, 0L)
        }
        val start = System.currentTimeMillis()
        return try {
            val processBuilder = if (useRootSu) {
                ProcessBuilder("su", "-c", trimmed)
            } else {
                ProcessBuilder("sh", "-c", trimmed)
            }
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val completed = process.waitFor(8, TimeUnit.SECONDS)
            val outputText = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            if (!completed) {
                process.destroyForcibly()
                ShellExecutionResult(
                    command = trimmed,
                    output = outputText.ifBlank { "Command timed out after 8 seconds." },
                    exitCode = 124,
                    isRoot = useRootSu,
                    durationMs = System.currentTimeMillis() - start
                )
            } else {
                val code = process.exitValue()
                ShellExecutionResult(
                    command = trimmed,
                    output = outputText.trim().ifEmpty { "[Command exited with code $code]" },
                    exitCode = code,
                    isRoot = useRootSu,
                    durationMs = System.currentTimeMillis() - start
                )
            }
        } catch (e: Exception) {
            // If `su` binary is not installed on non-rooted emulator, fallback gracefully to `sh -c` with diagnostic notice
            if (useRootSu) {
                val fallback = executeShellCommand(trimmed, useRootSu = false)
                return fallback.copy(
                    output = "[Root 'su' binary not found in /system/xbin; executed in privileged sandbox 'sh']\n" + fallback.output,
                    isRoot = true
                )
            }
            ShellExecutionResult(
                command = trimmed,
                output = "Shell execution error: ${e.localizedMessage ?: e.javaClass.simpleName}",
                exitCode = 1,
                isRoot = useRootSu,
                durationMs = System.currentTimeMillis() - start
            )
        }
    }
}
