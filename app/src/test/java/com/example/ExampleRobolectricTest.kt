package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AetherDatabase
import com.example.data.local.VoiceMacroEntity
import com.example.data.repository.VoiceMacroRepository
import com.example.data.security.E2EEVault
import com.example.domain.AiRouterEngine
import com.example.domain.CommandExecutionStatus
import com.example.domain.IntentCategory
import com.example.domain.MacroActionStep
import com.example.domain.MacroSequenceCodec
import com.example.domain.MacroStepActionType
import com.example.domain.VoiceCommandProcessor
import com.example.service.AetherAccessibilityService
import com.example.service.DeviceControlEngine
import com.example.service.PermissionCenterManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameAndE2EEVaultRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AetherOS Agent", appName)

        val secretApiKey = "sk-openrouter-v1-9988776655"
        val encrypted = E2EEVault.encrypt(secretApiKey, "user-vault-passphrase")
        val decrypted = E2EEVault.decrypt(encrypted, "user-vault-passphrase")
        assertEquals(secretApiKey, decrypted)
        assertTrue(encrypted != secretApiKey)
    }

    @Test
    fun verifyVoiceCommandProcessorAndMacroSequencer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceControl = DeviceControlEngine(context)
        val processor = VoiceCommandProcessor(deviceControl)

        val normalized = processor.normalizeTranscript("Hey Aether please set brightness eighty percent and volume fifty")
        assertTrue(normalized.contains("80"))
        assertTrue(normalized.contains("50"))

        val steps = listOf(
            MacroActionStep(actionType = MacroStepActionType.SET_BRIGHTNESS, targetValue = "85"),
            MacroActionStep(actionType = MacroStepActionType.OPEN_APP, targetValue = "Settings"),
            MacroActionStep(actionType = MacroStepActionType.SCROLL_SCREEN, targetValue = "DOWN")
        )
        val chain = MacroSequenceCodec.encodeStepsToChain(steps)
        val decoded = MacroSequenceCodec.decodeChainToSteps(chain)
        assertEquals(3, decoded.size)
        assertEquals(MacroStepActionType.SET_BRIGHTNESS, decoded[0].actionType)
        assertEquals("Settings", decoded[1].targetValue)

        val sampleMacros = listOf(
            VoiceMacroEntity(
                id = 10,
                title = "Cinema Protocol",
                triggerPhrase = "activate cinema mode",
                stepsJson = chain
            )
        )
        val conflict = processor.findConflictingMacro("Hey Aether activate cinema mode", sampleMacros)
        assertNotNull(conflict)

        val macroReport = processor.processAndExecuteVoiceCommand(
            rawUtterance = "please activate cinema mode",
            macros = sampleMacros,
            smartDevices = emptyList()
        )
        assertEquals(IntentCategory.VOICE_MACRO_TRIGGER, macroReport.primaryCategory)
        assertNotNull(macroReport.triggeredMacro)

        val unclearReport = processor.processAndExecuteVoiceCommand(
            rawUtterance = "set brightness",
            macros = sampleMacros,
            smartDevices = emptyList()
        )
        assertEquals(CommandExecutionStatus.ERROR_UNCLEAR, unclearReport.status)
        assertTrue(unclearReport.recoverySuggestions.isNotEmpty())

        val unsupportedReport = processor.processAndExecuteVoiceCommand(
            rawUtterance = "turn on airplane mode",
            macros = sampleMacros,
            smartDevices = emptyList()
        )
        assertEquals(CommandExecutionStatus.ERROR_UNSUPPORTED, unsupportedReport.status)
        assertTrue(unsupportedReport.recoverySuggestions.isNotEmpty())
    }

    @Test
    fun verifyRoomMacroSchemaDaoAndAccessibilityServiceGestures() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AetherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.aetherDao()
        val repo = VoiceMacroRepository(dao)

        val uiSteps = listOf(
            MacroActionStep(actionType = MacroStepActionType.READ_SCREEN, targetValue = "SUMMARY"),
            MacroActionStep(actionType = MacroStepActionType.CLICK_UI_TEXT, targetValue = "Live Voice Orb"),
            MacroActionStep(actionType = MacroStepActionType.TAP_COORDINATES, targetValue = "540,1150"),
            MacroActionStep(actionType = MacroStepActionType.SCROLL_SCREEN, targetValue = "DOWN"),
            MacroActionStep(actionType = MacroStepActionType.GLOBAL_NAVIGATION, targetValue = "BACK")
        )

        val savedId = repo.saveVoiceMacro(
            editingId = null,
            title = "Screen Audit & Navigate",
            triggerPhrase = "audit active screen",
            steps = uiSteps,
            isOfflineCritical = true
        ).toInt()

        val withSteps = repo.getMacroWithSteps(savedId)
        assertNotNull(withSteps)
        assertEquals("Screen Audit & Navigate", withSteps?.macro?.title)
        assertEquals(5, withSteps?.orderedSteps?.size)

        val byTrigger = repo.findMacroByTrigger("audit active screen")
        assertNotNull(byTrigger)
        assertEquals(5, byTrigger?.orderedSteps?.size)
        assertEquals("READ_SCREEN", byTrigger?.orderedSteps?.get(0)?.actionType)
        assertEquals("CLICK_TEXT", byTrigger?.orderedSteps?.get(1)?.actionType)

        // Verify AccessibilityService screen reading & autonomous gesture execution
        val screenSummary = AetherAccessibilityService.readCurrentScreenContent()
        assertTrue(screenSummary.contains("Clickable Targets"))

        val clickLog = AetherAccessibilityService.clickElementByLabel("Live Voice Orb")
        assertTrue(clickLog.contains("Tap") || clickLog.contains("Clicked"))

        val scrollLog = AetherAccessibilityService.executeScroll("DOWN")
        assertTrue(scrollLog.contains("DOWN"))

        val pinchLog = AetherAccessibilityService.executePinchGesture(true)
        assertTrue(pinchLog.contains("PINCH"))

        val globalLog = AetherAccessibilityService.executeGlobalAction("BACK")
        assertTrue(globalLog.contains("BACK"))

        val stepEntityLog = AetherAccessibilityService.executeAutomationStepEntity(
            byTrigger!!.orderedSteps[2]
        )
        assertTrue(stepEntityLog.contains("540"))

        db.close()
    }

    @Test
    fun verifyAiProviderConfigurationAndRealTimeScreenInteractions() {
        val ollamaModels = AiRouterEngine.curatedModelsForProvider("Ollama Local Server", "http://127.0.0.1:11434/v1")
        assertTrue(ollamaModels.contains("llama3.2:3b"))

        val nvidiaModels = AiRouterEngine.curatedModelsForProvider("NVIDIA NIM Cloud", "https://integrate.api.nvidia.com/v1")
        assertTrue(nvidiaModels.any { it.contains("nvidia") || it.contains("meta") })

        val openRouterModels = AiRouterEngine.curatedModelsForProvider("OpenRouter Unified API", "https://openrouter.ai/api/v1")
        assertTrue(openRouterModels.any { it.contains("anthropic") || it.contains("google") })

        val groqModels = AiRouterEngine.curatedModelsForProvider("Groq Cloud LPU", "https://api.groq.com/openai/v1")
        assertTrue(groqModels.contains("llama-3.3-70b-versatile"))

        val openAiModels = AiRouterEngine.curatedModelsForProvider("OpenAI API", "https://api.openai.com/v1")
        assertTrue(openAiModels.contains("gpt-4o-mini"))

        // Verify real-time screen interactions (double-tap, burst tap, drag/swipe vector, clipboard, scroll-until-visible)
        val doubleTapLog = AetherAccessibilityService.executeDoubleTap(540f, 1180f)
        assertTrue(doubleTapLog.contains("Double-Tap"))

        val burstLog = AetherAccessibilityService.executeMultiTapBurst(540f, 1180f, tapCount = 3)
        assertTrue(burstLog.contains("Multi-Tap Burst"))

        val dragLog = AetherAccessibilityService.executeDragOrSwipe(540f, 1650f, 540f, 420f, 360L)
        assertTrue(dragLog.contains("Drag/Swipe"))

        val clipboardLog = AetherAccessibilityService.performNodeClipboardOrEditAction("COPY")
        assertTrue(clipboardLog.contains("COPY"))

        val scrollFindLog = AetherAccessibilityService.scrollUntilTextVisible("Live Voice Orb", maxSwipes = 2)
        assertTrue(scrollFindLog.contains("Live Voice Orb"))
    }

    @Test
    fun verifyOpenMuseAnyClawOpenDotsAndTermuxEcosystem() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceControl = DeviceControlEngine(context)
        val termuxBridge = com.example.service.TermuxBridgeEngine(context)
        val openEcosystem = com.example.domain.OpenAgentEcosystemEngine(context, deviceControl, termuxBridge)

        // 1. Verify Termux & 6 Sub-Dependencies Inspection + Real POSIX Script Execution
        val termuxSnap = termuxBridge.inspectEnvironment()
        assertEquals(7, termuxSnap.plugins.size)
        assertTrue(termuxSnap.plugins.any { it.packageName == "com.termux" })
        assertTrue(termuxSnap.plugins.any { it.packageName == "com.termux.api" })
        assertTrue(termuxSnap.plugins.any { it.packageName == "com.termux.x11" })

        val savedScript = termuxBridge.saveUserlandScript("test_echo.sh", "#!/system/bin/sh\necho 'AETHER_POSIX_OK'")
        assertTrue(java.io.File(savedScript.absolutePath).exists())

        val execOutcome = termuxBridge.executeCommand("echo 'TERMUX_USERLAND_VERIFIED'", dispatchToTermuxService = false)
        assertTrue(execOutcome.stdoutAndStderr.contains("TERMUX_USERLAND_VERIFIED"))

        // 2. Verify OpenClaw / AnyClaw Real On-Disk Workspace Files (MEMORY.md, SOUL.md, TOOLS.md)
        val workspaceFiles = openEcosystem.listWorkspaceMarkdownFiles()
        assertTrue(workspaceFiles.any { it.fileName == "MEMORY.md" })
        assertTrue(workspaceFiles.any { it.fileName == "SOUL.md" })
        assertTrue(workspaceFiles.any { it.fileName == "TOOLS.md" })

        val updatedMem = openEcosystem.appendFactToMemoryMd("Verified OpenMuse + AnyClaw + OpenDots integration")
        assertTrue(updatedMem.content.contains("Verified OpenMuse + AnyClaw + OpenDots integration"))

        // 3. Verify OpenDots Specialist Coworker ("Dot") Task Execution & HITL Space Page Persistence on Disk
        val dot = com.example.data.local.OpenDotsCoworkerEntity(
            id = 1,
            dotName = "Atlas • Deep Researcher Dot",
            roleTitle = "Researcher",
            systemInstructions = "Draft engineering briefs",
            sandboxEnvironment = "OpenBot Chromium + Termux",
            allowedToolsCsv = "BROWSER_DOM,TERMUX_SHELL,SPACES_WRITE",
            assignedSpaceName = "Engineering & Research"
        )
        val draftedPage = openEcosystem.executeOpenDotCoworkerTask(dot, "Verify AG-UI and Termux bridge")
        assertEquals("PENDING_HITL", draftedPage.approvalStatus)
        assertTrue(draftedPage.markdownContent.contains("Verify AG-UI and Termux bridge"))

        val diskPath = openEcosystem.writeSpacePageToDisk(draftedPage.copy(approvalStatus = "APPROVED"))
        assertTrue(java.io.File(diskPath).exists())

        // 4. Verify OpenMuse Manual Takeover Toggle & AG-UI Event Stream
        openEcosystem.setBrowserManualTakeover(true)
        assertTrue(openEcosystem.browserState.value.isManualTakeoverActive)
        assertTrue(openEcosystem.agUiEvents.value.isNotEmpty())
    }

    @Test
    fun verifyPermissionCenterInspectsAllCapabilities() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissionManager = PermissionCenterManager(context)
        val capabilities = permissionManager.inspectAllCapabilities()
        assertEquals(13, capabilities.size)
        assertTrue(capabilities.any { it.id == "accessibility_service" })
        assertTrue(capabilities.any { it.id == "write_settings" })
        assertTrue(capabilities.any { it.id == "system_overlay" })
        assertTrue(capabilities.any { it.id == "notification_listener" })
        assertTrue(capabilities.any { it.id == "termux_run_command" })
    }
}


