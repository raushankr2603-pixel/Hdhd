package com.example.domain

import android.content.Context
import com.example.data.local.ClawGatewayChannelEntity
import com.example.data.local.ClawSkillEntity
import com.example.data.local.OpenDotsCoworkerEntity
import com.example.data.local.OpenDotsSpacePageEntity
import com.example.data.security.E2EEVault
import com.example.service.AetherAccessibilityService
import com.example.service.DeviceControlEngine
import com.example.service.TermuxBridgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * AG-UI (Agent-User Interaction) Protocol wire event as used by CopilotKit OpenMuse & OpenDots.
 */
data class AgUiProtocolEvent(
    val id: Long = System.nanoTime(),
    val eventType: String, // "RUN_STARTED", "TOOL_CALL_START", "TOOL_CALL_RESULT", "STATE_DELTA", "HITL_CHECKPOINT", "TEXT_MESSAGE_CONTENT", "RUN_FINISHED"
    val sourceAgent: String,
    val summary: String,
    val jsonPayload: String,
    val timestampLabel: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
)

/**
 * Snapshot of an OpenClaw / AnyClaw workspace file on disk (`MEMORY.md`, `SOUL.md`, `TOOLS.md`, or daily log).
 */
data class WorkspaceMarkdownFile(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val lastUpdatedLabel: String,
    val content: String
)

/**
 * Snapshot of the OpenMuse Persistent Browser Worker DOM inspection.
 */
data class BrowserDomInspectionResult(
    val url: String,
    val pageTitle: String,
    val statusCode: Int,
    val interactiveElements: List<String>,
    val headingsAndSummary: String,
    val isManualTakeoverActive: Boolean,
    val inspectedAtLabel: String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
)

class OpenAgentEcosystemEngine(
    private val context: Context,
    private val deviceControl: DeviceControlEngine,
    val termuxBridge: TermuxBridgeEngine
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val openClawWorkspaceDir: File by lazy {
        File(context.filesDir, "openclaw_workspace").apply { mkdirs() }
    }

    val openClawLogsDir: File by lazy {
        File(openClawWorkspaceDir, "logs").apply { mkdirs() }
    }

    val openDotsSpacesDir: File by lazy {
        File(context.filesDir, "opendots_spaces").apply { mkdirs() }
    }

    private val _agUiEvents = MutableStateFlow<List<AgUiProtocolEvent>>(emptyList())
    val agUiEvents: StateFlow<List<AgUiProtocolEvent>> = _agUiEvents.asStateFlow()

    private val _browserState = MutableStateFlow(
        BrowserDomInspectionResult(
            url = "https://github.com/CopilotKit/OpenMuse",
            pageTitle = "OpenMuse & OpenDots Persistent Chromium Worker",
            statusCode = 200,
            interactiveElements = listOf(
                "[LINK] CopilotKit/OpenMuse Repository",
                "[LINK] CopilotKit/OpenDots Workspace",
                "[LINK] openclaw/openclaw Gateway",
                "[BUTTON] Take Control (Manual Takeover)"
            ),
            headingsAndSummary = "Persistent Browser Worker initialized. Enter any URL to fetch live DOM elements, execute JS selectors, or toggle Manual Takeover.",
            isManualTakeoverActive = false
        )
    )
    val browserState: StateFlow<BrowserDomInspectionResult> = _browserState.asStateFlow()

    init {
        ensureWorkspaceFilesInitialized()
        emitAgUiEvent(
            eventType = "RUN_STARTED",
            sourceAgent = "OpenMuse + OpenClaw + OpenDots Kernel",
            summary = "Initialized AG-UI Event Stream, MEMORY.md/SOUL.md workspace files, and Termux POSIX bridge",
            jsonPayload = """{"protocol":"AG-UI/1.0","workspace":"${openClawWorkspaceDir.absolutePath}","hitlGate":"ENABLED"}"""
        )
    }

    fun emitAgUiEvent(
        eventType: String,
        sourceAgent: String,
        summary: String,
        jsonPayload: String
    ) {
        val ev = AgUiProtocolEvent(
            eventType = eventType,
            sourceAgent = sourceAgent,
            summary = summary,
            jsonPayload = jsonPayload
        )
        _agUiEvents.value = (listOf(ev) + _agUiEvents.value).take(45)
    }

    fun clearAgUiEvents() {
        _agUiEvents.value = emptyList()
    }

    // =========================================================================
    // 1. OPENCLAW & ANYCLAW ON-DISK WORKSPACE MEMORY (MEMORY.md, SOUL.md, TOOLS.md)
    // =========================================================================

    private fun ensureWorkspaceFilesInitialized() {
        val soulFile = File(openClawWorkspaceDir, "SOUL.md")
        if (!soulFile.exists()) {
            soulFile.writeText(
                """
                # SOUL.md — Agent Identity, Persona & Guardrails
                - **Agent Name**: AetherOS Nexus (OpenMuse + AnyClaw + OpenDots + Termux)
                - **Core Architecture**: Hybrid Local/Cloud Reasoning with Real Android Accessibility, Termux POSIX Userland, and OpenDots Spaces.
                - **Safety & HITL Directive**: Non-read-only document commits and self-improving skills require Human-in-the-Loop (HITL) approval.
                - **Privacy Principle**: API keys are encrypted via AES-256-GCM; local workspace files remain strictly on-device.
                """.trimIndent()
            )
        }

        val memoryFile = File(openClawWorkspaceDir, "MEMORY.md")
        if (!memoryFile.exists()) {
            memoryFile.writeText(
                """
                # MEMORY.md — Persistent Cross-Session Agent Memory
                - [FACT] Primary execution runtime: Android SDK ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.MODEL})
                - [FACT] Integrated Repositories: CopilotKit/OpenMuse, openclaw/openclaw, fastclaw-ai/anyclaw, CopilotKit/OpenDots, termux/termux-app
                - [PREFERENCE] Execute real shell commands via TermuxBridgeEngine and persist workspace pages in OpenDots Spaces.
                """.trimIndent()
            )
        }

        val toolsFile = File(openClawWorkspaceDir, "TOOLS.md")
        if (!toolsFile.exists()) {
            toolsFile.writeText(
                """
                # TOOLS.md — Registered MCP, Termux & Android Actuators
                - `termux.exec`: Executes POSIX/Bash commands via Termux RunCommandService & local userland (`${termuxBridge.localUserlandHome.absolutePath}`)
                - `browser.inspect_dom`: Inspects live web page DOM headings, links, forms & buttons via OpenMuse Browser Worker
                - `memory.append_fact`: Appends durable facts to `MEMORY.md` and daily log
                - `spaces.propose_page`: Drafts a Markdown page into OpenDots Spaces with a Human-in-the-Loop (HITL) approval checkpoint
                - `android.accessibility`: Dispatches real screen taps, double-taps, drags, pinch-zooms, and global actions
                """.trimIndent()
            )
        }

        appendDailyLogEntry("Initialized OpenClaw/AnyClaw on-disk workspace (${openClawWorkspaceDir.absolutePath}).")
    }

    fun getTodayLogFileName(): String {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "logs/$day.md"
    }

    fun appendDailyLogEntry(entry: String) {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val logFile = File(openClawLogsDir, "$day.md")
        if (!logFile.exists()) {
            logFile.writeText("# OpenClaw Daily Execution Log ($day)\n\n")
        }
        logFile.appendText("- `[$time]` $entry\n")
    }

    fun listWorkspaceMarkdownFiles(): List<WorkspaceMarkdownFile> {
        ensureWorkspaceFilesInitialized()
        val df = SimpleDateFormat("MMM dd HH:mm:ss", Locale.US)
        val rootFiles = listOf("MEMORY.md", "SOUL.md", "TOOLS.md").map { name ->
            val file = File(openClawWorkspaceDir, name)
            WorkspaceMarkdownFile(
                fileName = name,
                absolutePath = file.absolutePath,
                sizeBytes = file.length(),
                lastUpdatedLabel = df.format(Date(file.lastModified())),
                content = file.readText()
            )
        }
        val todayLogName = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".md"
        val logFile = File(openClawLogsDir, todayLogName)
        if (!logFile.exists()) {
            appendDailyLogEntry("Log file created.")
        }
        val logItem = WorkspaceMarkdownFile(
            fileName = "logs/$todayLogName",
            absolutePath = logFile.absolutePath,
            sizeBytes = logFile.length(),
            lastUpdatedLabel = df.format(Date(logFile.lastModified())),
            content = logFile.readText()
        )
        return rootFiles + logItem
    }

    fun saveWorkspaceMarkdownFile(fileName: String, newContent: String): WorkspaceMarkdownFile {
        val targetFile = if (fileName.startsWith("logs/")) {
            File(openClawWorkspaceDir, fileName)
        } else {
            File(openClawWorkspaceDir, fileName.substringAfterLast('/'))
        }
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(newContent)
        appendDailyLogEntry("Updated workspace file `$fileName` (${targetFile.length()} bytes).")
        emitAgUiEvent(
            eventType = "STATE_DELTA",
            sourceAgent = "OpenClaw Workspace FS",
            summary = "Saved $fileName to disk (${targetFile.length()} bytes)",
            jsonPayload = JSONObject().put("file", targetFile.absolutePath).put("bytes", targetFile.length()).toString()
        )
        return listWorkspaceMarkdownFiles().first { it.fileName == fileName }
    }

    fun appendFactToMemoryMd(factLine: String): WorkspaceMarkdownFile {
        val clean = factLine.trim()
        val memoryFile = File(openClawWorkspaceDir, "MEMORY.md")
        ensureWorkspaceFilesInitialized()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        memoryFile.appendText("\n- [$stamp] $clean")
        appendDailyLogEntry("Appended memory fact: $clean")
        emitAgUiEvent(
            eventType = "STATE_DELTA",
            sourceAgent = "OpenClaw MEMORY.md",
            summary = "Appended durable memory: $clean",
            jsonPayload = JSONObject().put("memoryFact", clean).toString()
        )
        return listWorkspaceMarkdownFiles().first { it.fileName == "MEMORY.md" }
    }

    fun buildWorkspacePromptContext(): String {
        ensureWorkspaceFilesInitialized()
        val soul = File(openClawWorkspaceDir, "SOUL.md").readText().take(500)
        val memory = File(openClawWorkspaceDir, "MEMORY.md").readText().takeLast(700)
        return "OPENCLAW WORKSPACE CONTEXT:\n[SOUL.md]\n$soul\n[MEMORY.md]\n$memory"
    }

    // =========================================================================
    // 2. OPENMUSE PERSISTENT BROWSER WORKER & MANUAL TAKEOVER ("TAKE CONTROL")
    // =========================================================================

    fun setBrowserManualTakeover(active: Boolean) {
        _browserState.value = _browserState.value.copy(isManualTakeoverActive = active)
        emitAgUiEvent(
            eventType = "STATE_DELTA",
            sourceAgent = "OpenMuse Browser Worker",
            summary = if (active) {
                "Manual Takeover ('Take Control') ENABLED — User has direct touch/keyboard control of WebView"
            } else {
                "Autonomous Agent Browser Control RESUMED"
            },
            jsonPayload = JSONObject().put("manualTakeover", active).put("url", _browserState.value.url).toString()
        )
    }

    fun updateBrowserDomFromWebView(url: String, title: String, domElementsJson: String) {
        val parsedItems = mutableListOf<String>()
        var summaryText = ""
        try {
            val obj = JSONObject(domElementsJson)
            summaryText = obj.optString("summary", "")
            val arr = obj.optJSONArray("elements")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    parsedItems.add(arr.getString(i))
                }
            }
        } catch (_: Exception) {
            summaryText = domElementsJson.take(300)
        }
        _browserState.value = _browserState.value.copy(
            url = url.ifBlank { _browserState.value.url },
            pageTitle = title.ifBlank { "Live WebView DOM" },
            statusCode = 200,
            interactiveElements = if (parsedItems.isNotEmpty()) parsedItems else _browserState.value.interactiveElements,
            headingsAndSummary = summaryText.ifBlank { "Live WebView DOM inspected (${parsedItems.size} interactive targets)" },
            inspectedAtLabel = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        )
        emitAgUiEvent(
            eventType = "TOOL_CALL_RESULT",
            sourceAgent = "OpenMuse Browser Worker",
            summary = "Inspected live WebView DOM for '${_browserState.value.pageTitle}' (${_browserState.value.interactiveElements.size} targets)",
            jsonPayload = JSONObject()
                .put("url", _browserState.value.url)
                .put("title", _browserState.value.pageTitle)
                .put("targetsCount", _browserState.value.interactiveElements.size)
                .toString()
        )
    }

    /**
     * Performs real HTTP fetch & HTML DOM parsing for any URL so OpenMuse Browser Worker and Dots
     * can inspect real live web pages even in background/headless mode.
     */
    suspend fun inspectUrlDomHeadless(targetUrl: String): BrowserDomInspectionResult = withContext(Dispatchers.IO) {
        val normalizedUrl = if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            targetUrl.trim()
        } else {
            "https://${targetUrl.trim()}"
        }
        emitAgUiEvent(
            eventType = "TOOL_CALL_START",
            sourceAgent = "OpenMuse Browser Worker",
            summary = "Navigating & extracting DOM from $normalizedUrl",
            jsonPayload = JSONObject().put("tool", "browser.inspect_dom").put("url", normalizedUrl).toString()
        )
        return@withContext try {
            val req = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; AetherOS-OpenMuse-Agent) AppleWebKit/537.36")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(html)?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim()
                    ?: normalizedUrl

                val links = Regex("<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .findAll(html)
                    .mapNotNull { m ->
                        val href = m.groupValues[1].trim()
                        val text = m.groupValues[2].replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
                        if (text.length in 2..60 && !href.startsWith("javascript:")) {
                            "[LINK] $text -> ${href.take(55)}"
                        } else null
                    }
                    .distinct()
                    .take(12)
                    .toList()

                val buttons = Regex("<button[^>]*>(.*?)</button>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .findAll(html)
                    .mapNotNull { m ->
                        val text = m.groupValues[1].replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
                        if (text.isNotEmpty()) "[BUTTON] $text" else null
                    }
                    .distinct()
                    .take(6)
                    .toList()

                val headings = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .findAll(html)
                    .mapNotNull { m ->
                        m.groupValues[1].replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
                            .takeIf { it.isNotEmpty() }
                    }
                    .distinct()
                    .take(6)
                    .joinToString(" • ")

                val combinedTargets = (buttons + links).ifEmpty {
                    listOf("[DOM] Document loaded (${html.length} bytes, HTTP ${resp.code})")
                }
                val result = BrowserDomInspectionResult(
                    url = normalizedUrl,
                    pageTitle = titleMatch,
                    statusCode = resp.code,
                    interactiveElements = combinedTargets,
                    headingsAndSummary = if (headings.isNotEmpty()) {
                        "Headings: $headings"
                    } else {
                        "Extracted ${combinedTargets.size} interactive DOM elements (${html.length} chars HTML)."
                    },
                    isManualTakeoverActive = _browserState.value.isManualTakeoverActive
                )
                _browserState.value = result
                emitAgUiEvent(
                    eventType = "TOOL_CALL_RESULT",
                    sourceAgent = "OpenMuse Browser Worker",
                    summary = "HTTP ${resp.code} • '$titleMatch' (${combinedTargets.size} interactive DOM elements)",
                    jsonPayload = JSONObject()
                        .put("url", normalizedUrl)
                        .put("status", resp.code)
                        .put("title", titleMatch)
                        .put("elements", combinedTargets.size)
                        .toString()
                )
                result
            }
        } catch (e: Exception) {
            val fallback = _browserState.value.copy(
                url = normalizedUrl,
                pageTitle = "Browser Worker Note (${e.javaClass.simpleName})",
                statusCode = 0,
                headingsAndSummary = "Could not reach $normalizedUrl: ${e.localizedMessage ?: "Check network or load in Live WebView"}"
            )
            _browserState.value = fallback
            fallback
        }
    }

    // =========================================================================
    // 3. OPENDOTS SPECIALIST COWORKERS ("DOTS") & SPACES/PAGES WITH HITL GATES
    // =========================================================================

    /**
     * Runs a Specialist OpenDots Coworker ("Dot") on a user task, executing real tools (Termux shell,
     * Browser DOM inspection, or system diagnostics) and creating a new Markdown page in OpenDots Spaces
     * gated by a Human-in-the-Loop (HITL) approval checkpoint.
     */
    suspend fun executeOpenDotCoworkerTask(
        coworker: OpenDotsCoworkerEntity,
        taskPrompt: String
    ): OpenDotsSpacePageEntity = withContext(Dispatchers.IO) {
        val cleanTask = taskPrompt.trim().ifEmpty {
            "Audit current Android + Termux + OpenClaw runtime state and produce an actionable engineering brief."
        }
        emitAgUiEvent(
            eventType = "RUN_STARTED",
            sourceAgent = coworker.dotName,
            summary = "Executing specialist task in sandbox [${coworker.sandboxEnvironment}]: $cleanTask",
            jsonPayload = JSONObject()
                .put("dot", coworker.dotName)
                .put("space", coworker.assignedSpaceName)
                .put("tools", coworker.allowedToolsCsv)
                .toString()
        )

        // Execute real underlying tools according to the Dot's allowed capabilities
        val shellSnapshot = termuxBridge.executeCommand(
            rawCommand = "uname -a && getprop ro.product.model && head -n 4 /proc/meminfo",
            dispatchToTermuxService = false
        )
        val diag = deviceControl.collectDiagnostics()
        val screenSnap = AetherAccessibilityService.activeSnapshot.value

        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val pageTitle = "${coworker.dotName.substringBefore("•").trim()}: ${cleanTask.take(44)}"
        val markdownDraft = buildString {
            appendLine("# $pageTitle")
            appendLine("- **Authored by Dot**: `${coworker.dotName}` (${coworker.roleTitle})")
            appendLine("- **Target Space**: `${coworker.assignedSpaceName}`")
            appendLine("- **Sandbox Environment**: `${coworker.sandboxEnvironment}`")
            appendLine("- **Timestamp**: `$stamp`")
            appendLine()
            appendLine("## 1. Task Objective & Instructions")
            appendLine("> $cleanTask")
            appendLine()
            appendLine("## 2. Live Tool & Sandbox Telemetry")
            appendLine("- **Active Foreground Window**: `${screenSnap.activePackage}` (${screenSnap.visibleElements.size} UI nodes)")
            appendLine("- **CPU & Memory**: `${diag.cpuModelAndCores}` • RAM ${diag.ramUsagePercent}% (${diag.availableRamMb} MB free)")
            appendLine("- **Browser Worker Context**: `${_browserState.value.pageTitle}` (`${_browserState.value.url}`)")
            appendLine()
            appendLine("### POSIX / Termux Userland Output")
            appendLine("```text")
            appendLine(shellSnapshot.stdoutAndStderr.take(600))
            appendLine("```")
            appendLine()
            appendLine("## 3. Actionable Deliverable & Recommendations")
            appendLine("1. **Verified Execution**: All tool outputs above were collected from real Android kernel, Accessibility, and POSIX userland probes.")
            appendLine("2. **Human-in-the-Loop (HITL) Gate**: Review and approve this draft to commit it to `${coworker.assignedSpaceName}` on disk.")
        }.trimIndent()

        emitAgUiEvent(
            eventType = "HITL_CHECKPOINT",
            sourceAgent = coworker.dotName,
            summary = "Drafted Space Page '$pageTitle' — Waiting for Human-in-the-Loop (HITL) Approval",
            jsonPayload = JSONObject()
                .put("space", coworker.assignedSpaceName)
                .put("pageTitle", pageTitle)
                .put("status", "PENDING_HITL")
                .toString()
        )

        appendDailyLogEntry("${coworker.dotName} drafted page '$pageTitle' in space '${coworker.assignedSpaceName}' (PENDING_HITL).")

        OpenDotsSpacePageEntity(
            spaceName = coworker.assignedSpaceName,
            pageTitle = pageTitle,
            markdownContent = markdownDraft,
            authoredByDot = coworker.dotName,
            approvalStatus = "PENDING_HITL",
            versionNumber = 1
        )
    }

    /**
     * Commits an approved OpenDots Space Page to disk as a real `.md` file inside `filesDir/opendots_spaces/<space>/`.
     */
    fun writeSpacePageToDisk(page: OpenDotsSpacePageEntity): String {
        val safeSpace = page.spaceName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "General" }
        val safeTitle = page.pageTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifEmpty { "Page_${page.id}" }
        val spaceFolder = File(openDotsSpacesDir, safeSpace).apply { mkdirs() }
        val mdFile = File(spaceFolder, "$safeTitle.md")
        mdFile.writeText(page.markdownContent)
        appendDailyLogEntry("Approved & committed Space Page '${page.pageTitle}' to ${mdFile.absolutePath}")
        emitAgUiEvent(
            eventType = "STATE_DELTA",
            sourceAgent = "OpenDots HITL Gate",
            summary = "APPROVED & committed '${page.pageTitle}' to ${mdFile.name}",
            jsonPayload = JSONObject()
                .put("filePath", mdFile.absolutePath)
                .put("bytes", mdFile.length())
                .put("approvalStatus", "APPROVED")
                .toString()
        )
        return mdFile.absolutePath
    }

    // =========================================================================
    // 4. CLAWHUB SKILLS, MCP JSON-RPC 2.0 BRIDGE & OPENMUSE SELF-IMPROVING SKILLS
    // =========================================================================

    /**
     * Executes a ClawSkillEntity (AnyClaw CLI command, Termux Bridge script, OpenClaw MCP tool, or OpenMuse DOM skill).
     */
    suspend fun executeClawSkill(skill: ClawSkillEntity): String = withContext(Dispatchers.IO) {
        emitAgUiEvent(
            eventType = "TOOL_CALL_START",
            sourceAgent = "ClawHub / ${skill.ecosystemSource}",
            summary = "Executing skill '${skill.title}' (${skill.skillKey})",
            jsonPayload = JSONObject().put("skillKey", skill.skillKey).put("payload", skill.executablePayload).toString()
        )

        val payload = skill.executablePayload.trim()
        val output = when {
            payload.startsWith("MCP_TOOL:") -> {
                val toolName = payload.removePrefix("MCP_TOOL:").trim()
                executeLocalOrRemoteMcpTool(toolName, "{}")
            }
            payload.startsWith("DOM_INSPECT:") -> {
                val snap = _browserState.value
                val domReport = "DOM Harvest from ${snap.url} (${snap.pageTitle}):\n" +
                    snap.interactiveElements.take(10).joinToString("\n") { "- $it" }
                appendFactToMemoryMd("Harvested ${snap.interactiveElements.size} DOM nodes from ${snap.url}")
                domReport
            }
            payload.startsWith("http://") || payload.startsWith("https://") -> {
                executeLocalOrRemoteMcpTool("tools/list", payload)
            }
            else -> {
                val res = termuxBridge.executeCommand(payload, dispatchToTermuxService = true)
                "[${res.executionMode} • ${res.elapsedMs}ms]\n${res.stdoutAndStderr}"
            }
        }

        appendDailyLogEntry("Executed skill '${skill.title}' -> ${output.lineSequence().firstOrNull().orEmpty().take(80)}")
        emitAgUiEvent(
            eventType = "TOOL_CALL_RESULT",
            sourceAgent = "ClawHub / ${skill.ecosystemSource}",
            summary = "Completed '${skill.title}'",
            jsonPayload = JSONObject().put("skillKey", skill.skillKey).put("preview", output.take(160)).toString()
        )
        output
    }

    /**
     * Executes a Model Context Protocol (MCP) JSON-RPC 2.0 request against a remote HTTP MCP server
     * OR executes built-in local Android/OpenClaw MCP tools.
     */
    suspend fun executeLocalOrRemoteMcpTool(
        methodOrToolName: String,
        endpointOrJsonArgs: String
    ): String = withContext(Dispatchers.IO) {
        if (endpointOrJsonArgs.startsWith("http://") || endpointOrJsonArgs.startsWith("https://")) {
            val rpcBody = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", System.currentTimeMillis())
                .put("method", methodOrToolName.ifBlank { "tools/list" })
                .put("params", JSONObject())
                .toString()
            return@withContext try {
                val req = Request.Builder()
                    .url(endpointOrJsonArgs.trim())
                    .post(rpcBody.toRequestBody(jsonMediaType))
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    "MCP JSON-RPC HTTP ${resp.code} from $endpointOrJsonArgs:\n${body.take(400)}"
                }
            } catch (e: Exception) {
                "MCP Remote Server ($endpointOrJsonArgs) unreachable: ${e.localizedMessage}. Falling back to Local Android MCP Server."
            }
        }

        return@withContext when (methodOrToolName.lowercase()) {
            "memory.append_fact" -> {
                val diag = deviceControl.collectDiagnostics()
                val updated = appendFactToMemoryMd(
                    "System checkpoint: RAM ${diag.ramUsagePercent}%, Battery ${diag.batteryPercent}%, Browser=${_browserState.value.url}"
                )
                "Appended system checkpoint to MEMORY.md (${updated.sizeBytes} bytes on disk)."
            }
            "memory.read_all" -> {
                File(openClawWorkspaceDir, "MEMORY.md").readText()
            }
            "android.screen_tree" -> {
                AetherAccessibilityService.readCurrentScreenContent()
            }
            else -> {
                val toolsJson = JSONArray()
                    .put(JSONObject().put("name", "termux.exec").put("description", "Execute POSIX/Termux shell command"))
                    .put(JSONObject().put("name", "browser.inspect_dom").put("description", "Inspect live page DOM via OpenMuse worker"))
                    .put(JSONObject().put("name", "memory.append_fact").put("description", "Append fact to OpenClaw MEMORY.md"))
                    .put(JSONObject().put("name", "spaces.propose_page").put("description", "Create HITL-gated page in OpenDots Space"))
                    .put(JSONObject().put("name", "android.screen_tree").put("description", "Read live Android Accessibility tree"))
                JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("result", JSONObject().put("tools", toolsJson))
                    .toString(2)
            }
        }
    }

    /**
     * Synthesizes a new OpenMuse Self-Improving Skill proposal based on recent user activity.
     */
    fun synthesizeSelfImprovingSkillProposal(recentPromptOrContext: String): ClawSkillEntity {
        val clean = recentPromptOrContext.trim().ifEmpty { "Automated System & Network Health Audit" }
        val slug = "openmuse_skill_${System.currentTimeMillis() % 10000}"
        emitAgUiEvent(
            eventType = "HITL_CHECKPOINT",
            sourceAgent = "OpenMuse Self-Improving Skills",
            summary = "Proposed new skill '$clean' — Awaiting user approval before ingestion",
            jsonPayload = JSONObject().put("skillKey", slug).put("approved", false).toString()
        )
        return ClawSkillEntity(
            skillKey = slug,
            title = "Self-Improved Skill: ${clean.take(42)}",
            ecosystemSource = "OPENMUSE_SELF_IMPROVED",
            description = "Synthesized by OpenMuse Post-Execution Intelligence. Requires manual user approval before becoming active in agent workflows.",
            executablePayload = "uname -a && getprop ro.build.version.release && df -h | head -n 5",
            isApproved = false,
            usageCount = 0,
            lastResultLog = "Pending User Approval Gate"
        )
    }

    // =========================================================================
    // 5. OPENCLAW MULTI-CHANNEL GATEWAY DISPATCHER (TELEGRAM, DISCORD, SLACK, AG-UI)
    // =========================================================================

    suspend fun dispatchGatewayChannelMessage(
        channel: ClawGatewayChannelEntity,
        messageText: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanMsg = messageText.trim().ifEmpty {
            "AetherOS + OpenClaw Gateway ping at ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}"
        }
        val url = channel.endpointOrBotUrl.trim()

        emitAgUiEvent(
            eventType = "TOOL_CALL_START",
            sourceAgent = "OpenClaw Gateway (${channel.channelType})",
            summary = "Routing message to ${channel.channelName}",
            jsonPayload = JSONObject().put("channel", channel.channelName).put("text", cleanMsg).toString()
        )

        if (channel.channelType == "WEBCHAT_AG_UI" || url.startsWith("http://127.0.0.1")) {
            emitAgUiEvent(
                eventType = "TEXT_MESSAGE_CONTENT",
                sourceAgent = channel.channelName,
                summary = cleanMsg,
                jsonPayload = JSONObject().put("channel", "WEBCHAT_AG_UI").put("message", cleanMsg).toString()
            )
            appendDailyLogEntry("Routed message via ${channel.channelName}: $cleanMsg")
            return@withContext true to "Streamed to local AG-UI / WebChat Gateway: \"$cleanMsg\""
        }

        if (url.contains("<TOKEN>") || url.contains("<ID>") || url.contains("<T00>")) {
            appendDailyLogEntry("Prepared ${channel.channelType} payload for ${channel.channelName} (Configure live webhook URL to send externally).")
            return@withContext true to "Prepared ${channel.channelType} JSON payload for '${channel.channelName}'. Replace placeholder URL with your live webhook/bot token URL to dispatch over the internet."
        }

        return@withContext try {
            val payloadJson = when (channel.channelType.uppercase()) {
                "DISCORD" -> JSONObject().put("content", cleanMsg).toString()
                "SLACK" -> JSONObject().put("text", cleanMsg).toString()
                "TELEGRAM" -> {
                    val token = E2EEVault.decrypt(channel.encryptedToken)
                    JSONObject().put("text", cleanMsg).put("chat_id", token.ifBlank { "@channel" }).toString()
                }
                else -> JSONObject().put("message", cleanMsg).toString()
            }
            val req = Request.Builder()
                .url(url)
                .post(payloadJson.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val ok = resp.isSuccessful
                val statusMsg = "HTTP ${resp.code} from ${channel.channelName}"
                appendDailyLogEntry("Dispatched to ${channel.channelName}: $statusMsg")
                ok to statusMsg
            }
        } catch (e: Exception) {
            false to "Gateway HTTP error (${channel.channelName}): ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }
}
