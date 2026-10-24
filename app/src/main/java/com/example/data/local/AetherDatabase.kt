package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.security.E2EEVault
import com.example.domain.MacroSequenceCodec

@Database(
    entities = [
        AiProviderEntity::class,
        VoiceMacroEntity::class,
        MacroAutomationStepEntity::class,
        SmartDeviceEntity::class,
        SubAgentEntity::class,
        GitHubRepoEntity::class,
        ShellHistoryEntity::class,
        OpenDotsCoworkerEntity::class,
        OpenDotsSpacePageEntity::class,
        ClawSkillEntity::class,
        ClawGatewayChannelEntity::class,
        AgentComputerEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun aetherDao(): AetherDao

    companion object {
        @Volatile
        private var INSTANCE: AetherDatabase? = null

        fun getInstance(context: Context): AetherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AetherDatabase::class.java,
                    "aether_os_agent.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDefaultsIfEmpty(dao: AetherDao) {
            val existingProviders = dao.getAllProvidersOnce()
            if (existingProviders.isEmpty()) {
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "Aether Local Neural Core (Offline)",
                        baseUrl = "local://on-device-neural-engine",
                        encryptedApiKey = E2EEVault.encrypt("OFFLINE_LOCAL_ZERO_TRUST"),
                        modelName = "aether-edge-3b-int4",
                        isLocalOffline = true,
                        isActive = true,
                        lastLatencyMs = 8L
                    )
                )
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "Ollama Local Server",
                        baseUrl = "http://127.0.0.1:11434/v1",
                        encryptedApiKey = E2EEVault.encrypt("ollama"),
                        modelName = "llama3.2:3b",
                        isLocalOffline = true,
                        isActive = false,
                        lastLatencyMs = 24L
                    )
                )
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "NVIDIA NIM Cloud",
                        baseUrl = "https://integrate.api.nvidia.com/v1",
                        encryptedApiKey = "",
                        modelName = "meta/llama-3.1-70b-instruct",
                        isLocalOffline = false,
                        isActive = false
                    )
                )
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "OpenRouter Unified API",
                        baseUrl = "https://openrouter.ai/api/v1",
                        encryptedApiKey = "",
                        modelName = "anthropic/claude-3.5-sonnet",
                        isLocalOffline = false,
                        isActive = false
                    )
                )
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "Groq Cloud LPU API",
                        baseUrl = "https://api.groq.com/openai/v1",
                        encryptedApiKey = "",
                        modelName = "llama-3.3-70b-versatile",
                        isLocalOffline = false,
                        isActive = false
                    )
                )
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "xAI Grok API",
                        baseUrl = "https://api.x.ai/v1",
                        encryptedApiKey = "",
                        modelName = "grok-2-latest",
                        isLocalOffline = false,
                        isActive = false
                    )
                )
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "OpenAI-Compatible API",
                        baseUrl = "https://api.openai.com/v1",
                        encryptedApiKey = "",
                        modelName = "gpt-4o-mini",
                        isLocalOffline = false,
                        isActive = false
                    )
                )
            } else if (existingProviders.none { it.providerName.contains("Groq", ignoreCase = true) }) {
                dao.insertProvider(
                    AiProviderEntity(
                        providerName = "Groq Cloud LPU API",
                        baseUrl = "https://api.groq.com/openai/v1",
                        encryptedApiKey = "",
                        modelName = "llama-3.3-70b-versatile",
                        isLocalOffline = false,
                        isActive = false
                    )
                )
            }

            if (dao.getAllMacrosOnce().isEmpty()) {
                seedMacroWithRelationalSteps(
                    dao = dao,
                    title = "Deep Focus & Stealth Protocol",
                    triggerPhrase = "activate stealth focus",
                    stepsChain = "SET_BRIGHTNESS:25 -> SET_VOLUME:0 -> SMART_HOME:Study Lights Dim -> SHELL:sync && echo 'Focus Active'"
                )
                seedMacroWithRelationalSteps(
                    dao = dao,
                    title = "Morning Workstation Warmup",
                    triggerPhrase = "good morning workstation",
                    stepsChain = "SET_BRIGHTNESS:85 -> SET_VOLUME:70 -> SMART_HOME:Studio Desk Power ON -> TTS:Workstation ready"
                )
                seedMacroWithRelationalSteps(
                    dao = dao,
                    title = "Autonomous Screen Scroll & Inspect",
                    triggerPhrase = "inspect and scroll screen",
                    stepsChain = "CLICK_TEXT:Live -> GESTURE_SCROLL:DOWN -> GESTURE_TAP:540,1200 -> TTS:Screen workflow complete"
                )
            }

            if (dao.getAllSmartDevicesOnce().isEmpty()) {
                dao.insertSmartDevice(
                    SmartDeviceEntity(
                        name = "Living Room Matter Bridge",
                        category = "SMART_HOME",
                        protocol = "Matter / Home Assistant REST",
                        endpointUrl = "http://192.168.1.50:8123/api/states/light.living_room",
                        isOnline = true,
                        stateLabel = "2700K Warm • 80%",
                        powerState = true,
                        telemetryValue = "14.2W"
                    )
                )
                dao.insertSmartDevice(
                    SmartDeviceEntity(
                        name = "Studio Climate & HVAC Thermostat",
                        category = "SMART_HOME",
                        protocol = "MQTT / Local Webhook",
                        endpointUrl = "http://192.168.1.52:8080/climate",
                        isOnline = true,
                        stateLabel = "Cooling • 21.5°C",
                        powerState = true,
                        telemetryValue = "44% RH"
                    )
                )
                dao.insertSmartDevice(
                    SmartDeviceEntity(
                        name = "Aether Desktop Standalone App",
                        category = "DESKTOP_APP",
                        protocol = "WebSocket E2EE Tunnel (TLS 1.3)",
                        endpointUrl = "ws://192.168.1.100:9443/aether-desktop",
                        isOnline = true,
                        stateLabel = "Clipboard & Workflow Synced",
                        powerState = true,
                        telemetryValue = "E2EE Verified"
                    )
                )
                dao.insertSmartDevice(
                    SmartDeviceEntity(
                        name = "Aether Chromium/Firefox Extension",
                        category = "BROWSER_EXT",
                        protocol = "Encrypted WebRTC / REST Bridge",
                        endpointUrl = "http://127.0.0.1:9444/ext-bridge",
                        isOnline = true,
                        stateLabel = "Active Tab Context Ready",
                        powerState = true,
                        telemetryValue = "Zero-Trust"
                    )
                )
                dao.insertSmartDevice(
                    SmartDeviceEntity(
                        name = "Secondary Pixel Fold Node",
                        category = "PHONE_NODE",
                        protocol = "Peer-to-Peer E2EE Mesh",
                        endpointUrl = "http://192.168.1.118:9445/peer",
                        isOnline = true,
                        stateLabel = "Battery 88% • Mesh Linked",
                        powerState = true,
                        telemetryValue = "12ms RTT"
                    )
                )
            }

            if (dao.getAllSubAgentsOnce().isEmpty()) {
                dao.insertSubAgent(
                    SubAgentEntity(
                        name = "UI Navigator & Screen Vision Agent",
                        role = "Accessibility Tree & Gesture Executor",
                        parentAgentName = "Aether Core v4",
                        generationDepth = 1,
                        status = "RUNNING",
                        currentTask = "Monitoring active window hierarchy & gesture queue",
                        cpuAllocationPercent = 14,
                        stepsCompleted = 4,
                        totalSteps = 5
                    )
                )
                dao.insertSubAgent(
                    SubAgentEntity(
                        name = "System Optimizer & Kernel Sentinel",
                        role = "Memory Trim, Thermal & IO Diagnostics",
                        parentAgentName = "Aether Core v4",
                        generationDepth = 1,
                        status = "RUNNING",
                        currentTask = "Sampling /proc/meminfo and background process threads",
                        cpuAllocationPercent = 8,
                        stepsCompleted = 5,
                        totalSteps = 5
                    )
                )
            }

            val existingRepos = dao.getAllGitHubReposOnce()
            if (existingRepos.isEmpty() || existingRepos.none { it.ownerRepo.contains("OpenMuse", ignoreCase = true) }) {
                val coreRepos = listOf(
                    GitHubRepoEntity(
                        ownerRepo = "CopilotKit/OpenMuse",
                        description = "Open-source personal AI agent template with AG-UI streaming, persistent Chromium browser worker, Manual Takeover ('Take Control'), and self-improving skills.",
                        defaultBranch = "main",
                        starsCount = 6840,
                        openIssuesCount = 42,
                        language = "TypeScript / React Native",
                        automationHook = "AG-UI Event Stream + Persistent Browser Worker & Manual Takeover"
                    ),
                    GitHubRepoEntity(
                        ownerRepo = "openclaw/openclaw",
                        description = "Self-hosted autonomous AI agent gateway with persistent MEMORY.md & SOUL.md workspace files, ClawHub skills, MCP server bridge, and multi-channel messaging.",
                        defaultBranch = "main",
                        starsCount = 148200,
                        openIssuesCount = 610,
                        language = "TypeScript / Node.js",
                        automationHook = "MEMORY.md + SOUL.md Sync, ClawHub Skills & Multi-Channel Gateway"
                    ),
                    GitHubRepoEntity(
                        ownerRepo = "fastclaw-ai/anyclaw",
                        description = "Android mobile adaptation & CLI wrapper packaging OpenClaw + coding agents, MCP bridges, and local Linux/Termux userland execution on Android without PC.",
                        defaultBranch = "main",
                        starsCount = 9420,
                        openIssuesCount = 38,
                        language = "Kotlin / Shell / Node.js",
                        automationHook = "Termux RUN_COMMAND Bridge & Local Android Userland Executor"
                    ),
                    GitHubRepoEntity(
                        ownerRepo = "CopilotKit/OpenDots",
                        description = "Self-hosted multi-agent workspace with specialist AI Coworkers ('Dots'), OpenBot sandboxes, Spaces & Pages markdown workspace, and Human-in-the-Loop (HITL) approval gates.",
                        defaultBranch = "main",
                        starsCount = 7390,
                        openIssuesCount = 31,
                        language = "TypeScript / TanStack AI",
                        automationHook = "Specialist Dots Dispatcher + Spaces & Pages HITL Approval Gate"
                    ),
                    GitHubRepoEntity(
                        ownerRepo = "termux/termux-app",
                        description = "Android terminal emulator and Linux environment app with RunCommandService external execution bridge, pkg bootstrap, Termux:API, Termux:Boot, Termux:Widget & X11.",
                        defaultBranch = "master",
                        starsCount = 41500,
                        openIssuesCount = 290,
                        language = "Java / C / Shell",
                        automationHook = "com.termux.RUN_COMMAND Service Bridge & Sub-Dependencies Manager"
                    ),
                    GitHubRepoEntity(
                        ownerRepo = "ollama/ollama",
                        description = "Get up and running with Llama 3.3, DeepSeek-R1, Phi-4, Gemma 3, and Qwen 2.5 Coder locally.",
                        defaultBranch = "main",
                        starsCount = 115400,
                        openIssuesCount = 840,
                        language = "Go",
                        automationHook = "Local Model Sync & Runner"
                    )
                )
                coreRepos.forEach { repo ->
                    if (existingRepos.none { it.ownerRepo.equals(repo.ownerRepo, ignoreCase = true) }) {
                        dao.insertGitHubRepo(repo)
                    }
                }
            }

            if (dao.getAllOpenDotsCoworkersOnce().isEmpty()) {
                dao.insertOpenDotsCoworker(
                    OpenDotsCoworkerEntity(
                        dotName = "Atlas • Deep Researcher Dot",
                        roleTitle = "Autonomous Web & Repository Intelligence Specialist",
                        systemInstructions = "Inspect web DOMs via OpenMuse Browser Worker, synthesize technical architectures, and draft structured briefs into the Engineering & Research Space with citations.",
                        sandboxEnvironment = "OpenBot Chromium Profile + AG-UI Stream",
                        allowedToolsCsv = "BROWSER_DOM,SPACES_WRITE,GITHUB_API,MEMORY_MD",
                        assignedSpaceName = "Engineering & Research",
                        status = "READY",
                        completedPagesCount = 3,
                        lastOutputSummary = "Authored 'OpenMuse, AnyClaw & OpenDots Unified Architecture' brief"
                    )
                )
                dao.insertOpenDotsCoworker(
                    OpenDotsCoworkerEntity(
                        dotName = "Forge • Full-Stack & Termux Coder Dot",
                        roleTitle = "Android, POSIX Shell & Termux Automation Engineer",
                        systemInstructions = "Write and execute real POSIX shell, Python, and Node.js scripts inside the Termux RunCommandService bridge and local Linux userland. Propose reusable ClawHub skills.",
                        sandboxEnvironment = "Termux /data/data/com.termux/files/home + Local Userland",
                        allowedToolsCsv = "TERMUX_RUN_COMMAND,LOCAL_SHELL,SKILL_BUILDER,SPACES_WRITE",
                        assignedSpaceName = "DevOps & Termux Runbooks",
                        status = "READY",
                        completedPagesCount = 2,
                        lastOutputSummary = "Verified Termux sub-dependencies & POSIX environment variables"
                    )
                )
                dao.insertOpenDotsCoworker(
                    OpenDotsCoworkerEntity(
                        dotName = "Sentinel • Security & HITL Auditor Dot",
                        roleTitle = "Zero-Trust Permission, E2EE & Accessibility Policy Guard",
                        systemInstructions = "Audit Android Accessibility actions, Termux external commands, and MCP tool calls. Enforce Human-in-the-Loop (HITL) approval gates before committing system changes.",
                        sandboxEnvironment = "Zero-Trust E2EE Vault + Permission Inspector",
                        allowedToolsCsv = "PERMISSION_AUDIT,E2EE_VAULT,ACCESSIBILITY_INSPECT,HITL_GATE",
                        assignedSpaceName = "Security & Compliance",
                        status = "READY",
                        completedPagesCount = 1,
                        lastOutputSummary = "All non-read-only Spaces writes gated by HITL approval"
                    )
                )
            }

            if (dao.getAllOpenDotsSpacePagesOnce().isEmpty()) {
                dao.insertOpenDotsSpacePage(
                    OpenDotsSpacePageEntity(
                        spaceName = "Engineering & Research",
                        pageTitle = "OpenMuse + AnyClaw + OpenDots + Termux Master Blueprint",
                        markdownContent = """
# Unified Agentic Architecture Blueprint
- **OpenMuse (`CopilotKit/OpenMuse`)**: Event-driven **AG-UI Protocol** (`RUN_STARTED`, `TOOL_CALL_START`, `HITL_CHECKPOINT`, `STATE_DELTA`), Persistent Browser Worker with **Manual Takeover ("Take Control")**, and Self-Improving Skills.
- **AnyClaw & OpenClaw (`openclaw/openclaw` & `fastclaw-ai/anyclaw`)**: Persistent on-disk workspace memory (`MEMORY.md`, `SOUL.md`, `TOOLS.md`), **ClawHub Skills**, **MCP JSON-RPC 2.0 Server Bridge**, and Multi-Channel Gateway (Telegram, Discord, Slack, WhatsApp, WebChat).
- **OpenDots (`CopilotKit/OpenDots`)**: Specialist AI Coworkers (**Dots**) operating inside isolated sandboxes and authoring collaborative markdown documents in **Spaces & Pages** protected by **Human-in-the-Loop (HITL) Approval Gates**.
- **Termux (`termux/termux-app`)**: Native `com.termux.RUN_COMMAND` intent bridge to `com.termux.app.RunCommandService` + local POSIX userland script runner and sub-dependency inspector (`Termux:API`, `Termux:Boot`, `Termux:Widget`, `Termux:Float`, `Termux:Styling`, `Termux:X11`).
                        """.trimIndent(),
                        authoredByDot = "Atlas • Deep Researcher Dot",
                        approvalStatus = "APPROVED",
                        versionNumber = 1
                    )
                )
                dao.insertOpenDotsSpacePage(
                    OpenDotsSpacePageEntity(
                        spaceName = "DevOps & Termux Runbooks",
                        pageTitle = "Automated Termux + AnyClaw Node Bootstrap Runbook",
                        markdownContent = """
# Termux & AnyClaw Environment Bootstrap
1. Enable external app execution inside `~/.termux/termux.properties`:
   `allow-external-apps=true`
2. Bootstrap core packages in Termux:
   `pkg update -y && pkg install -y git nodejs python clang curl openssh ffmpeg termux-api proot-distro`
3. Verify local workspace files (`MEMORY.md` & `SOUL.md`) and MCP tool bindings.
                        """.trimIndent(),
                        authoredByDot = "Forge • Full-Stack & Termux Coder Dot",
                        approvalStatus = "PENDING_HITL",
                        versionNumber = 1
                    )
                )
            }

            if (dao.getAllClawSkillsOnce().isEmpty()) {
                dao.insertClawSkill(
                    ClawSkillEntity(
                        skillKey = "anyclaw_sys_snapshot",
                        title = "AnyClaw Kernel, CPU, Memory & Mount Snapshot",
                        ecosystemSource = "ANYCLAW_CLI",
                        description = "Executes real POSIX telemetry inspection across /proc/version, /proc/meminfo, df -h, and active process tree.",
                        executablePayload = "uname -a && head -n 5 /proc/meminfo && df -h | head -n 6",
                        isApproved = true,
                        usageCount = 4
                    )
                )
                dao.insertClawSkill(
                    ClawSkillEntity(
                        skillKey = "termux_env_doctor",
                        title = "Termux Sub-Dependencies & Binary Toolchain Doctor",
                        ecosystemSource = "TERMUX_BRIDGE",
                        description = "Audits available shell binaries (sh, toybox, ping, curl, getprop, logcat) and checks Termux usr/bin environment paths.",
                        executablePayload = "which sh toybox getprop ping top ps && getprop ro.product.cpu.abi",
                        isApproved = true,
                        usageCount = 3
                    )
                )
                dao.insertClawSkill(
                    ClawSkillEntity(
                        skillKey = "openclaw_memory_consolidate",
                        title = "OpenClaw MEMORY.md & Daily Log Consolidator",
                        ecosystemSource = "OPENCLAW_MCP",
                        description = "Reads current device state, active AI provider, and recent commands and appends a timestamped entry into MEMORY.md.",
                        executablePayload = "MCP_TOOL:memory.append_fact",
                        isApproved = true,
                        usageCount = 5
                    )
                )
                dao.insertClawSkill(
                    ClawSkillEntity(
                        skillKey = "openmuse_auto_dom_harvester",
                        title = "OpenMuse Self-Improved Skill: Live DOM Action & Link Extractor",
                        ecosystemSource = "OPENMUSE_SELF_IMPROVED",
                        description = "Proposed by OpenMuse Intelligence after observing browser & screen automation patterns: extracts all interactive links, buttons, and inputs from the active page into a structured Space note.",
                        executablePayload = "DOM_INSPECT:EXTRACT_INTERACTIVE_ELEMENTS",
                        isApproved = false, // Waiting for manual user approval per OpenMuse Self-Improving Skills spec
                        usageCount = 0
                    )
                )
            }

            if (dao.getAllClawGatewayChannelsOnce().isEmpty()) {
                dao.insertClawGatewayChannel(
                    ClawGatewayChannelEntity(
                        channelType = "WEBCHAT_AG_UI",
                        channelName = "OpenMuse AG-UI SSE & Local WebChat Gateway",
                        endpointOrBotUrl = "http://127.0.0.1:9440/ag-ui/stream",
                        isEnabled = true,
                        messagesRoutedCount = 12,
                        lastEventStatus = "AG-UI Event Stream Active (RUN_STARTED / STATE_DELTA / HITL_GATE)"
                    )
                )
                dao.insertClawGatewayChannel(
                    ClawGatewayChannelEntity(
                        channelType = "TELEGRAM",
                        channelName = "OpenClaw Telegram Bot Bridge",
                        endpointOrBotUrl = "https://api.telegram.org/bot<TOKEN>/sendMessage",
                        isEnabled = true,
                        messagesRoutedCount = 0,
                        lastEventStatus = "Ready (Enter Bot Token & Chat URL to dispatch live Telegram messages)"
                    )
                )
                dao.insertClawGatewayChannel(
                    ClawGatewayChannelEntity(
                        channelType = "DISCORD",
                        channelName = "OpenClaw Discord Webhook Relay",
                        endpointOrBotUrl = "https://discord.com/api/webhooks/<ID>/<TOKEN>",
                        isEnabled = true,
                        messagesRoutedCount = 0,
                        lastEventStatus = "Ready (Supports live HTTP POST webhook JSON payloads)"
                    )
                )
                dao.insertClawGatewayChannel(
                    ClawGatewayChannelEntity(
                        channelType = "SLACK",
                        channelName = "OpenDots & OpenClaw Slack Channels SDK",
                        endpointOrBotUrl = "https://hooks.slack.com/services/<T00>/<B00>/<KEY>",
                        isEnabled = true,
                        messagesRoutedCount = 0,
                        lastEventStatus = "Ready for Dot Coworker thread notifications & HITL approvals"
                    )
                )
            }

            if (dao.getAllAgentComputersOnce().isEmpty()) {
                dao.insertAgentComputer(
                    AgentComputerEntity(
                        agentName = "GrokBot • Autonomous Software & Cloud Engineer",
                        vmHostname = "grokbot-vm-01",
                        personaRole = "Full-Stack Code Execution, Shell Automation & Cloud Container Architect",
                        computeBackend = "ISOLATED_LINUX_CONTAINER",
                        activeBrowserUrl = "https://github.com/openclaw/openclaw",
                        activeBrowserTitle = "GrokBot Browser Worker • OpenClaw Repository",
                        networkIsolationMode = "TOKEN_BROWSER_GATEWAY",
                        executionTimeoutSec = 30,
                        status = "ONLINE",
                        currentGoal = "Autonomous repository inspection, shell diagnostics & build artifact generation",
                        planStepsJson = """[{"step":1,"title":"Provision Isolated VM Workspace & Environment","status":"COMPLETED"},{"step":2,"title":"Inspect Target Repository DOM via Token-Protected Browser","status":"COMPLETED"},{"step":3,"title":"Execute POSIX Kernel & Toolchain Verification in VM","status":"COMPLETED"},{"step":4,"title":"Write Verified Deliverable Artifact to /artifacts","status":"COMPLETED"}]""",
                        lastTerminalReceipt = "Receipt #GB-101 • exit=0 • 14ms • /workspace/grokbot_audit.sh",
                        artifactsCount = 3,
                        commandsExecutedCount = 6,
                        cpuLoadPercent = 18,
                        memoryUsedMb = 212
                    )
                )
                dao.insertAgentComputer(
                    AgentComputerEntity(
                        agentName = "MetaMuse • Deep Web & Multimodal Research Agent",
                        vmHostname = "metamuse-vm-02",
                        personaRole = "Persistent Chromium Browser Worker, DOM Form Automation & Knowledge Synthesis",
                        computeBackend = "ISOLATED_LINUX_CONTAINER",
                        activeBrowserUrl = "https://github.com/CopilotKit/OpenMuse",
                        activeBrowserTitle = "MetaMuse Secure VM • CopilotKit/OpenMuse",
                        networkIsolationMode = "TOKEN_BROWSER_GATEWAY",
                        executionTimeoutSec = 30,
                        status = "ONLINE",
                        currentGoal = "Stream AG-UI research cards, extract live DOM nodes, and compile executive briefs",
                        planStepsJson = """[{"step":1,"title":"Initialize MetaMuse Secure VM & Chromium Profile","status":"COMPLETED"},{"step":2,"title":"Extract Interactive DOM Nodes & Headings","status":"COMPLETED"},{"step":3,"title":"Synthesize Structured Markdown Brief in VM Volume","status":"COMPLETED"}]""",
                        lastTerminalReceipt = "Receipt #MM-204 • exit=0 • 19ms • DOM Harvest Complete",
                        artifactsCount = 3,
                        commandsExecutedCount = 5,
                        cpuLoadPercent = 14,
                        memoryUsedMb = 184
                    )
                )
                dao.insertAgentComputer(
                    AgentComputerEntity(
                        agentName = "AnyClaw • Termux & Linux Kernel DevOps Agent",
                        vmHostname = "anyclaw-vm-03",
                        personaRole = "Termux RunCommandService Bridge, MCP Server Host & System Optimizer",
                        computeBackend = "TERMUX_USERLAND_VM",
                        activeBrowserUrl = "https://github.com/fastclaw-ai/anyclaw",
                        activeBrowserTitle = "AnyClaw Mobile Linux & MCP Runtime",
                        networkIsolationMode = "UNRESTRICTED_BRIDGE",
                        executionTimeoutSec = 30,
                        status = "ONLINE",
                        currentGoal = "Manage Termux sub-dependencies, MEMORY.md/SOUL.md sync & POSIX automation",
                        planStepsJson = """[{"step":1,"title":"Mount Termux & Local POSIX Userland Volume","status":"COMPLETED"},{"step":2,"title":"Probe 16 System & Termux Binaries","status":"COMPLETED"},{"step":3,"title":"Sync MEMORY.md & SOUL.md into Agent VM","status":"COMPLETED"}]""",
                        lastTerminalReceipt = "Receipt #AC-309 • exit=0 • 11ms • POSIX Toolchain Verified",
                        artifactsCount = 2,
                        commandsExecutedCount = 8,
                        cpuLoadPercent = 11,
                        memoryUsedMb = 156
                    )
                )
                dao.insertAgentComputer(
                    AgentComputerEntity(
                        agentName = "OpenDots • Collaborative Spaces & HITL Architect",
                        vmHostname = "opendots-vm-04",
                        personaRole = "Multi-Agent Workspace Coordinator & Human-in-the-Loop Document Publisher",
                        computeBackend = "ISOLATED_LINUX_CONTAINER",
                        activeBrowserUrl = "https://github.com/CopilotKit/OpenDots",
                        activeBrowserTitle = "OpenDots Multi-Agent Workspace",
                        networkIsolationMode = "TOKEN_BROWSER_GATEWAY",
                        executionTimeoutSec = 30,
                        status = "ONLINE",
                        currentGoal = "Coordinate specialist Dots and enforce Human-in-the-Loop (HITL) approval gates",
                        planStepsJson = """[{"step":1,"title":"Initialize OpenBot Sandbox Volume","status":"COMPLETED"},{"step":2,"title":"Verify Spaces & Pages On-Disk Directory","status":"COMPLETED"},{"step":3,"title":"Enforce HITL Checkpoints on Non-Read-Only Tools","status":"COMPLETED"}]""",
                        lastTerminalReceipt = "Receipt #OD-402 • exit=0 • 9ms • Spaces Volume Mounted",
                        artifactsCount = 2,
                        commandsExecutedCount = 4,
                        cpuLoadPercent = 9,
                        memoryUsedMb = 140
                    )
                )
            }
        }

        private suspend fun seedMacroWithRelationalSteps(
            dao: AetherDao,
            title: String,
            triggerPhrase: String,
            stepsChain: String
        ) {
            val decoded = MacroSequenceCodec.decodeChainToSteps(stepsChain)
            val macro = VoiceMacroEntity(
                title = title,
                triggerPhrase = triggerPhrase,
                stepsJson = stepsChain,
                isOfflineCritical = true,
                e2eeSignature = E2EEVault.sha256Fingerprint(triggerPhrase)
            )
            val stepEntities = decoded.mapIndexed { idx, s ->
                MacroAutomationStepEntity(
                    macroId = 0,
                    stepOrder = idx,
                    actionType = s.actionType.code,
                    selectorType = s.actionType.category.name,
                    targetValue = s.targetValue,
                    continueOnError = s.continueOnError
                )
            }
            dao.saveMacroWithAutomationSteps(macro, stepEntities)
        }
    }
}
