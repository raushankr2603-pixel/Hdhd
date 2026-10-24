package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AgentComputerEntity
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuseWorkspaceScreen(
    viewModel: AetherViewModel,
    onNavigateToTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val agentComputers by viewModel.agentComputers.collectAsStateWithLifecycle()
    val computerFiles by viewModel.selectedComputerFiles.collectAsStateWithLifecycle()
    val lastReceipt by viewModel.lastAgentTerminalReceipt.collectAsStateWithLifecycle()
    val agUiEvents by viewModel.agUiEvents.collectAsStateWithLifecycle()
    val spacePages by viewModel.openDotsSpacePages.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    var selectedComputerIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeComputer: AgentComputerEntity? = agentComputers.getOrNull(selectedComputerIndex)
        ?: agentComputers.firstOrNull()

    LaunchedEffect(activeComputer?.id) {
        if (activeComputer != null) {
            viewModel.refreshComputerFiles(activeComputer)
        }
    }

    // Viewport mode: 0 = Browser Viewport, 1 = Terminal TTY, 2 = Files & Artifacts, 3 = Cloud VM Config
    var viewportMode by rememberSaveable { mutableIntStateOf(0) }
    var showSpawnAgentModal by rememberSaveable { mutableStateOf(false) }

    var newAgentName by remember { mutableStateOf("") }
    var newAgentRole by remember { mutableStateOf("Autonomous Full-Stack & Cloud Researcher") }
    var newAgentBackend by remember { mutableStateOf("ISOLATED_LINUX_CONTAINER") }
    var newAgentRemoteUrl by remember { mutableStateOf("") }
    var newAgentApiKey by remember { mutableStateOf("") }
    var newAgentBrowserUrl by remember { mutableStateOf("https://github.com/CopilotKit/OpenMuse") }

    var goalComposerInput by remember {
        mutableStateOf("Inspect https://github.com/CopilotKit/OpenMuse DOM, run POSIX kernel & memory audit in VM, and generate a deliverable markdown artifact.")
    }

    val pendingHitlPages = spacePages.filter { it.approvalStatus == "PENDING_HITL" }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. MUSE AGENT COMPUTERS BAR (GrokBot / MetaMuse Per-Agent Cloud Computers)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(CyberCyan, NeuralViolet, QuantumEmerald)),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(CyberCyan, NeuralViolet))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = null,
                                        tint = Color(0xFF040914),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "MUSE AGENT CLOUD COMPUTERS (${agentComputers.size} ACTIVE VMs)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "Every Agent Runs Its Own Isolated Computer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Button(
                                onClick = { showSpawnAgentModal = !showSpawnAgentModal },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QuantumEmerald,
                                    contentColor = Color(0xFF002114)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("spawn_agent_computer_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showSpawnAgentModal) "Close" else "+ Agent VM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showSpawnAgentModal) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = ObsidianSurfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Provision New Agent with Dedicated Cloud / Sandbox Computer",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    OutlinedTextField(
                                        value = newAgentName,
                                        onValueChange = { newAgentName = it },
                                        label = { Text("Agent Name (e.g. Hyperion • Security & Cloud Agent)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("new_agent_vm_name_input")
                                    )
                                    OutlinedTextField(
                                        value = newAgentRole,
                                        onValueChange = { newAgentRole = it },
                                        label = { Text("Agent Specialist Role & Directives") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "ISOLATED_LINUX_CONTAINER",
                                            "TERMUX_USERLAND_VM",
                                            "CLOUD_VM_BRIDGE"
                                        ).forEach { backend ->
                                            FilterChip(
                                                selected = newAgentBackend == backend,
                                                onClick = { newAgentBackend = backend },
                                                label = { Text(backend, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = newAgentRemoteUrl,
                                        onValueChange = { newAgentRemoteUrl = it },
                                        label = { Text("Optional Remote Cloud Container URL (Docker/E2B/Daytona HTTP)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = newAgentBrowserUrl,
                                        onValueChange = { newAgentBrowserUrl = it },
                                        label = { Text("Initial Browser Worker URL") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.spawnAgentWithCloudComputer(
                                                agentName = newAgentName,
                                                personaRole = newAgentRole,
                                                computeBackend = newAgentBackend,
                                                remoteCloudEndpoint = newAgentRemoteUrl,
                                                rawCloudApiKey = newAgentApiKey,
                                                initialBrowserUrl = newAgentBrowserUrl
                                            )
                                            newAgentName = ""
                                            showSpawnAgentModal = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("confirm_spawn_agent_vm_button")
                                    ) {
                                        Text("Provision Dedicated Agent Computer & Mount /workspace")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Agent Computer Switcher Cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            agentComputers.forEachIndexed { idx, comp ->
                                val isSelected = (activeComputer?.id == comp.id)
                                val dotColor = when (comp.status) {
                                    "EXECUTING" -> CyberCyan
                                    "MANUAL_TAKEOVER" -> AmberAlert
                                    else -> QuantumEmerald
                                }
                                Surface(
                                    color = if (isSelected) CyberCyan.copy(alpha = 0.16f) else ObsidianSurfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .width(225.dp)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) CyberCyan else BorderSubtle,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedComputerIndex = idx
                                            viewModel.refreshComputerFiles(comp)
                                        }
                                        .testTag("select_agent_vm_${comp.vmHostname}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = comp.vmHostname,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberCyan
                                                )
                                            }
                                            Text(
                                                text = "CPU ${comp.cpuLoadPercent}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QuantumEmerald
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comp.agentName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${comp.computeBackend} • ${comp.artifactsCount} artifacts",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. THE MUSE LIVE AGENT COMPUTER VIEWPORT WINDOW
            if (activeComputer != null) {
                item {
                    MuseAgentComputerViewportCard(
                        viewModel = viewModel,
                        computer = activeComputer,
                        computerFiles = computerFiles,
                        lastReceipt = lastReceipt,
                        viewportMode = viewportMode,
                        onSelectViewportMode = { viewportMode = it }
                    )
                }

                // 3. AUTONOMOUS TASK PLAN & INLINE GENERATIVE AG-UI STREAM CARDS
                item {
                    MuseTaskPlanAndGenerativeFeedCard(
                        viewModel = viewModel,
                        computer = activeComputer,
                        computerFiles = computerFiles,
                        pendingHitlPages = pendingHitlPages,
                        agUiEvents = agUiEvents,
                        onOpenFilesTab = { viewportMode = 2 }
                    )
                }
            }
        }

        // 4. DOCKED MUSE CONVERSATIONAL & GOAL COMPOSER BAR
        if (activeComputer != null) {
            Surface(
                color = ObsidianSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickGoals = listOf(
                            "Audit Kernel, RAM & Mounts in VM",
                            "Harvest OpenMuse DOM & Write Artifact",
                            "Verify Termux & OpenClaw Workspace",
                            "Generate Security & Permission Report"
                        )
                        quickGoals.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    goalComposerInput = preset
                                    viewModel.runAutonomousGoalOnAgentComputer(activeComputer, preset)
                                },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onNavigateToTab(1) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceState.isListening) CrimsonError.copy(alpha = 0.25f)
                                    else NeuralViolet.copy(alpha = 0.22f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Live Voice Orb",
                                tint = if (voiceState.isListening) CrimsonError else CyberCyan
                            )
                        }

                        OutlinedTextField(
                            value = goalComposerInput,
                            onValueChange = { goalComposerInput = it },
                            placeholder = {
                                Text("Assign autonomous goal to ${activeComputer.vmHostname}...")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("muse_goal_composer_input")
                        )

                        Button(
                            onClick = {
                                viewModel.runAutonomousGoalOnAgentComputer(activeComputer, goalComposerInput)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            ),
                            modifier = Modifier.testTag("muse_run_goal_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Execute", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuseAgentComputerViewportCard(
    viewModel: AetherViewModel,
    computer: AgentComputerEntity,
    computerFiles: List<com.example.domain.AgentComputerFileItem>,
    lastReceipt: com.example.domain.AgentTerminalReceipt?,
    viewportMode: Int,
    onSelectViewportMode: (Int) -> Unit
) {
    val browserDomState by viewModel.browserState.collectAsStateWithLifecycle()
    var browserUrlDraft by remember(computer.id, computer.activeBrowserUrl) {
        mutableStateOf(computer.activeBrowserUrl)
    }
    var manualDomInputDraft by remember(computer.id) {
        mutableStateOf("")
    }

    var terminalCmdDraft by remember(computer.id) {
        mutableStateOf("pwd && uname -a && ls -la")
    }

    var selectedFileIndex by remember(computer.id, computerFiles.size) { mutableIntStateOf(0) }
    val activeFile = computerFiles.getOrNull(selectedFileIndex) ?: computerFiles.firstOrNull()
    var fileEditorName by remember(activeFile?.relativePath) {
        mutableStateOf(activeFile?.fileName ?: "agent_note.md")
    }
    var fileEditorCategory by remember(activeFile?.folderCategory) {
        mutableStateOf(activeFile?.folderCategory ?: "artifacts")
    }
    var fileEditorContent by remember(activeFile?.relativePath, activeFile?.previewContent) {
        mutableStateOf(activeFile?.previewContent.orEmpty())
    }

    var cfgBackend by remember(computer.id, computer.computeBackend) {
        mutableStateOf(computer.computeBackend)
    }
    var cfgRemoteEndpoint by remember(computer.id, computer.remoteCloudEndpoint) {
        mutableStateOf(computer.remoteCloudEndpoint)
    }
    var cfgCloudKey by remember(computer.id) { mutableStateOf("") }
    var cfgNetMode by remember(computer.id, computer.networkIsolationMode) {
        mutableStateOf(computer.networkIsolationMode)
    }
    var cfgTimeout by remember(computer.id, computer.executionTimeoutSec) {
        mutableStateOf(computer.executionTimeoutSec.toString())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = if (computer.isManualTakeover) AmberAlert else CyberCyan.copy(alpha = 0.65f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Muse Computer Monitor Chrome Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Window dots
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CrimsonError))
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AmberAlert))
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(QuantumEmerald))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${computer.vmHostname} • ${computer.agentName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${computer.computeBackend} • RAM ${computer.memoryUsedMb}MB • Timeout ${computer.executionTimeoutSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                    }
                }

                // Signature Muse "Take Control" Manual Takeover Button
                Button(
                    onClick = {
                        viewModel.toggleAgentComputerManualTakeover(computer, !computer.isManualTakeover)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (computer.isManualTakeover) AmberAlert else ObsidianSurfaceVariant,
                        contentColor = if (computer.isManualTakeover) Color(0xFF1F1200) else CyberCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("muse_take_control_button")
                ) {
                    Icon(Icons.Default.PanTool, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (computer.isManualTakeover) "Release Control" else "Take Control",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Viewport Mode Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    Triple(0, "Browser Viewport", Icons.Default.Language),
                    Triple(1, "Terminal TTY (${computer.commandsExecutedCount})", Icons.Default.Terminal),
                    Triple(2, "Files & Artifacts (${computerFiles.size})", Icons.Default.FolderOpen),
                    Triple(3, "Cloud VM Config", Icons.Default.CloudQueue)
                )
                modes.forEach { (idx, label, icon) ->
                    FilterChip(
                        selected = viewportMode == idx,
                        onClick = { onSelectViewportMode(idx) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                            selectedLabelColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("computer_viewport_tab_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MODE 0: PERSISTENT TOKEN-PROTECTED BROWSER DOM VIEWPORT
            if (viewportMode == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = browserUrlDraft,
                        onValueChange = { browserUrlDraft = it },
                        label = { Text("${computer.vmHostname} Browser Worker URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val normalized = if (browserUrlDraft.startsWith("http")) browserUrlDraft else "https://$browserUrlDraft"
                            viewModel.updateAgentComputerBrowserState(computer, normalized, browserDomState.pageTitle)
                            viewModel.inspectUrlWithOpenMuseWorker(normalized)
                        }
                    ) {
                        Text("Inspect DOM")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFF060A12),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (computer.isManualTakeover) AmberAlert else BorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = browserDomState.pageTitle.ifBlank { computer.activeBrowserTitle },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "HTTP ${browserDomState.statusCode} • ${browserDomState.inspectedAtLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = QuantumEmerald
                            )
                        }

                        Text(
                            text = browserDomState.headingsAndSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "INTERACTIVE DOM NODES (${browserDomState.interactiveElements.size}) — Tap node to dispatch action:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuralViolet
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            browserDomState.interactiveElements.take(8).forEach { nodeLabel ->
                                AssistChip(
                                    onClick = {
                                        viewModel.executeCommandOnAgentComputer(
                                            computer = computer,
                                            command = "echo 'Dispatched Browser Worker DOM click on: ${nodeLabel.replace("'", "")}' >> browser_dom_actions.log && tail -n 3 browser_dom_actions.log"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = nodeLabel.take(36),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }

                        if (computer.isManualTakeover) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualDomInputDraft,
                                    onValueChange = { manualDomInputDraft = it },
                                    label = { Text("Manual Takeover Form / Session Input") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (manualDomInputDraft.isNotBlank()) {
                                            viewModel.executeCommandOnAgentComputer(
                                                computer = computer,
                                                command = "echo 'Manual Takeover input submitted: ${manualDomInputDraft.replace("'", "")}' >> manual_takeover_audit.log && cat manual_takeover_audit.log"
                                            )
                                            manualDomInputDraft = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberAlert,
                                        contentColor = Color(0xFF1F1200)
                                    )
                                ) {
                                    Text("Inject")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Active Session: ${computer.activeBrowserTitle} • Network Policy: ${computer.networkIsolationMode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = QuantumEmerald
                )
            }

            // MODE 1: ISOLATED LINUX / POSIX TERMINAL TTY VIEWPORT
            if (viewportMode == 1) {
                Surface(
                    color = Color(0xFF05080F),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "root@${computer.vmHostname}:${computer.workspacePathOnDisk}/workspace#",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = QuantumEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lastReceipt != null && lastReceipt.vmHostname == computer.vmHostname) {
                                "[${lastReceipt.receiptId} • exit=${lastReceipt.exitCode} • ${lastReceipt.elapsedMs}ms]\n$ ${lastReceipt.command}\n${lastReceipt.output}"
                            } else {
                                "${computer.lastTerminalReceipt}\nIsolated POSIX container ready. Run any shell command or script below."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
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
                    val quickTtyCmds = listOf(
                        "pwd && ls -la",
                        "cat vm_manifest.json",
                        "uname -a && id",
                        "head -n 6 /proc/meminfo",
                        "df -h | head -n 6",
                        "sh ../scripts/vm_health_probe.sh"
                    )
                    quickTtyCmds.forEach { cmd ->
                        AssistChip(
                            onClick = {
                                terminalCmdDraft = cmd
                                viewModel.executeCommandOnAgentComputer(computer, cmd)
                            },
                            label = { Text(cmd, style = MaterialTheme.typography.labelSmall) }
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
                        value = terminalCmdDraft,
                        onValueChange = { terminalCmdDraft = it },
                        label = { Text("Execute in ${computer.vmHostname} (/workspace)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("agent_vm_terminal_input")
                    )
                    Button(
                        onClick = { viewModel.executeCommandOnAgentComputer(computer, terminalCmdDraft) },
                        modifier = Modifier.testTag("agent_vm_terminal_run_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run")
                    }
                }
            }

            // MODE 2: ISOLATED FILESYSTEM & DELIVERABLE ARTIFACTS EXPLORER
            if (viewportMode == 2) {
                Text(
                    text = "Isolated VM Volume: ${computer.workspacePathOnDisk}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    computerFiles.forEachIndexed { idx, fileItem ->
                        FilterChip(
                            selected = selectedFileIndex == idx,
                            onClick = { selectedFileIndex = idx },
                            label = {
                                Text(
                                    "${fileItem.relativePath} (${fileItem.sizeBytes}B)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fileEditorCategory,
                        onValueChange = { fileEditorCategory = it },
                        label = { Text("Folder (artifacts/workspace/scripts)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fileEditorName,
                        onValueChange = { fileEditorName = it },
                        label = { Text("Filename") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = fileEditorContent,
                    onValueChange = { fileEditorContent = it },
                    label = { Text("File Content in ${computer.vmHostname}") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveFileInAgentComputer(
                                computer = computer,
                                folderCategory = fileEditorCategory,
                                fileName = fileEditorName,
                                content = fileEditorContent
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to VM Disk")
                    }
                    if (activeFile != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.executeCommandOnAgentComputer(
                                    computer,
                                    "cat \"${activeFile.absolutePath}\""
                                )
                                onSelectViewportMode(1)
                            }
                        ) {
                            Text("Cat in TTY")
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteFileInAgentComputer(computer, activeFile.relativePath)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = CrimsonError)
                        }
                    }
                }
            }

            // MODE 3: CLOUD VM / CONTAINER & NETWORK ISOLATION CONFIG
            if (viewportMode == 3) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Configure Compute Runtime & Remote Cloud Container Bridge for ${computer.vmHostname}:",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ISOLATED_LINUX_CONTAINER", "TERMUX_USERLAND_VM", "CLOUD_VM_BRIDGE").forEach { b ->
                            FilterChip(
                                selected = cfgBackend == b,
                                onClick = { cfgBackend = b },
                                label = { Text(b, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("TOKEN_BROWSER_GATEWAY", "UNRESTRICTED_BRIDGE").forEach { net ->
                            FilterChip(
                                selected = cfgNetMode == net,
                                onClick = { cfgNetMode = net },
                                label = { Text("Network: $net", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = cfgRemoteEndpoint,
                        onValueChange = { cfgRemoteEndpoint = it },
                        label = { Text("Remote Cloud Container HTTP Endpoint (Docker / E2B / Daytona / Webtop)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cfgCloudKey,
                            onValueChange = { cfgCloudKey = it },
                            label = { Text("Cloud VM Bearer Key (AES-256 Encrypted)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cfgTimeout,
                            onValueChange = { cfgTimeout = it },
                            label = { Text("Timeout (s)") },
                            singleLine = true,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.updateAgentComputerCloudConfig(
                                computer = computer,
                                computeBackend = cfgBackend,
                                remoteEndpoint = cfgRemoteEndpoint,
                                rawApiKey = cfgCloudKey,
                                networkIsolationMode = cfgNetMode,
                                timeoutSec = cfgTimeout.toIntOrNull() ?: 30
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Cloud Computer Configuration")
                    }
                }
            }
        }
    }
}

@Composable
private fun MuseTaskPlanAndGenerativeFeedCard(
    viewModel: AetherViewModel,
    computer: AgentComputerEntity,
    computerFiles: List<com.example.domain.AgentComputerFileItem>,
    pendingHitlPages: List<com.example.data.local.OpenDotsSpacePageEntity>,
    agUiEvents: List<com.example.domain.AgUiProtocolEvent>,
    onOpenFilesTab: () -> Unit
) {
    val planSteps = remember(computer.planStepsJson) {
        viewModel.computerRuntime.decodePlanSteps(computer.planStepsJson)
    }
    val latestArtifact = computerFiles.firstOrNull { it.folderCategory == "artifacts" }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Step-by-Step Autonomous Task Plan Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MUSE AUTONOMOUS STEP-BY-STEP PLAN & PROGRESS TRACKER",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
                Text(
                    text = computer.currentGoal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                planSteps.forEach { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = QuantumEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Step ${step.stepNumber}: ${step.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = step.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Latest Receipt: ${computer.lastTerminalReceipt}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
            }
        }

        // 2. Inline Generative Deliverable Artifact Card
        if (latestArtifact != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuantumEmerald.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = QuantumEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "INLINE GENERATIVE ARTIFACT CARD (${computer.vmHostname})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantumEmerald
                                )
                                Text(
                                    text = "${latestArtifact.relativePath} (${latestArtifact.sizeBytes} bytes)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenFilesTab,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Open in VM FS", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = latestArtifact.previewContent.take(420),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 3. Inline Human-in-the-Loop (HITL) Confirmation Gate Cards (if any pending)
        pendingHitlPages.forEach { pendingPage ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AmberAlert, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = AmberAlert
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MUSE HUMAN-IN-THE-LOOP (HITL) APPROVAL GATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberAlert
                            )
                            Text(
                                text = "${pendingPage.authoredByDot} requests approval to commit '${pendingPage.pageTitle}'",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = pendingPage.markdownContent.take(220),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.approveOpenDotsSpacePage(pendingPage) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumEmerald,
                                contentColor = Color(0xFF002114)
                            )
                        ) {
                            Text("Approve & Commit to Space", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { viewModel.rejectOpenDotsSpacePage(pendingPage) }
                        ) {
                            Text("Reject", style = MaterialTheme.typography.labelSmall, color = CrimsonError)
                        }
                    }
                }
            }
        }
    }
}
