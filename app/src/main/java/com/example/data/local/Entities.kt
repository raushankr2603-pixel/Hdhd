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
