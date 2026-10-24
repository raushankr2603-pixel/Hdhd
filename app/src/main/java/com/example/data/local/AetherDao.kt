package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AetherDao {
    // AI Providers
    @Query("SELECT * FROM ai_providers ORDER BY isActive DESC, timestamp DESC")
    fun observeProviders(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers")
    suspend fun getAllProvidersOnce(): List<AiProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: AiProviderEntity): Long

    @Query("UPDATE ai_providers SET isActive = (id = :activeId)")
    suspend fun setActiveProvider(activeId: Int)

    @Query("UPDATE ai_providers SET lastLatencyMs = :latencyMs WHERE id = :id")
    suspend fun updateProviderLatency(id: Int, latencyMs: Long)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteProvider(id: Int)

    // Voice Macros & Relational UI Automation Steps
    @Query("SELECT * FROM voice_macros ORDER BY isOfflineCritical DESC, id ASC")
    fun observeMacros(): Flow<List<VoiceMacroEntity>>

    @Transaction
    @Query("SELECT * FROM voice_macros ORDER BY isOfflineCritical DESC, id ASC")
    fun observeMacrosWithSteps(): Flow<List<VoiceMacroWithSteps>>

    @Query("SELECT * FROM voice_macros")
    suspend fun getAllMacrosOnce(): List<VoiceMacroEntity>

    @Transaction
    @Query("SELECT * FROM voice_macros WHERE id = :macroId LIMIT 1")
    suspend fun getMacroWithStepsById(macroId: Int): VoiceMacroWithSteps?

    @Transaction
    @Query("SELECT * FROM voice_macros WHERE LOWER(triggerPhrase) = LOWER(:trigger) LIMIT 1")
    suspend fun findMacroWithStepsByTrigger(trigger: String): VoiceMacroWithSteps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: VoiceMacroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationSteps(steps: List<MacroAutomationStepEntity>)

    @Query("DELETE FROM macro_automation_steps WHERE macroId = :macroId")
    suspend fun deleteStepsForMacro(macroId: Int)

    @Query("SELECT * FROM macro_automation_steps WHERE macroId = :macroId ORDER BY stepOrder ASC")
    suspend fun getAutomationStepsForMacro(macroId: Int): List<MacroAutomationStepEntity>

    @Transaction
    suspend fun saveMacroWithAutomationSteps(
        macro: VoiceMacroEntity,
        steps: List<MacroAutomationStepEntity>
    ): Long {
        val macroId = insertMacro(macro).toInt()
        deleteStepsForMacro(macroId)
        val boundSteps = steps.mapIndexed { idx, step ->
            step.copy(macroId = macroId, stepOrder = idx)
        }
        insertAutomationSteps(boundSteps)
        return macroId.toLong()
    }

    @Query("UPDATE voice_macros SET runCount = runCount + 1, lastExecutedAt = :now WHERE id = :id")
    suspend fun incrementMacroRun(id: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM voice_macros WHERE id = :id")
    suspend fun deleteMacro(id: Int)

    // Smart Home & Cross-Platform Companion Devices
    @Query("SELECT * FROM smart_devices ORDER BY category ASC, id ASC")
    fun observeSmartDevices(): Flow<List<SmartDeviceEntity>>

    @Query("SELECT * FROM smart_devices")
    suspend fun getAllSmartDevicesOnce(): List<SmartDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartDevice(device: SmartDeviceEntity)

    @Update
    suspend fun updateSmartDevice(device: SmartDeviceEntity)

    @Query("DELETE FROM smart_devices WHERE id = :id")
    suspend fun deleteSmartDevice(id: Int)

    // Autonomous Sub-Agents
    @Query("SELECT * FROM sub_agents ORDER BY createdAt DESC")
    fun observeSubAgents(): Flow<List<SubAgentEntity>>

    @Query("SELECT * FROM sub_agents")
    suspend fun getAllSubAgentsOnce(): List<SubAgentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubAgent(agent: SubAgentEntity)

    @Update
    suspend fun updateSubAgent(agent: SubAgentEntity)

    @Query("DELETE FROM sub_agents WHERE id = :id")
    suspend fun deleteSubAgent(id: Int)

    // GitHub Repositories
    @Query("SELECT * FROM github_repos ORDER BY lastSyncedAt DESC")
    fun observeGitHubRepos(): Flow<List<GitHubRepoEntity>>

    @Query("SELECT * FROM github_repos")
    suspend fun getAllGitHubReposOnce(): List<GitHubRepoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGitHubRepo(repo: GitHubRepoEntity)

    @Query("DELETE FROM github_repos WHERE id = :id")
    suspend fun deleteGitHubRepo(id: Int)

    // Shell History
    @Query("SELECT * FROM shell_history ORDER BY timestamp DESC LIMIT 50")
    fun observeShellHistory(): Flow<List<ShellHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShellEntry(entry: ShellHistoryEntity)

    @Query("DELETE FROM shell_history")
    suspend fun clearShellHistory()

    // OpenDots Specialist AI Coworkers ("Dots")
    @Query("SELECT * FROM opendots_coworkers ORDER BY id ASC")
    fun observeOpenDotsCoworkers(): Flow<List<OpenDotsCoworkerEntity>>

    @Query("SELECT * FROM opendots_coworkers")
    suspend fun getAllOpenDotsCoworkersOnce(): List<OpenDotsCoworkerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenDotsCoworker(coworker: OpenDotsCoworkerEntity): Long

    @Update
    suspend fun updateOpenDotsCoworker(coworker: OpenDotsCoworkerEntity)

    @Query("DELETE FROM opendots_coworkers WHERE id = :id")
    suspend fun deleteOpenDotsCoworker(id: Int)

    // OpenDots Spaces & Pages (with Human-in-the-Loop Approval Gates)
    @Query("SELECT * FROM opendots_space_pages ORDER BY CASE WHEN approvalStatus = 'PENDING_HITL' THEN 0 ELSE 1 END, updatedAt DESC")
    fun observeOpenDotsSpacePages(): Flow<List<OpenDotsSpacePageEntity>>

    @Query("SELECT * FROM opendots_space_pages")
    suspend fun getAllOpenDotsSpacePagesOnce(): List<OpenDotsSpacePageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenDotsSpacePage(page: OpenDotsSpacePageEntity): Long

    @Update
    suspend fun updateOpenDotsSpacePage(page: OpenDotsSpacePageEntity)

    @Query("DELETE FROM opendots_space_pages WHERE id = :id")
    suspend fun deleteOpenDotsSpacePage(id: Int)

    // AnyClaw / OpenClaw ClawHub Skills, MCP Tools & OpenMuse Self-Improving Skills
    @Query("SELECT * FROM claw_skills ORDER BY isApproved ASC, usageCount DESC, id ASC")
    fun observeClawSkills(): Flow<List<ClawSkillEntity>>

    @Query("SELECT * FROM claw_skills")
    suspend fun getAllClawSkillsOnce(): List<ClawSkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClawSkill(skill: ClawSkillEntity): Long

    @Update
    suspend fun updateClawSkill(skill: ClawSkillEntity)

    @Query("DELETE FROM claw_skills WHERE id = :id")
    suspend fun deleteClawSkill(id: Int)

    // OpenClaw Multi-Channel Gateway Connectors
    @Query("SELECT * FROM claw_gateway_channels ORDER BY id ASC")
    fun observeClawGatewayChannels(): Flow<List<ClawGatewayChannelEntity>>

    @Query("SELECT * FROM claw_gateway_channels")
    suspend fun getAllClawGatewayChannelsOnce(): List<ClawGatewayChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClawGatewayChannel(channel: ClawGatewayChannelEntity): Long

    @Update
    suspend fun updateClawGatewayChannel(channel: ClawGatewayChannelEntity)

    @Query("DELETE FROM claw_gateway_channels WHERE id = :id")
    suspend fun deleteClawGatewayChannel(id: Int)
}

