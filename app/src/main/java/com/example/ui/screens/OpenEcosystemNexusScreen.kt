package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenEcosystemNexusScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    val agUiEvents by viewModel.agUiEvents.collectAsStateWithLifecycle()
    val browserState by viewModel.browserState.collectAsStateWithLifecycle()
    val coworkers by viewModel.openDotsCoworkers.collectAsStateWithLifecycle()
    val spacePages by viewModel.openDotsSpacePages.collectAsStateWithLifecycle()
    val clawSkills by viewModel.clawSkills.collectAsStateWithLifecycle()
    val gatewayChannels by viewModel.clawGatewayChannels.collectAsStateWithLifecycle()
    val workspaceFiles by viewModel.workspaceFiles.collectAsStateWithLifecycle()
    val termuxEnv by viewModel.termuxEnvironment.collectAsStateWithLifecycle()
    val lastTermuxOutcome by viewModel.lastTermuxOutcome.collectAsStateWithLifecycle()
    val lastMcpResponse by viewModel.lastMcpResponse.collectAsStateWithLifecycle()

    val pendingHitlPagesCount = spacePages.count { it.approvalStatus == "PENDING_HITL" }
    val pendingSkillsCount = clawSkills.count { !it.isApproved }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner & Sub-Ecosystem Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OPENMUSE • ANYCLAW • OPENDOTS • TERMUX NEXUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Unified Open Agent Runtime & Linux Bridge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshTermuxAndWorkspaceState() },
                            modifier = Modifier.testTag("nexus_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Ecosystem Telemetry",
                                tint = QuantumEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Real AG-UI Streaming • Browser Worker with Manual Takeover • Specialist Dots & Spaces HITL Gates • Persistent MEMORY.md/SOUL.md • ClawHub & MCP • Termux RunCommandService & POSIX Userland",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf(
                            "1. OpenMuse (AG-UI & Browser)",
                            "2. OpenDots (Dots & HITL${if (pendingHitlPagesCount > 0) " [$pendingHitlPagesCount]" else ""})",
                            "3. AnyClaw & OpenClaw (Memory & MCP${if (pendingSkillsCount > 0) " [$pendingSkillsCount]" else ""})",
                            "4. Termux & Sub-Dependencies"
                        )
                        tabs.forEachIndexed { idx, label ->
                            FilterChip(
                                selected = selectedSection == idx,
                                onClick = { selectedSection = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("nexus_subtab_$idx")
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SECTION 0: OPENMUSE (AG-UI STREAM, PERSISTENT BROWSER WORKER & MANUAL TAKEOVER)
        // =====================================================================
        if (selectedSection == 0) {
            item {
                OpenMuseBrowserAndAgUiSection(
                    viewModel = viewModel,
                    browserState = browserState,
                    agUiEvents = agUiEvents
                )
            }
        }

        // =====================================================================
        // SECTION 1: OPENDOTS (SPECIALIST "DOTS" COWORKERS, SPACES & PAGES + HITL GATES)
        // =====================================================================
        if (selectedSection == 1) {
            item {
                OpenDotsCoworkersAndSpacesSection(
                    viewModel = viewModel,
                    coworkers = coworkers,
                    spacePages = spacePages
                )
            }
        }

        // =====================================================================
        // SECTION 2: ANYCLAW & OPENCLAW (MEMORY.MD / SOUL.MD, CLAWHUB SKILLS, MCP & GATEWAY)
        // =====================================================================
        if (selectedSection == 2) {
            item {
                AnyClawAndOpenClawSection(
                    viewModel = viewModel,
                    workspaceFiles = workspaceFiles,
                    clawSkills = clawSkills,
                    gatewayChannels = gatewayChannels,
                    lastMcpResponse = lastMcpResponse
                )
            }
        }

        // =====================================================================
        // SECTION 3: TERMUX & SUB-DEPENDENCIES (RUN_COMMAND BRIDGE, BINARIES & SCRIPTS)
        // =====================================================================
        if (selectedSection == 3) {
            item {
                TermuxAndSubDependenciesSection(
                    viewModel = viewModel,
                    termuxEnv = termuxEnv,
                    lastTermuxOutcome = lastTermuxOutcome
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenMuseBrowserAndAgUiSection(
    viewModel: AetherViewModel,
    browserState: com.example.domain.BrowserDomInspectionResult,
    agUiEvents: List<com.example.domain.AgUiProtocolEvent>
) {
    var urlInput by remember(browserState.url) { mutableStateOf(browserState.url) }
    var showLiveWebView by rememberSaveable { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var jsCommandInput by remember { mutableStateOf("document.title + ' | Links: ' + document.links.length") }
    var jsEvalResult by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OPENMUSE PERSISTENT BROWSER WORKER",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Live Chromium WebView + Manual Takeover ('Take Control')",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Manual Takeover Toggle ("Take Control" from OpenMuse spec)
                Surface(
                    color = if (browserState.isManualTakeoverActive) AmberAlert.copy(alpha = 0.16f) else ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (browserState.isManualTakeoverActive) {
                                    "MANUAL TAKEOVER ('TAKE CONTROL') ACTIVE"
                                } else {
                                    "AUTONOMOUS AGENT BROWSER MODE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (browserState.isManualTakeoverActive) AmberAlert else QuantumEmerald
                            )
                            Text(
                                text = if (browserState.isManualTakeoverActive) {
                                    "You have physical touch & keyboard control of the embedded browser session."
                                } else {
                                    "OpenMuse Worker inspects DOM nodes & executes JS automation selectors."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = browserState.isManualTakeoverActive,
                            onCheckedChange = { viewModel.toggleBrowserManualTakeover(it) },
                            modifier = Modifier.testTag("openmuse_manual_takeover_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick target URLs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickUrls = listOf(
                        "OpenMuse" to "https://github.com/CopilotKit/OpenMuse",
                        "OpenDots" to "https://github.com/CopilotKit/OpenDots",
                        "OpenClaw" to "https://github.com/openclaw/openclaw",
                        "AnyClaw" to "https://github.com/fastclaw-ai/anyclaw",
                        "Termux" to "https://github.com/termux/termux-app"
                    )
                    quickUrls.forEach { (label, targetUrl) ->
                        AssistChip(
                            onClick = {
                                urlInput = targetUrl
                                viewModel.inspectUrlWithOpenMuseWorker(targetUrl)
                                webViewRef?.loadUrl(targetUrl)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Browser Worker Target URL") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("openmuse_url_input")
                    )
                    Button(
                        onClick = {
                            viewModel.inspectUrlWithOpenMuseWorker(urlInput)
                            webViewRef?.loadUrl(
                                if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                            )
                        },
                        modifier = Modifier.testTag("openmuse_inspect_url_button")
                    ) {
                        Text("Load & Inspect")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${browserState.pageTitle} (HTTP ${browserState.statusCode} • ${browserState.inspectedAtLabel})",
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantumEmerald
                    )
                    AssistChip(
                        onClick = { showLiveWebView = !showLiveWebView },
                        label = { Text(if (showLiveWebView) "Hide Live WebView" else "Show Live WebView") }
                    )
                }

                if (showLiveWebView) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.5.dp,
                                color = if (browserState.isManualTakeoverActive) AmberAlert else CyberCyan,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadsImagesAutomatically = true
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val extractScript = """
                                                (function() {
                                                    var items = [];
                                                    var btns = document.querySelectorAll('button, input[type="submit"], a');
                                                    for (var i = 0; i < Math.min(btns.length, 12); i++) {
                                                        var el = btns[i];
                                                        var txt = (el.innerText || el.value || el.getAttribute('aria-label') || '').trim();
                                                        if (txt.length > 1) {
                                                            items.push('[' + el.tagName + '] ' + txt.substring(0, 50));
                                                        }
                                                    }
                                                    return JSON.stringify({
                                                        summary: 'Live WebView DOM: ' + document.title + ' (' + btns.length + ' interactive nodes)',
                                                        elements: items
                                                    });
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(extractScript) { rawJson ->
                                                val unescaped = rawJson?.trim('"')
                                                    ?.replace("\\\"", "\"")
                                                    ?.replace("\\\\", "\\")
                                                    .orEmpty()
                                                if (unescaped.startsWith("{")) {
                                                    viewModel.onWebViewDomExtracted(
                                                        url = url.orEmpty(),
                                                        title = view.title.orEmpty(),
                                                        domElementsJson = unescaped
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    loadUrl(browserState.url)
                                    webViewRef = this
                                }
                            },
                            update = { wv ->
                                webViewRef = wv
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live JavaScript DOM Executor inside Browser Worker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = jsCommandInput,
                        onValueChange = { jsCommandInput = it },
                        label = { Text("Browser Worker JS Selector / Action") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            webViewRef?.evaluateJavascript(jsCommandInput) { result ->
                                jsEvalResult = "JS Result: ${result ?: "null"}"
                            }
                        }
                    ) {
                        Text("Run JS")
                    }
                }

                if (jsEvalResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = jsEvalResult,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = browserState.headingsAndSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    browserState.interactiveElements.take(8).forEach { elem ->
                        Surface(
                            color = ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = elem,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // AG-UI Protocol Event Stream Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeuralViolet.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = NeuralViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AG-UI (AGENT-USER INTERACTION) EVENT STREAM",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuralViolet
                            )
                            Text(
                                text = "Standardized Streaming Events & HITL Checkpoints (${agUiEvents.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                agUiEvents.take(8).forEach { ev ->
                    val badgeColor = when (ev.eventType) {
                        "HITL_CHECKPOINT" -> AmberAlert
                        "TOOL_CALL_START", "TOOL_CALL_RESULT" -> CyberCyan
                        "STATE_DELTA" -> QuantumEmerald
                        else -> NeuralViolet
                    }
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "[${ev.eventType}] • ${ev.sourceAgent}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                                Text(
                                    text = ev.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ev.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = ev.jsonPayload,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenDotsCoworkersAndSpacesSection(
    viewModel: AetherViewModel,
    coworkers: List<com.example.data.local.OpenDotsCoworkerEntity>,
    spacePages: List<com.example.data.local.OpenDotsSpacePageEntity>
) {
    var dotTaskPrompt by remember {
        mutableStateOf("Inspect system health, browser DOM, and Termux environment and draft a technical runbook page.")
    }
    var showCreateDotForm by remember { mutableStateOf(false) }
    var newDotName by remember { mutableStateOf("") }
    var newDotRole by remember { mutableStateOf("") }
    var newDotInstructions by remember { mutableStateOf("") }
    var newDotSpace by remember { mutableStateOf("Engineering & Research") }

    var editingPageId by remember { mutableStateOf<Int?>(null) }
    var editingMarkdown by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Specialist AI Coworkers ("Dots") Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GroupWork,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OPENDOTS SPECIALIST AI COWORKERS ('DOTS')",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Named Persistent Agents with Sandboxes & Bounded Tools",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showCreateDotForm = !showCreateDotForm },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showCreateDotForm) "Close" else "New Dot", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (showCreateDotForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newDotName,
                                onValueChange = { newDotName = it },
                                label = { Text("Dot Coworker Name (e.g. Nova • API Architect Dot)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDotRole,
                                onValueChange = { newDotRole = it },
                                label = { Text("Specialist Role Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDotSpace,
                                onValueChange = { newDotSpace = it },
                                label = { Text("Assigned OpenDots Space") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDotInstructions,
                                onValueChange = { newDotInstructions = it },
                                label = { Text("System Instructions & Tool Boundaries") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.createOpenDotsCoworker(
                                        dotName = newDotName,
                                        roleTitle = newDotRole,
                                        instructions = newDotInstructions,
                                        sandboxEnv = "OpenBot Chromium + Termux POSIX Sandbox",
                                        allowedToolsCsv = "BROWSER_DOM,TERMUX_SHELL,SPACES_WRITE,HITL_GATE",
                                        assignedSpace = newDotSpace
                                    )
                                    newDotName = ""
                                    newDotRole = ""
                                    newDotInstructions = ""
                                    showCreateDotForm = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Deploy Specialist Dot Coworker")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dotTaskPrompt,
                    onValueChange = { dotTaskPrompt = it },
                    label = { Text("Workspace Task Assignment for Selected Dot") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opendots_task_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                coworkers.forEach { dot ->
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dot.dotName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "${dot.roleTitle} • Space: ${dot.assignedSpaceName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QuantumEmerald
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.runOpenDotsCoworkerTask(dot, dotTaskPrompt) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("run_dot_${dot.id}")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run Dot", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteOpenDotsCoworker(dot.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Dot",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sandbox: ${dot.sandboxEnvironment} • Tools: ${dot.allowedToolsCsv}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Status: [${dot.status}] • Pages Authored: ${dot.completedPagesCount} • ${dot.lastOutputSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dot.status == "AWAITING_HITL") AmberAlert else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. OpenDots Spaces & Pages with Human-in-the-Loop (HITL) Approval Gates
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QuantumEmerald.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = QuantumEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "OPENDOTS SPACES & PAGES + HUMAN-IN-THE-LOOP (HITL) GATES",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                        Text(
                            text = "Collaborative Markdown Workspace with Approval Checkpoints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                spacePages.forEach { page ->
                    val isPending = page.approvalStatus == "PENDING_HITL"
                    val statusColor = when (page.approvalStatus) {
                        "APPROVED" -> QuantumEmerald
                        "PENDING_HITL" -> AmberAlert
                        else -> CrimsonError
                    }
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(
                                width = 1.dp,
                                color = statusColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "[${page.spaceName}] ${page.pageTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "By ${page.authoredByDot} • v${page.versionNumber} • Gate: ${page.approvalStatus}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteOpenDotsSpacePage(page.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Page",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (page.filePathOnDisk.isNotEmpty()) {
                                Text(
                                    text = "On-Disk File: ${page.filePathOnDisk}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (editingPageId == page.id) {
                                OutlinedTextField(
                                    value = editingMarkdown,
                                    onValueChange = { editingMarkdown = it },
                                    label = { Text("Edit Space Page Markdown Before Committing") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.approveOpenDotsSpacePage(page, editingMarkdown)
                                            editingPageId = null
                                        }
                                    ) {
                                        Text("Save & Approve to Disk")
                                    }
                                    OutlinedButton(onClick = { editingPageId = null }) {
                                        Text("Cancel")
                                    }
                                }
                            } else {
                                Text(
                                    text = page.markdownContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isPending) {
                                        Button(
                                            onClick = { viewModel.approveOpenDotsSpacePage(page) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = QuantumEmerald,
                                                contentColor = Color(0xFF002114)
                                            ),
                                            modifier = Modifier.testTag("hitl_approve_page_${page.id}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("HITL Approve & Save .md", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.rejectOpenDotsSpacePage(page) }
                                        ) {
                                            Text("Reject", style = MaterialTheme.typography.labelSmall, color = CrimsonError)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            editingPageId = page.id
                                            editingMarkdown = page.markdownContent
                                        }
                                    ) {
                                        Text("Edit Markdown", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnyClawAndOpenClawSection(
    viewModel: AetherViewModel,
    workspaceFiles: List<com.example.domain.WorkspaceMarkdownFile>,
    clawSkills: List<com.example.data.local.ClawSkillEntity>,
    gatewayChannels: List<com.example.data.local.ClawGatewayChannelEntity>,
    lastMcpResponse: String?
) {
    var selectedFileIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentFile = workspaceFiles.getOrNull(selectedFileIndex) ?: workspaceFiles.firstOrNull()
    var fileEditorContent by remember(currentFile?.fileName, currentFile?.content) {
        mutableStateOf(currentFile?.content.orEmpty())
    }
    var quickFactInput by remember { mutableStateOf("") }

    var mcpMethodInput by remember { mutableStateOf("tools/list") }
    var mcpEndpointOrArgs by remember { mutableStateOf("{}") }

    var gatewayTestMsg by remember { mutableStateOf("AetherOS + OpenClaw Autonomous Status: All systems nominal.") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. OpenClaw / AnyClaw Persistent On-Disk Workspace (MEMORY.md, SOUL.md, TOOLS.md, YYYY-MM-DD.md)
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "OPENCLAW & ANYCLAW ON-DISK WORKSPACE FILES",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Text(
                            text = "Persistent MEMORY.md, SOUL.md, TOOLS.md & Daily Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workspaceFiles.forEachIndexed { idx, wf ->
                        FilterChip(
                            selected = selectedFileIndex == idx,
                            onClick = { selectedFileIndex = idx },
                            label = {
                                Text("${wf.fileName} (${wf.sizeBytes}B)", style = MaterialTheme.typography.labelSmall)
                            }
                        )
                    }
                }

                if (currentFile != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Disk Path: ${currentFile.absolutePath} • Updated: ${currentFile.lastUpdatedLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = QuantumEmerald
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fileEditorContent,
                        onValueChange = { fileEditorContent = it },
                        label = { Text("Editing ${currentFile.fileName} (Real On-Device File)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openclaw_workspace_editor")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.saveWorkspaceMarkdown(currentFile.fileName, fileEditorContent) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_workspace_file_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save ${currentFile.fileName} to Disk")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickFactInput,
                        onValueChange = { quickFactInput = it },
                        label = { Text("Quick Append Durable Fact to MEMORY.md") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("memory_md_fact_input")
                    )
                    Button(
                        onClick = {
                            viewModel.appendFactToMemoryMd(quickFactInput)
                            quickFactInput = ""
                        },
                        modifier = Modifier.testTag("append_memory_md_button")
                    ) {
                        Text("Append")
                    }
                }
            }
        }

        // 2. ClawHub Skills & OpenMuse Self-Improving Skills Registry
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeuralViolet.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeuralViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CLAWHUB SKILLS & OPENMUSE SELF-IMPROVING SKILLS",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuralViolet
                            )
                            Text(
                                text = "Modular AnyClaw CLI, Termux & Self-Synthesized Skills",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.proposeSelfImprovingSkill("Automated Kernel, Memory & Active App Snapshot")
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Synthesize Skill", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                clawSkills.forEach { skill ->
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                width = 1.dp,
                                color = if (!skill.isApproved) AmberAlert else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skill.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!skill.isApproved) AmberAlert else CyberCyan
                                    )
                                    Text(
                                        text = "[${skill.ecosystemSource}] • Key: ${skill.skillKey} • Runs: ${skill.usageCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QuantumEmerald
                                    )
                                }
                                if (!skill.isApproved) {
                                    Button(
                                        onClick = { viewModel.approveClawSkill(skill) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberAlert,
                                            contentColor = Color(0xFF1F1400)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Approve Skill", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.runClawSkill(skill) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("run_claw_skill_${skill.skillKey}")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Execute", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = skill.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Payload: ${skill.executablePayload}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            if (skill.lastResultLog != "Idle") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = skill.lastResultLog,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = QuantumEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Model Context Protocol (MCP) JSON-RPC 2.0 Bridge & Multi-Channel Gateway
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = QuantumEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MODEL CONTEXT PROTOCOL (MCP) & MULTI-CHANNEL GATEWAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                        Text(
                            text = "JSON-RPC 2.0 Tool Discovery + Telegram/Discord/Slack/AG-UI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("tools/list", "memory.append_fact", "memory.read_all", "android.screen_tree").forEach { m ->
                        AssistChip(
                            onClick = {
                                mcpMethodInput = m
                                viewModel.invokeMcpToolOrServer(m, mcpEndpointOrArgs)
                            },
                            label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = mcpMethodInput,
                        onValueChange = { mcpMethodInput = it },
                        label = { Text("MCP Method / Tool") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.invokeMcpToolOrServer(mcpMethodInput, mcpEndpointOrArgs) }
                    ) {
                        Text("Call MCP")
                    }
                }

                if (!lastMcpResponse.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastMcpResponse,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gatewayTestMsg,
                    onValueChange = { gatewayTestMsg = it },
                    label = { Text("OpenClaw Multi-Channel Broadcast Payload") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                gatewayChannels.forEach { ch ->
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[${ch.channelType}] ${ch.channelName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "Routed: ${ch.messagesRoutedCount} • ${ch.lastEventStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = { viewModel.sendGatewayChannelTestMessage(ch, gatewayTestMsg) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dispatch", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TermuxAndSubDependenciesSection(
    viewModel: AetherViewModel,
    termuxEnv: com.example.service.TermuxEnvironmentSnapshot,
    lastTermuxOutcome: com.example.service.TermuxExecutionOutcome?
) {
    var termuxCmdInput by remember { mutableStateOf("uname -a && id && ls -la") }
    var scriptFileName by remember { mutableStateOf("custom_agent_job.sh") }
    var scriptBody by remember {
        mutableStateOf(
            """
            #!/system/bin/sh
            echo "Running custom Termux + AnyClaw script in ${'$'}HOME"
            uname -a
            df -h | head -n 5
            """.trimIndent()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Termux Core & 6 Sub-Dependencies Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QuantumEmerald.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = QuantumEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TERMUX & SUB-DEPENDENCIES ECOSYSTEM",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald
                            )
                            Text(
                                text = "RunCommandService Bridge, Plugins & POSIX Userland",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.launchTermuxOrAppSettings() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (termuxEnv.isTermuxAppInstalled) "Open Termux" else "Termux Perms",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "com.termux.permission.RUN_COMMAND: ${if (termuxEnv.isRunCommandPermissionGranted) "GRANTED" else "Configure in App Permissions"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (termuxEnv.isRunCommandPermissionGranted) QuantumEmerald else AmberAlert
                        )
                        Text(
                            text = "Local POSIX Userland Home: ${termuxEnv.localUserlandHomePath}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                        Text(
                            text = "Termux Prefix: ${termuxEnv.termuxPrefixPath}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Termux Sub-Dependencies & Add-ons (7 Packages):",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                termuxEnv.plugins.forEach { plugin ->
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${plugin.title} (${plugin.packageName})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (plugin.isInstalled) QuantumEmerald else TextPrimary
                                )
                                Text(
                                    text = plugin.roleDescription,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = plugin.versionName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (plugin.isInstalled) QuantumEmerald else CyberCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "One-Tap Termux Bootstrap & External Apps Config (Copies to Clipboard):",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.copyTermuxBootstrapToClipboard("ALLOW_EXTERNAL") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("1. Enable allow-external-apps=true", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = { viewModel.copyTermuxBootstrapToClipboard("CORE_PACKAGES") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("2. pkg install git nodejs python clang ffmpeg", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = { viewModel.copyTermuxBootstrapToClipboard("ANYCLAW_OPENCLAW") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("3. Install OpenClaw + AnyClaw + MCP SDK", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = { viewModel.copyTermuxBootstrapToClipboard("OPENMUSE_OPENDOTS") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("4. Clone OpenMuse & OpenDots", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Live Binary Toolchain Probe (${termuxEnv.binaryProbes.count { it.isAvailable }}/${termuxEnv.binaryProbes.size} available):",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    termuxEnv.binaryProbes.forEach { probe ->
                        Surface(
                            color = if (probe.isAvailable) QuantumEmerald.copy(alpha = 0.16f) else ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${if (probe.isAvailable) "✓" else "○"} ${probe.binaryName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (probe.isAvailable) QuantumEmerald else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Real Termux RunCommandService & Local POSIX Userland Console
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TERMUX RUN_COMMAND SERVICE + LOCAL POSIX EXECUTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
                Text(
                    text = "Execute Commands & Persistent Userland Scripts (.sh / .py / .js)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = termuxCmdInput,
                        onValueChange = { termuxCmdInput = it },
                        label = { Text("Termux / POSIX Command") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("termux_bridge_cmd_input")
                    )
                    Button(
                        onClick = { viewModel.executeTermuxBridgeCommand(termuxCmdInput, true) },
                        modifier = Modifier.testTag("termux_bridge_exec_button")
                    ) {
                        Text("Run")
                    }
                }

                if (lastTermuxOutcome != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "[${lastTermuxOutcome.executionMode}] exit=${lastTermuxOutcome.exitCode} (${lastTermuxOutcome.elapsedMs}ms)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastTermuxOutcome.stdoutAndStderr,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = QuantumEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = scriptFileName,
                    onValueChange = { scriptFileName = it },
                    label = { Text("Userland Script Filename (e.g. audit_node.sh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = scriptBody,
                    onValueChange = { scriptBody = it },
                    label = { Text("Executable Script Body") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.saveAndExecuteTermuxScript(scriptFileName, scriptBody, true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_and_run_termux_script_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save chmod +x & Execute Script in Userland")
                }

                Spacer(modifier = Modifier.height(10.dp))

                termuxEnv.savedScripts.forEach { script ->
                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${script.fileName} (${script.sizeBytes}B • ${script.interpreter})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = script.absolutePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.executeTermuxBridgeCommand("sh \"${script.absolutePath}\"", true)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Run", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteTermuxScript(script.fileName) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Script",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
