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
        ShellHistoryEntity::class
    ],
    version = 2,
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

            if (dao.getAllGitHubReposOnce().isEmpty()) {
                dao.insertGitHubRepo(
                    GitHubRepoEntity(
                        ownerRepo = "ollama/ollama",
                        description = "Get up and running with Llama 3.3, DeepSeek-R1, Phi-4, Gemma 3, and other large language models locally.",
                        defaultBranch = "main",
                        starsCount = 115400,
                        openIssuesCount = 840,
                        language = "Go",
                        automationHook = "Local Model Sync & Runner"
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
