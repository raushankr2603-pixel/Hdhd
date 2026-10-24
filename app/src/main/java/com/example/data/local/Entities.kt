package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerName: String,
    val baseUrl: String,
    val encryptedApiKey: String,
    val modelName: String,
    val isLocalOffline: Boolean = false,
    val isActive: Boolean = false,
    val lastLatencyMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "voice_macros",
    indices = [Index(value = ["triggerPhrase"], unique = false)]
)
data class VoiceMacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val triggerPhrase: String,
    val stepsJson: String, // Encoded sequence chain for fast preview & backwards compatibility
    val isOfflineCritical: Boolean = true,
    val e2eeSignature: String = "",
    val runCount: Int = 0,
    val lastExecutedAt: Long = 0L
)

/**
 * Relational entity storing ordered UI automation and device control commands for a VoiceMacroEntity.
 */
@Entity(
    tableName = "macro_automation_steps",
    foreignKeys = [
        ForeignKey(
            entity = VoiceMacroEntity::class,
            parentColumns = ["id"],
            childColumns = ["macroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["macroId"])]
)
data class MacroAutomationStepEntity(
    @PrimaryKey(autoGenerate = true) val stepId: Int = 0,
    val macroId: Int,
    val stepOrder: Int,
    val actionType: String, // e.g., "CLICK_TEXT", "GESTURE_TAP", "GESTURE_SCROLL", "GLOBAL_NAV", "LAUNCH_APP", "SET_BRIGHTNESS", "SET_VOLUME"
    val selectorType: String = "TEXT_OR_ID", // "TEXT", "VIEW_ID", "COORDINATES", "DIRECTION", "SYSTEM_SETTING", "PACKAGE"
    val targetValue: String, // e.g., "Search", "540,1150", "DOWN", "com.android.settings", "80"
    val inputPayload: String = "", // Optional text to inject or secondary parameter
    val waitDelayMs: Long = 200L,
    val continueOnError: Boolean = true
)

/**
 * Room relational projection joining a VoiceMacroEntity with its ordered list of UI automation commands.
 */
data class VoiceMacroWithSteps(
    @Embedded val macro: VoiceMacroEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "macroId"
    )
    val steps: List<MacroAutomationStepEntity>
) {
    val orderedSteps: List<MacroAutomationStepEntity>
        get() = steps.sortedBy { it.stepOrder }
}

@Entity(tableName = "smart_devices")
data class SmartDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "SMART_HOME", "PHONE_NODE", "DESKTOP_APP", "BROWSER_EXT"
    val protocol: String, // "Matter / Local HTTP", "Home Assistant REST", "WebSocket E2EE", "ADB/LAN"
    val endpointUrl: String,
    val isOnline: Boolean = true,
    val stateLabel: String = "Active",
    val powerState: Boolean = true,
    val telemetryValue: String = "Synced"
)

@Entity(tableName = "sub_agents")
data class SubAgentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val parentAgentName: String = "Aether Core v4",
    val generationDepth: Int = 1,
    val status: String = "RUNNING",
    val currentTask: String,
    val cpuAllocationPercent: Int = 12,
    val stepsCompleted: Int = 0,
    val totalSteps: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "github_repos")
data class GitHubRepoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerRepo: String,
    val description: String,
    val defaultBranch: String,
    val starsCount: Int,
    val openIssuesCount: Int,
    val language: String,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val automationHook: String = "Auto-Watch & Shell Sync"
)

@Entity(tableName = "shell_history")
data class ShellHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val output: String,
    val isRootAttempt: Boolean = false,
    val exitCode: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * OpenDots Specialist AI Coworker ("Dot") with bounded permissions, role instructions, and sandboxed OpenBot/Termux environment.
 */
@Entity(tableName = "opendots_coworkers")
data class OpenDotsCoworkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dotName: String,
    val roleTitle: String,
    val systemInstructions: String,
    val sandboxEnvironment: String, // e.g. "OpenBot Chromium + Termux Linux Sandbox"
    val allowedToolsCsv: String, // e.g. "BROWSER_DOM,TERMUX_SHELL,SPACES_WRITE,GITHUB_MCP"
    val assignedSpaceName: String = "Engineering & Research",
    val status: String = "READY", // "READY", "WORKING", "AWAITING_HITL"
    val completedPagesCount: Int = 0,
    val lastOutputSummary: String = "Ready for autonomous workspace assignment",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * OpenDots Workspace Document ("Space & Page") with Human-in-the-Loop (HITL) approval gate and on-disk Markdown persistence.
 */
@Entity(tableName = "opendots_space_pages")
data class OpenDotsSpacePageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val spaceName: String,
    val pageTitle: String,
    val markdownContent: String,
    val authoredByDot: String,
    val approvalStatus: String = "PENDING_HITL", // "PENDING_HITL", "APPROVED", "REJECTED"
    val filePathOnDisk: String = "",
    val versionNumber: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * AnyClaw / OpenClaw ClawHub Skill, MCP Tool Binding, or OpenMuse Self-Improving Skill Proposal.
 */
@Entity(tableName = "claw_skills")
data class ClawSkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val skillKey: String,
    val title: String,
    val ecosystemSource: String, // "ANYCLAW_CLI", "OPENCLAW_MCP", "OPENMUSE_SELF_IMPROVED", "TERMUX_BRIDGE"
    val description: String,
    val executablePayload: String, // Shell script, MCP JSON-RPC method, or DOM action sequence
    val isApproved: Boolean = true, // False when proposed by OpenMuse Self-Improving Skills until user approves
    val usageCount: Int = 0,
    val lastResultLog: String = "Idle",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * OpenClaw Multi-Channel Gateway Connector (Telegram, Discord, Slack, WhatsApp, WebChat SSE, AG-UI Stream).
 */
@Entity(tableName = "claw_gateway_channels")
data class ClawGatewayChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelType: String, // "TELEGRAM", "DISCORD", "SLACK", "WHATSAPP", "WEBCHAT_AG_UI"
    val channelName: String,
    val endpointOrBotUrl: String,
    val encryptedToken: String = "",
    val isEnabled: Boolean = true,
    val messagesRoutedCount: Int = 0,
    val lastEventStatus: String = "Gateway Standby"
)

/**
 * Dedicated Cloud / Sandbox Computer assigned to an individual AI Agent (MetaMuse / GrokBot / OpenBot architecture).
 * Each AgentComputerEntity has its own isolated on-disk Linux/POSIX directory volume, persistent Browser Worker state,
 * terminal execution receipts, network policy, and optional remote Cloud VM endpoint.
 */
@Entity(tableName = "agent_computers")
data class AgentComputerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val agentName: String,
    val vmHostname: String,
    val personaRole: String,
    val computeBackend: String = "ISOLATED_LINUX_CONTAINER", // "ISOLATED_LINUX_CONTAINER", "CLOUD_VM_BRIDGE", "TERMUX_USERLAND_VM"
    val remoteCloudEndpoint: String = "", // Optional HTTP/WebSocket remote container endpoint (E2B / Docker / Daytona / Custom)
    val encryptedCloudApiKey: String = "",
    val workspacePathOnDisk: String = "",
    val activeBrowserUrl: String = "https://github.com/CopilotKit/OpenMuse",
    val activeBrowserTitle: String = "OpenMuse Agent Computer Viewport",
    val isManualTakeover: Boolean = false,
    val networkIsolationMode: String = "TOKEN_BROWSER_GATEWAY", // "TOKEN_BROWSER_GATEWAY" or "UNRESTRICTED_BRIDGE"
    val executionTimeoutSec: Int = 30,
    val status: String = "ONLINE", // "ONLINE", "EXECUTING", "MANUAL_TAKEOVER", "AWAITING_HITL"
    val currentGoal: String = "Ready for autonomous multi-step computer workflow",
    val planStepsJson: String = "[]",
    val lastTerminalReceipt: String = "VM initialized • Exit Code 0",
    val artifactsCount: Int = 2,
    val commandsExecutedCount: Int = 1,
    val cpuLoadPercent: Int = 12,
    val memoryUsedMb: Int = 148,
    val updatedAt: Long = System.currentTimeMillis()
)


