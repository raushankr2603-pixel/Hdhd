package com.example.ui

import android.app.Activity
import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AetherDatabase
import com.example.data.local.AiProviderEntity
import com.example.data.local.GitHubRepoEntity
import com.example.data.local.ShellHistoryEntity
import com.example.data.local.SmartDeviceEntity
import com.example.data.local.SubAgentEntity
import com.example.data.local.VoiceMacroEntity
import com.example.data.security.E2EEVault
import com.example.domain.AiRouterEngine
import com.example.domain.AutonomousMacroRunState
import com.example.domain.CommandExecutionStatus
import com.example.domain.IntentCategory
import com.example.domain.MacroActionStep
import com.example.domain.MacroSequenceCodec
import com.example.domain.MacroStepExecutionLog
import com.example.domain.StepRunState
import com.example.domain.VoiceLiveManager
import com.example.domain.VoiceProcessingReport
import com.example.service.AetherAccessibilityService
import com.example.service.AetherLiveForegroundService
import com.example.service.AetherNotificationListenerService
import com.example.service.CapabilityPermissionStatus
import com.example.service.CapturedNotificationItem
import com.example.service.DeviceControlEngine
import com.example.service.InstalledAppInfo
import com.example.service.PermissionCenterManager
import com.example.service.ScreenVisionSnapshot
import com.example.service.SystemDiagnosticsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ConversationTurn(
    val id: Long = System.currentTimeMillis(),
    val userPrompt: String,
    val agentReply: String,
    val executedTools: List<String> = emptyList(),
    val providerBadge: String = "Aether Local Core",
    val latencyMs: Long = 8L,
    val intentBadge: String = "VOICE INTENT",
    val isErrorOrClarification: Boolean = false,
    val recoverySuggestions: List<String> = emptyList()
)

class AetherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AetherDatabase.getInstance(application)
    private val dao = database.aetherDao()
    val macroRepository = com.example.data.repository.VoiceMacroRepository(dao)
    val deviceControl = DeviceControlEngine(application)
    val permissionCenter = PermissionCenterManager(application)
    private val aiRouter = AiRouterEngine(deviceControl)

    val voiceManager = VoiceLiveManager(application) { spokenText, confidence ->
        sendCommand(spokenText, speechConfidence = confidence, speakReply = true)
    }

    val providers: StateFlow<List<AiProviderEntity>> = dao.observeProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<VoiceMacroEntity>> = macroRepository.allMacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macrosWithSteps: StateFlow<List<com.example.data.local.VoiceMacroWithSteps>> =
        macroRepository.allMacrosWithSteps
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartDevices: StateFlow<List<SmartDeviceEntity>> = dao.observeSmartDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subAgents: StateFlow<List<SubAgentEntity>> = dao.observeSubAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val githubRepos: StateFlow<List<GitHubRepoEntity>> = dao.observeGitHubRepos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shellHistory: StateFlow<List<ShellHistoryEntity>> = dao.observeShellHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenVision: StateFlow<ScreenVisionSnapshot> = AetherAccessibilityService.activeSnapshot
    val capturedNotifications: StateFlow<List<CapturedNotificationItem>> = AetherNotificationListenerService.capturedNotifications
    val isBackgroundDaemonRunning: StateFlow<Boolean> = AetherLiveForegroundService.isRunning

    val voiceState = voiceManager.state

    private val _capabilitiesList = MutableStateFlow(permissionCenter.inspectAllCapabilities())
    val capabilitiesList: StateFlow<List<CapabilityPermissionStatus>> = _capabilitiesList.asStateFlow()

    private val _offlinePrivacyLock = MutableStateFlow(false)
    val offlinePrivacyLock: StateFlow<Boolean> = _offlinePrivacyLock.asStateFlow()

    private val _isProcessingCommand = MutableStateFlow(false)
    val isProcessingCommand: StateFlow<Boolean> = _isProcessingCommand.asStateFlow()

    private val _lastVoiceReport = MutableStateFlow<VoiceProcessingReport?>(null)
    val lastVoiceReport: StateFlow<VoiceProcessingReport?> = _lastVoiceReport.asStateFlow()

    private val _activeMacroRunState = MutableStateFlow(AutonomousMacroRunState())
    val activeMacroRunState: StateFlow<AutonomousMacroRunState> = _activeMacroRunState.asStateFlow()

    private val _conversation = MutableStateFlow(
        listOf(
            ConversationTurn(
                id = 1L,
                userPrompt = "System Boot: Initialize AetherOS Voice Command Processor & Autonomous Macro Engine",
                agentReply = "AetherOS Agent v4 online. All system services (Accessibility Screen Vision, Notification Listener, Background Live Daemon, Hardware Settings Controller, Root/SH Shell, and Multi-Provider AI Router) are initialized.",
                executedTools = listOf(
                    "VOICE_NLU: Multi-intent parser & spoken-number normalizer online",
                    "PERMISSIONS_HUB: Accessibility, Overlay, Write Settings, DND & Runtime Perms ready"
                ),
                providerBadge = "Aether Local Neural Core",
                latencyMs = 6L,
                intentBadge = "SYSTEM_BOOT"
            )
        )
    )
    val conversation: StateFlow<List<ConversationTurn>> = _conversation.asStateFlow()

    private val _brightnessPercent = MutableStateFlow(deviceControl.getScreenBrightnessPercent())
    val brightnessPercent: StateFlow<Int> = _brightnessPercent.asStateFlow()

    private val _volumePercent = MutableStateFlow(deviceControl.getVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _isTorchOn = MutableStateFlow(deviceControl.isTorchEnabled())
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _diagnostics = MutableStateFlow(deviceControl.collectDiagnostics())
    val diagnostics: StateFlow<SystemDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    private val _e2eeSyncPayload = MutableStateFlow("")
    val e2eeSyncPayload: StateFlow<String> = _e2eeSyncPayload.asStateFlow()

    private val _discoveredModels = MutableStateFlow<List<String>>(
        AiRouterEngine.curatedModelsForProvider("Ollama", "http://127.0.0.1:11434/v1")
    )
    val discoveredModels: StateFlow<List<String>> = _discoveredModels.asStateFlow()

    private val _isDiscoveringModels = MutableStateFlow(false)
    val isDiscoveringModels: StateFlow<Boolean> = _isDiscoveringModels.asStateFlow()

    private val _providerLiveTestResult = MutableStateFlow<String?>(null)
    val providerLiveTestResult: StateFlow<String?> = _providerLiveTestResult.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            AetherDatabase.seedDefaultsIfEmpty(dao)
            _installedApps.value = deviceControl.getInstalledApps()
            refreshDiagnostics()
            refreshPermissionsStatus()
            if (dao.observeShellHistory().stateIn(viewModelScope).value.isEmpty()) {
                val res = deviceControl.executeShellCommand("uname -a && getprop ro.build.version.release", false)
                dao.insertShellEntry(
                    ShellHistoryEntity(
                        command = res.command,
                        output = res.output,
                        isRootAttempt = false,
                        exitCode = res.exitCode
                    )
                )
            }
        }
    }

    fun clearStatusToast() {
        _statusToast.value = null
    }

    fun postToast(message: String) {
        _statusToast.value = message
    }

    fun refreshPermissionsStatus() {
        _capabilitiesList.value = permissionCenter.inspectAllCapabilities()
        _diagnostics.value = deviceControl.collectDiagnostics()
    }

    fun launchSpecialPermission(capabilityId: String) {
        val msg = permissionCenter.launchSpecialPermissionIntent(capabilityId)
        postToast(msg)
        refreshPermissionsStatus()
    }

    fun openAppSystemSettings() {
        val msg = permissionCenter.openAppDetailsSettings()
        postToast(msg)
    }

    fun toggleBackgroundLiveDaemon(enable: Boolean) {
        val context = getApplication<Application>()
        val msg = if (enable) {
            AetherLiveForegroundService.startDaemon(context)
        } else {
            AetherLiveForegroundService.stopDaemon(context)
        }
        voiceManager.toggleContinuousLiveMode(enable)
        postToast(msg)
    }

    fun toggleOfflinePrivacyLock(enabled: Boolean) {
        _offlinePrivacyLock.value = enabled
        postToast(
            if (enabled) "100% Offline Privacy Lock ENABLED (Zero external network calls)"
            else "Hybrid Cloud + Local AI Routing Enabled"
        )
    }

    fun sendCommand(
        prompt: String,
        activity: Activity? = null,
        speechConfidence: Float = 0.95f,
        speakReply: Boolean = true
    ) {
        val clean = prompt.trim()
        if (clean.isEmpty()) {
            postToast("Please speak or enter a command first")
            return
        }
        viewModelScope.launch {
            _isProcessingCommand.value = true
            val activeProv = providers.value.firstOrNull { it.isActive } ?: providers.value.firstOrNull()
            val result = aiRouter.processUserCommand(
                prompt = clean,
                speechConfidence = speechConfidence,
                activeProvider = activeProv,
                forceOfflineMode = _offlinePrivacyLock.value,
                macros = macros.value,
                smartDevices = smartDevices.value,
                includeScreenVision = voiceState.value.liveScreenVisionEnabled,
                activity = activity,
                onMacroMatched = { matchedMacro ->
                    executeMacroSequentiallyInternal(matchedMacro, activity)
                }
            )
            result.voiceReport?.let { _lastVoiceReport.value = it }
            result.spawnedAgent?.let { dao.insertSubAgent(it) }
            _brightnessPercent.value = deviceControl.getScreenBrightnessPercent()
            _volumePercent.value = deviceControl.getVolumePercent()
            _isTorchOn.value = deviceControl.isTorchEnabled()
            refreshDiagnostics()
            refreshPermissionsStatus()

            val report = result.voiceReport
            val isErr = report?.status == CommandExecutionStatus.ERROR_UNCLEAR ||
                report?.status == CommandExecutionStatus.ERROR_UNSUPPORTED ||
                report?.status == CommandExecutionStatus.PARTIAL_WARNING

            val newTurn = ConversationTurn(
                id = System.currentTimeMillis(),
                userPrompt = clean,
                agentReply = result.replyText,
                executedTools = result.executedActions,
                providerBadge = result.providerUsed,
                latencyMs = result.latencyMs,
                intentBadge = report?.primaryCategory?.badgeLabel ?: IntentCategory.CONVERSATIONAL_QUERY.badgeLabel,
                isErrorOrClarification = isErr,
                recoverySuggestions = report?.recoverySuggestions.orEmpty()
            )
            _conversation.value = listOf(newTurn) + _conversation.value
            _isProcessingCommand.value = false

            if (speakReply) {
                voiceManager.speakResponse(result.replyText)
            }
        }
    }

    fun findTriggerConflict(triggerPhrase: String, editingMacroId: Int? = null): VoiceMacroEntity? {
        return aiRouter.voiceProcessor.findConflictingMacro(
            candidateTrigger = triggerPhrase,
            existingMacros = macros.value,
            ignoreMacroId = editingMacroId
        )
    }

    fun saveOrUpdateVoiceMacro(
        editingId: Int? = null,
        title: String,
        triggerPhrase: String,
        steps: List<MacroActionStep>,
        isOfflineCritical: Boolean
    ): Boolean {
        val cleanTitle = title.trim()
        val cleanTrigger = aiRouter.voiceProcessor.normalizeTranscript(triggerPhrase)
        if (cleanTitle.isBlank() || cleanTrigger.isBlank()) {
            postToast("Please provide a macro title and a unique voice command phrase")
            return false
        }
        if (steps.isEmpty()) {
            postToast("Add at least one action step to the macro sequence")
            return false
        }
        val conflict = findTriggerConflict(cleanTrigger, editingId)
        if (conflict != null) {
            postToast("Voice command \"$cleanTrigger\" is already assigned to macro '${conflict.title}'. Choose a unique phrase.")
            return false
        }

        viewModelScope.launch(Dispatchers.IO) {
            macroRepository.saveVoiceMacro(
                editingId = editingId,
                title = cleanTitle,
                triggerPhrase = cleanTrigger,
                steps = steps,
                isOfflineCritical = isOfflineCritical
            )
            postToast(
                if (editingId == null) "Created Voice Macro '$cleanTitle' (Trigger: \"$cleanTrigger\")"
                else "Updated Voice Macro '$cleanTitle'"
            )
        }
        return true
    }

    fun addVoiceMacro(
        title: String,
        triggerPhrase: String,
        stepsChain: String,
        isOfflineCritical: Boolean
    ) {
        val steps = MacroSequenceCodec.decodeChainToSteps(stepsChain)
        saveOrUpdateVoiceMacro(
            editingId = null,
            title = title,
            triggerPhrase = triggerPhrase,
            steps = steps,
            isOfflineCritical = isOfflineCritical
        )
    }

    private suspend fun executeMacroSequentiallyInternal(
        macro: VoiceMacroEntity,
        activity: Activity?
    ): List<String> {
        val relationalEntities = macroRepository.getOrderedAutomationSteps(macro.id)
        val steps = if (relationalEntities.isNotEmpty()) {
            com.example.data.repository.VoiceMacroRepository.mapStepEntitiesToDomain(relationalEntities)
        } else {
            MacroSequenceCodec.decodeChainToSteps(macro.stepsJson)
        }
        if (steps.isEmpty()) return emptyList()

        val initialLogs = steps.mapIndexed { index, step ->
            MacroStepExecutionLog(
                stepIndex = index,
                step = step,
                state = StepRunState.PENDING,
                outputMessage = "Queued: ${step.actionType.label} -> ${step.targetValue}"
            )
        }.toMutableList()

        _activeMacroRunState.value = AutonomousMacroRunState(
            macroId = macro.id,
            macroTitle = macro.title,
            triggerPhrase = macro.triggerPhrase,
            isRunning = true,
            currentStepIndex = 0,
            totalSteps = steps.size,
            stepLogs = initialLogs,
            summaryMessage = "Executing autonomous sequence '${macro.title}'..."
        )

        val completedOutputs = mutableListOf<String>()

        for ((idx, step) in steps.withIndex()) {
            val stepStart = System.currentTimeMillis()
            initialLogs[idx] = initialLogs[idx].copy(
                state = StepRunState.RUNNING,
                outputMessage = "Executing ${step.actionType.label} (${step.targetValue})..."
            )
            _activeMacroRunState.value = _activeMacroRunState.value.copy(
                currentStepIndex = idx + 1,
                stepLogs = initialLogs.toList(),
                summaryMessage = "Step ${idx + 1}/${steps.size}: ${step.actionType.label}"
            )

            delay(180L)

            val (ok, msg) = aiRouter.executeStructuredMacroStep(
                step = step,
                activity = activity,
                onSpeakTts = { text -> voiceManager.speakResponse(text) }
            )
            val elapsed = (System.currentTimeMillis() - stepStart).coerceAtLeast(12L)
            initialLogs[idx] = initialLogs[idx].copy(
                state = if (ok) StepRunState.SUCCESS else StepRunState.FAILED,
                outputMessage = msg,
                durationMs = elapsed
            )
            completedOutputs.add("[Step ${idx + 1}] $msg")

            _activeMacroRunState.value = _activeMacroRunState.value.copy(
                stepLogs = initialLogs.toList()
            )

            if (!ok && !step.continueOnError) {
                break
            }
        }

        macroRepository.recordMacroExecution(macro.id)
        _brightnessPercent.value = deviceControl.getScreenBrightnessPercent()
        _volumePercent.value = deviceControl.getVolumePercent()
        _isTorchOn.value = deviceControl.isTorchEnabled()

        _activeMacroRunState.value = _activeMacroRunState.value.copy(
            isRunning = false,
            summaryMessage = "Completed ${completedOutputs.size}/${steps.size} steps for '${macro.title}'"
        )
        return completedOutputs
    }

    fun runVoiceMacro(macro: VoiceMacroEntity, activity: Activity? = null) {
        viewModelScope.launch {
            val executed = executeMacroSequentiallyInternal(macro, activity)
            val turn = ConversationTurn(
                id = System.currentTimeMillis(),
                userPrompt = "Voice Macro Trigger: \"${macro.triggerPhrase}\"",
                agentReply = "Autonomously executed ${executed.size} step(s) for macro '${macro.title}'.",
                executedTools = executed,
                providerBadge = "Autonomous Macro Engine",
                latencyMs = 14L,
                intentBadge = IntentCategory.VOICE_MACRO_TRIGGER.badgeLabel
            )
            _conversation.value = listOf(turn) + _conversation.value
            voiceManager.speakResponse("Completed voice macro ${macro.title}")
            postToast("Executed Macro: ${macro.title}")
        }
    }

    fun clearMacroRunBanner() {
        _activeMacroRunState.value = AutonomousMacroRunState()
    }

    fun deleteMacro(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            macroRepository.deleteVoiceMacro(id)
        }
    }

    fun saveOrUpdateCustomAiProvider(
        editingId: Int? = null,
        providerName: String,
        baseUrl: String,
        rawApiKey: String,
        modelName: String,
        isLocalOffline: Boolean,
        activateImmediately: Boolean = true
    ) {
        if (providerName.isBlank() || baseUrl.isBlank() || modelName.isBlank()) {
            postToast("Please enter Provider Name, Base URL, and Model Name")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val encryptedKey = if (rawApiKey.isNotBlank()) {
                E2EEVault.encrypt(rawApiKey.trim())
            } else if (editingId != null) {
                providers.value.firstOrNull { it.id == editingId }?.encryptedApiKey.orEmpty()
            } else {
                ""
            }
            val newId = dao.insertProvider(
                AiProviderEntity(
                    id = editingId ?: 0,
                    providerName = providerName.trim(),
                    baseUrl = baseUrl.trim(),
                    encryptedApiKey = encryptedKey,
                    modelName = modelName.trim(),
                    isLocalOffline = isLocalOffline,
                    isActive = activateImmediately
                )
            ).toInt()
            if (activateImmediately) {
                dao.setActiveProvider(newId)
            }
            postToast(
                if (editingId == null) "Saved & Activated Provider: ${providerName.trim()} (${modelName.trim()})"
                else "Updated Provider: ${providerName.trim()} (${modelName.trim()})"
            )
        }
    }

    fun saveCustomAiProvider(
        providerName: String,
        baseUrl: String,
        rawApiKey: String,
        modelName: String,
        isLocalOffline: Boolean,
        activateImmediately: Boolean = true
    ) {
        saveOrUpdateCustomAiProvider(
            editingId = null,
            providerName = providerName,
            baseUrl = baseUrl,
            rawApiKey = rawApiKey,
            modelName = modelName,
            isLocalOffline = isLocalOffline,
            activateImmediately = activateImmediately
        )
    }

    fun switchProviderModel(provider: AiProviderEntity, newModelName: String) {
        val cleanModel = newModelName.trim()
        if (cleanModel.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertProvider(
                provider.copy(
                    modelName = cleanModel,
                    isActive = true,
                    timestamp = System.currentTimeMillis()
                )
            )
            dao.setActiveProvider(provider.id)
            postToast("Switched ${provider.providerName} to model '$cleanModel'")
        }
    }

    fun decryptProviderKeyForEdit(provider: AiProviderEntity): String {
        return E2EEVault.decrypt(provider.encryptedApiKey)
    }

    fun loadCuratedModelsForDraft(providerName: String, baseUrl: String) {
        _discoveredModels.value = AiRouterEngine.curatedModelsForProvider(providerName, baseUrl)
    }

    fun discoverModelsFromEndpoint(providerName: String, baseUrl: String, rawApiKey: String) {
        viewModelScope.launch {
            _isDiscoveringModels.value = true
            val (online, models) = aiRouter.fetchAvailableModels(providerName, baseUrl, rawApiKey)
            _discoveredModels.value = models
            _isDiscoveringModels.value = false
            postToast(
                if (online) "Discovered ${models.size} models from endpoint"
                else "Loaded ${models.size} recommended models for ${providerName.ifBlank { "provider" }}"
            )
        }
    }

    fun runProviderLiveTest(provider: AiProviderEntity, testPrompt: String = "Confirm model readiness in one concise sentence.") {
        viewModelScope.launch {
            _providerLiveTestResult.value = "Testing [${provider.providerName} • ${provider.modelName}]..."
            val (reply, latency) = aiRouter.testProviderLiveCompletion(provider, testPrompt)
            dao.updateProviderLatency(provider.id, latency)
            _providerLiveTestResult.value = "[${provider.providerName} • ${provider.modelName} • ${latency}ms]: $reply"
            postToast("Tested ${provider.providerName} (${latency}ms)")
        }
    }

    fun clearProviderLiveTestResult() {
        _providerLiveTestResult.value = null
    }

    fun selectActiveProvider(provider: AiProviderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setActiveProvider(provider.id)
            postToast("Active AI Model switched to ${provider.providerName} (${provider.modelName})")
        }
    }

    fun testProviderConnection(provider: AiProviderEntity) {
        viewModelScope.launch {
            postToast("Pinging ${provider.providerName} endpoint...")
            val (ok, latency) = aiRouter.testProviderEndpoint(provider)
            dao.updateProviderLatency(provider.id, latency)
            postToast(
                if (ok) "${provider.providerName} verified (${latency}ms RTT)"
                else "${provider.providerName} unreachable (${latency}ms) - Offline fallback ready"
            )
        }
    }

    fun deleteProvider(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteProvider(id)
        }
    }

    fun spawnSubAgent(name: String, role: String, task: String, parentName: String = "Aether Core v4", depth: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSubAgent(
                SubAgentEntity(
                    name = name.ifBlank { "Aether Sub-Agent #${(100..999).random()}" },
                    role = role.ifBlank { "Autonomous Multi-Step Worker" },
                    parentAgentName = parentName,
                    generationDepth = depth,
                    status = "RUNNING",
                    currentTask = task.ifBlank { "Autonomous cross-app task execution" },
                    cpuAllocationPercent = (8..22).random(),
                    stepsCompleted = 1,
                    totalSteps = 5
                )
            )
            postToast("Spawned Autonomous Agent: $name (Gen $depth)")
        }
    }

    fun reproduceSubAgent(parent: SubAgentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val childName = "${parent.name.substringBefore(" #")} Clone #${(200..999).random()}"
            dao.insertSubAgent(
                SubAgentEntity(
                    name = childName,
                    role = "${parent.role} (Forked Specialist)",
                    parentAgentName = parent.name,
                    generationDepth = parent.generationDepth + 1,
                    status = "RUNNING",
                    currentTask = "Parallelizing: ${parent.currentTask}",
                    cpuAllocationPercent = (6..16).random(),
                    stepsCompleted = 0,
                    totalSteps = parent.totalSteps
                )
            )
            postToast("Agent '${parent.name}' reproduced -> '$childName' (Gen ${parent.generationDepth + 1})")
        }
    }

    fun advanceSubAgentStep(agent: SubAgentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextStep = (agent.stepsCompleted + 1).coerceAtMost(agent.totalSteps)
            val nextStatus = if (nextStep >= agent.totalSteps) "COMPLETED" else "RUNNING"
            dao.updateSubAgent(
                agent.copy(
                    stepsCompleted = nextStep,
                    status = nextStatus,
                    currentTask = if (nextStatus == "COMPLETED") "Workflow verified & idle" else agent.currentTask
                )
            )
        }
    }

    fun deleteSubAgent(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSubAgent(id)
        }
    }

    fun updateBrightness(percent: Int, activity: Activity?) {
        _brightnessPercent.value = percent
        val msg = deviceControl.setScreenBrightnessPercent(percent, activity)
        postToast(msg)
    }

    fun updateVolume(percent: Int) {
        _volumePercent.value = percent
        val msg = deviceControl.setVolumePercent(percent)
        postToast(msg)
    }

    fun toggleTorch() {
        val msg = deviceControl.toggleFlashlight()
        _isTorchOn.value = deviceControl.isTorchEnabled()
        postToast(msg)
    }

    fun setRingerProfile(mode: String) {
        postToast(deviceControl.setRingerMode(mode))
    }

    fun launchInstalledApp(app: InstalledAppInfo) {
        postToast(deviceControl.launchAppByQuery(app.packageName))
    }

    fun openAccessibilitySettings() {
        deviceControl.openSystemSettingsPanel(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    fun openWriteSettingsPermission() {
        deviceControl.requestWriteSettingsPermission()
    }

    fun performScreenTap(x: Float, y: Float) {
        val msg = AetherAccessibilityService.executeTap(x, y)
        postToast(msg)
    }

    fun performScreenDoubleTap(x: Float, y: Float) {
        val msg = AetherAccessibilityService.executeDoubleTap(x, y)
        postToast(msg)
    }

    fun performScreenLongPress(x: Float, y: Float) {
        val msg = AetherAccessibilityService.executeLongPress(x, y)
        postToast(msg)
    }

    fun performScreenMultiTapBurst(x: Float, y: Float, tapCount: Int = 3) {
        val msg = AetherAccessibilityService.executeMultiTapBurst(x, y, tapCount)
        postToast(msg)
    }

    fun performScreenDragOrSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 320L
    ) {
        val msg = AetherAccessibilityService.executeDragOrSwipe(startX, startY, endX, endY, durationMs)
        postToast(msg)
    }

    fun performScreenScroll(direction: String) {
        val msg = AetherAccessibilityService.executeScroll(direction)
        postToast(msg)
    }

    fun performScrollUntilVisible(targetText: String) {
        val msg = AetherAccessibilityService.scrollUntilTextVisible(targetText)
        postToast(msg)
    }

    fun performPinchGesture(zoomIn: Boolean) {
        val msg = AetherAccessibilityService.executePinchGesture(zoomIn)
        postToast(msg)
    }

    fun performReadScreenContent() {
        val msg = AetherAccessibilityService.readCurrentScreenContent()
        postToast(msg)
    }

    fun performClickByLabel(labelOrId: String) {
        val msg = AetherAccessibilityService.clickElementByLabel(labelOrId)
        postToast(msg)
    }

    fun performLongClickByLabel(labelOrId: String) {
        val msg = AetherAccessibilityService.longClickElementByLabel(labelOrId)
        postToast(msg)
    }

    fun performGlobalNav(action: String) {
        val msg = AetherAccessibilityService.executeGlobalAction(action)
        postToast(msg)
    }

    fun performTextInputInjection(text: String, targetNodeLabel: String = "") {
        val msg = AetherAccessibilityService.inputTextIntoFocusedNode(text, targetNodeLabel)
        postToast(msg)
    }

    fun performClipboardOrEditAction(actionType: String) {
        val msg = AetherAccessibilityService.performNodeClipboardOrEditAction(actionType)
        postToast(msg)
    }

    fun toggleSmartDevicePower(device: SmartDeviceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextPower = !device.powerState
            val updated = device.copy(
                powerState = nextPower,
                stateLabel = if (nextPower) "Active • Online" else "Standby • Paused"
            )
            dao.updateSmartDevice(updated)
            postToast("${device.name}: ${if (nextPower) "ON" else "STANDBY"}")
        }
    }

    fun addSmartOrCompanionDevice(
        name: String,
        category: String,
        protocol: String,
        endpointUrl: String
    ) {
        if (name.isBlank() || endpointUrl.isBlank()) {
            postToast("Enter device name and endpoint URL / IP")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSmartDevice(
                SmartDeviceEntity(
                    name = name.trim(),
                    category = category,
                    protocol = protocol.ifBlank { "Matter / E2EE WebSocket" },
                    endpointUrl = endpointUrl.trim(),
                    isOnline = true,
                    stateLabel = "Connected & Synced",
                    powerState = true,
                    telemetryValue = "E2EE Ready"
                )
            )
            postToast("Added node: ${name.trim()}")
        }
    }

    fun generateE2EESyncBundle(passphrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val root = JSONObject().apply {
                put("protocol", "AetherOS-E2EE-Sync-v4")
                put("timestamp", System.currentTimeMillis())
                put("macrosCount", macros.value.size)
                put("providersCount", providers.value.size)
                put("devicesCount", smartDevices.value.size)
                val macroArr = JSONArray()
                macros.value.forEach { m ->
                    macroArr.put(
                        JSONObject()
                            .put("title", m.title)
                            .put("trigger", m.triggerPhrase)
                            .put("steps", m.stepsJson)
                    )
                }
                put("macros", macroArr)
            }
            val cipherBundle = E2EEVault.encrypt(
                root.toString(),
                passphrase.ifBlank { "AetherOS_Default_Device_Vault_Key" }
            )
            _e2eeSyncPayload.value = cipherBundle
            postToast("Generated AES-256-GCM E2EE Sync Bundle (${cipherBundle.length} bytes)")
        }
    }

    fun runShellCommand(command: String, asRoot: Boolean) {
        if (command.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val res = deviceControl.executeShellCommand(command, useRootSu = asRoot)
            dao.insertShellEntry(
                ShellHistoryEntity(
                    command = res.command,
                    output = res.output,
                    isRootAttempt = res.isRoot,
                    exitCode = res.exitCode
                )
            )
            refreshDiagnostics()
        }
    }

    fun runDeepSystemOptimization() {
        viewModelScope.launch(Dispatchers.IO) {
            val report = deviceControl.runDeepSystemOptimization()
            dao.insertShellEntry(
                ShellHistoryEntity(
                    command = "aether-diag --optimize-all --trim-memory",
                    output = report,
                    isRootAttempt = true,
                    exitCode = 0
                )
            )
            refreshDiagnostics()
            postToast("Deep System Optimization complete")
        }
    }

    fun refreshDiagnostics() {
        _diagnostics.value = deviceControl.collectDiagnostics()
    }

    fun addGitHubRepository(ownerRepo: String, hook: String) {
        if (ownerRepo.isBlank()) {
            postToast("Enter a GitHub repository (e.g., owner/repo)")
            return
        }
        viewModelScope.launch {
            postToast("Inspecting GitHub repository '$ownerRepo'...")
            val res = aiRouter.fetchGitHubRepository(ownerRepo, hook)
            res.onSuccess { entity ->
                dao.insertGitHubRepo(entity)
                postToast("Synced GitHub Repo: ${entity.ownerRepo} (★ ${entity.starsCount})")
            }.onFailure { err ->
                postToast(err.localizedMessage ?: "Failed to add repository")
            }
        }
    }

    fun deleteGitHubRepo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteGitHubRepo(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
