package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TermuxPluginStatus(
    val packageName: String,
    val title: String,
    val roleDescription: String,
    val isInstalled: Boolean,
    val versionName: String,
    val githubRepoUrl: String
)

data class TermuxBinaryProbe(
    val binaryName: String,
    val resolvedPath: String,
    val isAvailable: Boolean,
    val sourceLayer: String // "Android System", "Termux usr/bin", "Local Userland"
)

data class TermuxUserlandScript(
    val fileName: String,
    val absolutePath: String,
    val interpreter: String,
    val sizeBytes: Long,
    val content: String,
    val lastModifiedLabel: String
)

data class TermuxEnvironmentSnapshot(
    val isTermuxAppInstalled: Boolean,
    val termuxVersion: String,
    val isRunCommandPermissionGranted: Boolean,
    val localUserlandHomePath: String,
    val termuxPrefixPath: String = "/data/data/com.termux/files/usr",
    val termuxHomePath: String = "/data/data/com.termux/files/home",
    val plugins: List<TermuxPluginStatus>,
    val binaryProbes: List<TermuxBinaryProbe>,
    val savedScripts: List<TermuxUserlandScript>
)

data class TermuxExecutionOutcome(
    val commandOrScript: String,
    val executionMode: String, // "TERMUX_RUN_COMMAND_SERVICE" or "LOCAL_POSIX_USERLAND"
    val exitCode: Int,
    val stdoutAndStderr: String,
    val workingDirectory: String,
    val elapsedMs: Long
)

class TermuxBridgeEngine(private val context: Context) {

    val localUserlandHome: File by lazy {
        File(context.filesDir, "termux_home").apply { mkdirs() }
    }

    val localScriptsDir: File by lazy {
        File(localUserlandHome, "scripts").apply { mkdirs() }
    }

    val localBinDir: File by lazy {
        File(localUserlandHome, "bin").apply { mkdirs() }
    }

    init {
        ensureDefaultUserlandFiles()
    }

    private fun ensureDefaultUserlandFiles() {
        val termuxPropsDir = File(localUserlandHome, ".termux").apply { mkdirs() }
        val propsFile = File(termuxPropsDir, "termux.properties")
        if (!propsFile.exists()) {
            propsFile.writeText(
                """
                # AetherOS + Termux External Command Bridge Configuration
                allow-external-apps=true
                bell-character=ignore
                use-black-ui=true
                shortcut.create-session=ctrl + t
                """.trimIndent()
            )
        }

        val defaultSysAuditScript = File(localScriptsDir, "aether_termux_doctor.sh")
        if (!defaultSysAuditScript.exists()) {
            defaultSysAuditScript.writeText(
                """
                #!/system/bin/sh
                echo "=== AETHER-OS + TERMUX & ANYCLAW POSIX DOCTOR ==="
                echo "Timestamp: $(date)"
                echo "Kernel: $(uname -a)"
                echo "Android SDK: $(getprop ro.build.version.sdk) (Release $(getprop ro.build.version.release))"
                echo "CPU ABI: $(getprop ro.product.cpu.abi)"
                echo "Userland Home: ${'$'}HOME"
                echo "--- Memory Snapshot ---"
                head -n 6 /proc/meminfo
                echo "--- Storage Mounts ---"
                df -h | head -n 6
                """.trimIndent()
            )
            defaultSysAuditScript.setExecutable(true)
        }

        val anyClawBootstrapScript = File(localScriptsDir, "bootstrap_anyclaw_openclaw.sh")
        if (!anyClawBootstrapScript.exists()) {
            anyClawBootstrapScript.writeText(
                """
                #!/system/bin/sh
                echo "=== ANYCLAW / OPENCLAW & TERMUX SUB-DEPENDENCY BOOTSTRAP ==="
                echo "Checking Termux Prefix: /data/data/com.termux/files/usr"
                if [ -d "/data/data/com.termux/files/usr/bin" ]; then
                  echo "[OK] Native Termux prefix directory detected."
                else
                  echo "[INFO] Running in AetherOS Embedded POSIX Userland (${'$'}HOME)."
                fi
                echo "Workspace files in OpenClaw directory:"
                ls -la "${File(context.filesDir, "openclaw_workspace").absolutePath}" 2>/dev/null || echo "Initializing workspace..."
                echo "Available shell utilities:"
                which sh toybox ping getprop top ps df 2>/dev/null
                """.trimIndent()
            )
            anyClawBootstrapScript.setExecutable(true)
        }
    }

    fun inspectEnvironment(): TermuxEnvironmentSnapshot {
        val pm = context.packageManager
        val termuxInfo = getPackageVersion(pm, "com.termux")
        val isRunCmdGranted = ContextCompat.checkSelfPermission(
            context,
            "com.termux.permission.RUN_COMMAND"
        ) == PackageManager.PERMISSION_GRANTED

        val pluginSpecs = listOf(
            Triple("com.termux", "Termux Core Terminal & Linux Environment", "Main Linux userland, pkg manager & RunCommandService"),
            Triple("com.termux.api", "Termux:API Hardware & Sensor Add-on", "CLI access to Android battery, camera, microphone, TTS, SMS & clipboard"),
            Triple("com.termux.boot", "Termux:Boot Autonomous Startup Daemon", "Runs ~/.termux/boot/ scripts automatically on Android device boot"),
            Triple("com.termux.widget", "Termux:Widget Home Screen Shortcuts", "Executes ~/.shortcuts/ automation scripts directly from launcher"),
            Triple("com.termux.window", "Termux:Float Overlay Terminal Window", "Floating picture-in-picture Linux terminal window over any app"),
            Triple("com.termux.styling", "Termux:Styling Color Schemes & Fonts", "Custom terminal color palettes and powerline Nerd Fonts"),
            Triple("com.termux.x11", "Termux:X11 Graphical Linux Desktop", "Hardware-accelerated X11 server for desktop Linux GUI apps on Android")
        )

        val plugins = pluginSpecs.map { (pkg, title, desc) ->
            val ver = getPackageVersion(pm, pkg)
            val repoSlug = when (pkg) {
                "com.termux" -> "termux/termux-app"
                "com.termux.api" -> "termux/termux-api"
                "com.termux.boot" -> "termux/termux-boot"
                "com.termux.widget" -> "termux/termux-widget"
                "com.termux.window" -> "termux/termux-float"
                "com.termux.styling" -> "termux/termux-styling"
                "com.termux.x11" -> "termux/termux-x11"
                else -> "termux/termux-app"
            }
            TermuxPluginStatus(
                packageName = pkg,
                title = title,
                roleDescription = desc,
                isInstalled = ver != null,
                versionName = ver ?: "Not Installed (Userland Ready)",
                githubRepoUrl = "https://github.com/$repoSlug"
            )
        }

        val binariesToProbe = listOf(
            "sh", "toybox", "getprop", "ping", "top", "ps", "df", "cat", "uname",
            "bash", "git", "python3", "node", "curl", "ssh", "ffmpeg", "clang"
        )
        val probes = binariesToProbe.map { bin ->
            probeBinary(bin)
        }

        return TermuxEnvironmentSnapshot(
            isTermuxAppInstalled = termuxInfo != null,
            termuxVersion = termuxInfo ?: "Embedded POSIX Userland Active",
            isRunCommandPermissionGranted = isRunCmdGranted,
            localUserlandHomePath = localUserlandHome.absolutePath,
            plugins = plugins,
            binaryProbes = probes,
            savedScripts = listUserlandScripts()
        )
    }

    private fun getPackageVersion(pm: PackageManager, packageName: String): String? {
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            pkgInfo.versionName ?: "Installed"
        } catch (_: Exception) {
            null
        }
    }

    private fun probeBinary(binaryName: String): TermuxBinaryProbe {
        val termuxBin = File("/data/data/com.termux/files/usr/bin", binaryName)
        if (termuxBin.exists()) {
            return TermuxBinaryProbe(binaryName, termuxBin.absolutePath, true, "Termux usr/bin")
        }
        val localBin = File(localBinDir, binaryName)
        if (localBin.exists()) {
            return TermuxBinaryProbe(binaryName, localBin.absolutePath, true, "Local Userland")
        }
        val sysPaths = listOf("/system/bin", "/system/xbin", "/vendor/bin", "/sbin")
        for (dir in sysPaths) {
            val candidate = File(dir, binaryName)
            if (candidate.exists()) {
                return TermuxBinaryProbe(binaryName, candidate.absolutePath, true, "Android System")
            }
        }
        return TermuxBinaryProbe(
            binaryName = binaryName,
            resolvedPath = "/data/data/com.termux/files/usr/bin/$binaryName",
            isAvailable = false,
            sourceLayer = "Install via Termux pkg"
        )
    }

    fun listUserlandScripts(): List<TermuxUserlandScript> {
        val df = SimpleDateFormat("MMM dd HH:mm:ss", Locale.US)
        return (localScriptsDir.listFiles()?.toList() ?: emptyList())
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val text = try {
                    file.readText()
                } catch (_: Exception) {
                    ""
                }
                val firstLine = text.lineSequence().firstOrNull().orEmpty()
                val interp = when {
                    firstLine.startsWith("#!") -> firstLine.removePrefix("#!").trim()
                    file.name.endsWith(".py") -> "/data/data/com.termux/files/usr/bin/python3"
                    file.name.endsWith(".js") -> "/data/data/com.termux/files/usr/bin/node"
                    else -> "/system/bin/sh"
                }
                TermuxUserlandScript(
                    fileName = file.name,
                    absolutePath = file.absolutePath,
                    interpreter = interp,
                    sizeBytes = file.length(),
                    content = text,
                    lastModifiedLabel = df.format(Date(file.lastModified()))
                )
            }
    }

    fun saveUserlandScript(fileName: String, content: String): TermuxUserlandScript {
        val cleanName = fileName.trim().ifEmpty { "custom_script.sh" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(localScriptsDir, cleanName)
        val finalContent = if (content.trimStart().startsWith("#!")) {
            content
        } else {
            "#!/system/bin/sh\n$content"
        }
        file.writeText(finalContent)
        file.setExecutable(true)
        return listUserlandScripts().first { it.fileName == cleanName }
    }

    fun deleteUserlandScript(fileName: String): Boolean {
        val file = File(localScriptsDir, fileName)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Dispatches a command to Termux's official `com.termux.app.RunCommandService` if Termux is installed
     * AND also executes the command in the real local POSIX userland so output is captured immediately.
     */
    fun executeCommand(
        rawCommand: String,
        dispatchToTermuxService: Boolean = true,
        runInBackground: Boolean = true
    ): TermuxExecutionOutcome {
        val startMs = System.currentTimeMillis()
        val cleanCmd = rawCommand.trim()
        if (cleanCmd.isEmpty()) {
            return TermuxExecutionOutcome(
                commandOrScript = "",
                executionMode = "LOCAL_POSIX_USERLAND",
                exitCode = -1,
                stdoutAndStderr = "No command provided.",
                workingDirectory = localUserlandHome.absolutePath,
                elapsedMs = 0L
            )
        }

        var termuxServiceNote = ""
        val envSnapshot = inspectEnvironment()
        if (dispatchToTermuxService && envSnapshot.isTermuxAppInstalled) {
            termuxServiceNote = dispatchTermuxRunCommandIntent(
                executablePath = "/data/data/com.termux/files/usr/bin/bash",
                arguments = arrayOf("-c", cleanCmd),
                workdir = "/data/data/com.termux/files/home",
                background = runInBackground
            )
        }

        return try {
            val pb = ProcessBuilder("sh", "-c", cleanCmd)
            pb.directory(localUserlandHome)
            pb.redirectErrorStream(true)
            val env = pb.environment()
            env["HOME"] = localUserlandHome.absolutePath
            env["TERMUX_WORKSPACE"] = File(context.filesDir, "openclaw_workspace").absolutePath
            env["PREFIX"] = "/data/data/com.termux/files/usr"
            env["TMPDIR"] = context.cacheDir.absolutePath
            val currentPath = env["PATH"] ?: "/system/bin:/system/xbin"
            env["PATH"] = "${localBinDir.absolutePath}:/data/data/com.termux/files/usr/bin:$currentPath"

            val process = pb.start()
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = if (finished) process.exitValue() else 124
            val combinedOut = buildString {
                if (termuxServiceNote.isNotEmpty()) {
                    appendLine("[Termux RunCommandService] $termuxServiceNote")
                }
                if (output.isNotEmpty()) {
                    append(output)
                } else {
                    append("(Command completed with exit code $exitCode)")
                }
            }
            TermuxExecutionOutcome(
                commandOrScript = cleanCmd,
                executionMode = if (termuxServiceNote.isNotEmpty()) {
                    "TERMUX_RUN_COMMAND_SERVICE + LOCAL_POSIX"
                } else {
                    "LOCAL_POSIX_USERLAND"
                },
                exitCode = exitCode,
                stdoutAndStderr = combinedOut,
                workingDirectory = localUserlandHome.absolutePath,
                elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            )
        } catch (e: Exception) {
            TermuxExecutionOutcome(
                commandOrScript = cleanCmd,
                executionMode = "LOCAL_POSIX_USERLAND",
                exitCode = -1,
                stdoutAndStderr = "Execution error: ${e.localizedMessage ?: e.javaClass.simpleName}",
                workingDirectory = localUserlandHome.absolutePath,
                elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            )
        }
    }

    /**
     * Sends the official `com.termux.RUN_COMMAND` intent to `com.termux.app.RunCommandService`.
     */
    fun dispatchTermuxRunCommandIntent(
        executablePath: String = "/data/data/com.termux/files/usr/bin/bash",
        arguments: Array<String>,
        workdir: String = "/data/data/com.termux/files/home",
        background: Boolean = true
    ): String {
        return try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.shared.intent.extra.COMMAND_PATH", executablePath)
                putExtra("com.termux.shared.intent.extra.ARGUMENTS", arguments)
                putExtra("com.termux.shared.intent.extra.WORKDIR", workdir)
                putExtra("com.termux.shared.intent.extra.BACKGROUND", background)
                putExtra("com.termux.shared.intent.extra.SESSION_ACTION", "0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            "Dispatched com.termux.RUN_COMMAND to com.termux.app.RunCommandService ($executablePath ${arguments.joinToString(" ")})"
        } catch (e: Exception) {
            "Termux RunCommandService note: ${e.localizedMessage ?: e.javaClass.simpleName} (Ensure allow-external-apps=true in ~/.termux/termux.properties and grant RUN_COMMAND permission)"
        }
    }

    /**
     * Copies a one-line setup command to the Android Clipboard and optionally opens Termux so the user
     * can bootstrap all sub-dependencies (git, nodejs, python, clang, ffmpeg, termux-api, proot-distro, openclaw, anyclaw).
     */
    fun copyBootstrapCommandToClipboard(bootstrapType: String): String {
        val commandText = when (bootstrapType.uppercase()) {
            "ALLOW_EXTERNAL" -> "mkdir -p ~/.termux && echo 'allow-external-apps=true' >> ~/.termux/termux.properties && termux-reload-settings"
            "CORE_PACKAGES" -> "pkg update -y && pkg upgrade -y && pkg install -y git nodejs python clang curl wget openssh ffmpeg jq termux-api proot-distro"
            "ANYCLAW_OPENCLAW" -> "pkg install -y git nodejs python && npm install -g openclaw anyclaw @modelcontextprotocol/sdk"
            "OPENMUSE_OPENDOTS" -> "pkg install -y git nodejs && git clone https://github.com/CopilotKit/OpenMuse.git && git clone https://github.com/CopilotKit/OpenDots.git"
            else -> "pkg update -y && pkg install -y git nodejs python termux-api"
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Termux Bootstrap Command", commandText))
        return commandText
    }

    fun launchTermuxOrSettings(): String {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Launched Termux Terminal App"
            } else {
                val settingsIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(settingsIntent)
                "Termux APK not installed; opened App Permissions & Settings"
            }
        } catch (e: Exception) {
            "Could not launch Termux: ${e.localizedMessage}"
        }
    }
}
