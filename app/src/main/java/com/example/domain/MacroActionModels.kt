package com.example.domain

import org.json.JSONArray
import org.json.JSONObject

enum class MacroStepCategory(val displayName: String) {
    LAUNCH_APP("Open Application"),
    IN_APP_ACTION("In-App UI Action"),
    ADJUST_SETTING("Adjust Phone Setting"),
    WAIT_DELAY("Wait / Delay"),
    SMART_HOME("Smart Home Node"),
    SHELL_SCRIPT("Shell Command"),
    TTS_ANNOUNCE("Voice Announcement")
}

enum class MacroStepActionType(
    val category: MacroStepCategory,
    val code: String,
    val label: String,
    val paramHint: String
) {
    OPEN_APP(MacroStepCategory.LAUNCH_APP, "LAUNCH_APP", "Launch App by Name/Package", "e.g. Settings, Chrome, Calculator"),
    READ_SCREEN(MacroStepCategory.IN_APP_ACTION, "READ_SCREEN", "Read Active Screen Content", "e.g. SUMMARY or ALL_NODES"),
    CLICK_UI_TEXT(MacroStepCategory.IN_APP_ACTION, "CLICK_TEXT", "Click In-App Button / View ID", "e.g. Search, Next, Submit"),
    TAP_COORDINATES(MacroStepCategory.IN_APP_ACTION, "GESTURE_TAP", "Tap Screen Coordinates (X, Y)", "e.g. 540,1150"),
    DOUBLE_TAP_COORDINATES(MacroStepCategory.IN_APP_ACTION, "DOUBLE_TAP", "Double-Tap Coordinates (X, Y)", "e.g. 540,1150"),
    LONG_PRESS_COORDINATES(MacroStepCategory.IN_APP_ACTION, "LONG_PRESS", "Long-Press Coordinates (X, Y)", "e.g. 540,1150"),
    DRAG_SWIPE_VECTOR(MacroStepCategory.IN_APP_ACTION, "DRAG_SWIPE", "Drag / Swipe Path (X1,Y1,X2,Y2)", "e.g. 540,1600,540,500"),
    SCROLL_SCREEN(MacroStepCategory.IN_APP_ACTION, "GESTURE_SCROLL", "Scroll Active Screen", "DOWN, UP, LEFT, or RIGHT"),
    PINCH_ZOOM(MacroStepCategory.IN_APP_ACTION, "PINCH_ZOOM", "Multi-Touch Pinch Gesture", "IN or OUT"),
    INPUT_TEXT(MacroStepCategory.IN_APP_ACTION, "INPUT_TEXT", "Type Text into Focused Input", "e.g. Hello world"),
    GLOBAL_NAVIGATION(MacroStepCategory.IN_APP_ACTION, "GLOBAL_NAV", "OS System Gesture / Action", "BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, SCREENSHOT"),
    SET_BRIGHTNESS(MacroStepCategory.ADJUST_SETTING, "SET_BRIGHTNESS", "Set Screen Brightness (%)", "5 to 100 (e.g. 80)"),
    SET_VOLUME(MacroStepCategory.ADJUST_SETTING, "SET_VOLUME", "Set Media Volume (%)", "0 to 100 (e.g. 60)"),
    SET_RINGER_MODE(MacroStepCategory.ADJUST_SETTING, "SET_RINGER", "Set Ringer Profile", "NORMAL, VIBRATE, or SILENT"),
    TOGGLE_FLASHLIGHT(MacroStepCategory.ADJUST_SETTING, "TOGGLE_TORCH", "Flashlight / Torch", "ON, OFF, or TOGGLE"),
    OPEN_SETTINGS_PANEL(MacroStepCategory.ADJUST_SETTING, "OPEN_PANEL", "Open System Settings Panel", "WIFI, BLUETOOTH, DISPLAY, SOUND"),
    WAIT_MS(MacroStepCategory.WAIT_DELAY, "WAIT_MS", "Pause Between Steps (ms)", "e.g. 500"),
    SMART_HOME_CMD(MacroStepCategory.SMART_HOME, "SMART_HOME", "Smart Home Device Action", "e.g. Study Lights Dim"),
    RUN_SHELL(MacroStepCategory.SHELL_SCRIPT, "SHELL", "Execute Shell Command", "e.g. sync && echo Ready"),
    SPEAK_TTS(MacroStepCategory.TTS_ANNOUNCE, "TTS", "Speak Voice Confirmation", "e.g. Workflow sequence finished")
}

data class MacroActionStep(
    val id: String = "step_${System.nanoTime()}",
    val actionType: MacroStepActionType,
    val targetValue: String,
    val description: String = "${actionType.label}: $targetValue",
    val continueOnError: Boolean = true
) {
    fun toTokenString(): String = "${actionType.code}:$targetValue"
}

enum class StepRunState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

data class MacroStepExecutionLog(
    val stepIndex: Int,
    val step: MacroActionStep,
    val state: StepRunState,
    val outputMessage: String,
    val durationMs: Long = 0L
)

data class AutonomousMacroRunState(
    val macroId: Int = 0,
    val macroTitle: String = "",
    val triggerPhrase: String = "",
    val isRunning: Boolean = false,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val stepLogs: List<MacroStepExecutionLog> = emptyList(),
    val summaryMessage: String = ""
)

object MacroSequenceCodec {

    fun encodeStepsToChain(steps: List<MacroActionStep>): String {
        return steps.joinToString(" -> ") { "${it.actionType.code}:${it.targetValue.trim()}" }
    }

    fun decodeChainToSteps(chain: String): List<MacroActionStep> {
        if (chain.isBlank()) return emptyList()
        // Support both JSON array format and readable "CODE:VALUE -> CODE:VALUE" format
        val trimmed = chain.trim()
        if (trimmed.startsWith("[")) {
            return try {
                val arr = JSONArray(trimmed)
                (0 until arr.length()).mapNotNull { idx ->
                    val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                    val code = obj.optString("code", "LAUNCH_APP")
                    val target = obj.optString("target", "")
                    val type = MacroStepActionType.entries.firstOrNull { it.code == code }
                        ?: MacroStepActionType.OPEN_APP
                    MacroActionStep(
                        id = "step_${idx}_${System.nanoTime()}",
                        actionType = type,
                        targetValue = target,
                        continueOnError = obj.optBoolean("continueOnError", true)
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        return trimmed.split("->")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { idx, rawToken ->
                val codePart = rawToken.substringBefore(":").trim().uppercase()
                val valPart = rawToken.substringAfter(":", "").trim()
                val matchedType = MacroStepActionType.entries.firstOrNull {
                    it.code.equals(codePart, ignoreCase = true)
                } ?: when {
                    codePart.contains("VISION") || codePart.contains("CLICK") -> MacroStepActionType.CLICK_UI_TEXT
                    codePart.contains("SYNC") -> MacroStepActionType.SPEAK_TTS
                    else -> MacroStepActionType.OPEN_APP
                }
                MacroActionStep(
                    id = "step_${idx}_${rawToken.hashCode()}",
                    actionType = matchedType,
                    targetValue = valPart.ifEmpty { rawToken }
                )
            }
    }

    fun toJsonMetadata(steps: List<MacroActionStep>): String {
        val arr = JSONArray()
        steps.forEach { step ->
            arr.put(
                JSONObject()
                    .put("code", step.actionType.code)
                    .put("category", step.actionType.category.name)
                    .put("target", step.targetValue)
                    .put("continueOnError", step.continueOnError)
            )
        }
        return arr.toString()
    }
}
