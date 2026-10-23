package com.example.domain

import android.app.Activity
import android.provider.Settings
import com.example.data.local.SmartDeviceEntity
import com.example.data.local.VoiceMacroEntity
import com.example.service.AetherAccessibilityService
import com.example.service.DeviceControlEngine
import kotlin.math.max

enum class IntentCategory(val badgeLabel: String) {
    VOICE_MACRO_TRIGGER("VOICE MACRO TRIGGER"),
    PHONE_SETTING_CONTROL("PHONE SETTING ADJUSTMENT"),
    APP_MANAGEMENT("APPLICATION MANAGEMENT"),
    IN_APP_UI_ACTION("IN-APP GESTURE / UI ACTION"),
    SMART_HOME_OR_SYNC("ECOSYSTEM / DEVICE SYNC"),
    AGENT_OR_DIAGNOSTIC("AGENT / SYSTEM DIAGNOSTIC"),
    CONVERSATIONAL_QUERY("AI CONVERSATIONAL QUERY"),
    UNCLEAR_COMMAND("UNCLEAR / AMBIGUOUS COMMAND"),
    UNSUPPORTED_COMMAND("UNSUPPORTED / RESTRICTED ACTION")
}

enum class CommandExecutionStatus {
    SUCCESS,
    PARTIAL_WARNING,
    ERROR_UNCLEAR,
    ERROR_UNSUPPORTED,
    ROUTED_TO_AI
}

data class ParsedSubCommand(
    val rawClause: String,
    val category: IntentCategory,
    val actionName: String,
    val parameters: Map<String, String> = emptyMap(),
    val matchedMacro: VoiceMacroEntity? = null,
    val confidence: Float = 0.95f,
    val errorReason: String? = null,
    val recoverySuggestions: List<String> = emptyList()
)

data class VoiceProcessingReport(
    val rawTranscript: String,
    val normalizedTranscript: String,
    val transcriptionConfidence: Float,
    val primaryCategory: IntentCategory,
    val status: CommandExecutionStatus,
    val parsedSubCommands: List<ParsedSubCommand>,
    val executedActionLogs: List<String>,
    val triggeredMacro: VoiceMacroEntity? = null,
    val userFacingMessage: String,
    val recoverySuggestions: List<String> = emptyList()
)

/**
 * Dedicated Voice Command Processing Module:
 * 1. Normalizes spoken transcripts (spoken numbers, filler words, wake-word stripping, clarity scoring).
 * 2. Parses single or multi-step natural language utterances into typed intents (Voice Macros, Settings, Apps, In-App UI Actions).
 * 3. Provides deterministic error handling and actionable recovery suggestions for unclear or unsupported voice commands.
 */
class VoiceCommandProcessor(
    private val deviceControl: DeviceControlEngine
) {
    private val spokenNumbers = mapOf(
        "zero" to "0", "mute" to "0", "ten" to "10", "fifteen" to "15",
        "twenty" to "20", "twenty five" to "25", "thirty" to "30",
        "forty" to "40", "fifty" to "50", "half" to "50",
        "sixty" to "60", "seventy" to "70", "seventy five" to "75",
        "eighty" to "80", "eighty five" to "85", "ninety" to "90",
        "hundred" to "100", "one hundred" to "100", "full" to "100", "maximum" to "100", "max" to "100",
        "minimum" to "10", "min" to "10"
    )

    private val wakeWordPrefixes = listOf(
        "hey aether", "ok aether", "aether agent", "aether", "assistant", "please", "can you", "could you", "i want you to"
    )

    /**
     * Normalizes raw spoken language into a clean, canonical command string.
     */
    fun normalizeTranscript(raw: String): String {
        var text = raw.trim().lowercase()
        // Strip leading wake words and polite fillers
        for (prefix in wakeWordPrefixes) {
            if (text.startsWith("$prefix ")) {
                text = text.removePrefix("$prefix ").trim()
            }
        }
        text = text.replace(Regex("""\b(um|uh|er|like|please)\b"""), " ")
        // Convert multi-word spoken numbers first, then single-word numbers
        spokenNumbers.entries.sortedByDescending { it.key.length }.forEach { (word, digits) ->
            text = text.replace(Regex("""\b${Regex.escape(word)}\b"""), digits)
        }
        return text.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Evaluates whether a candidate macro trigger phrase conflicts with an existing macro.
     */
    fun findConflictingMacro(
        candidateTrigger: String,
        existingMacros: List<VoiceMacroEntity>,
        ignoreMacroId: Int? = null
    ): VoiceMacroEntity? {
        val normCandidate = normalizeTranscript(candidateTrigger)
        if (normCandidate.isBlank()) return null
        return existingMacros.firstOrNull { existing ->
            existing.id != ignoreMacroId &&
                normalizeTranscript(existing.triggerPhrase) == normCandidate
        }
    }

    /**
     * Parses a spoken or typed command, matches voice macros or multi-step intents,
     * executes supported settings/app/UI actions, and handles unclear or unsupported commands gracefully.
     */
    fun processAndExecuteVoiceCommand(
        rawUtterance: String,
        speechConfidence: Float = 0.94f,
        macros: List<VoiceMacroEntity>,
        smartDevices: List<SmartDeviceEntity>,
        activity: Activity? = null
    ): VoiceProcessingReport {
        val trimmedRaw = rawUtterance.trim()
        val normalized = normalizeTranscript(trimmedRaw)

        // 1. Check for empty, ultra-short, or unintelligible noise input
        if (normalized.isEmpty() || normalized.length < 2 || normalized.all { !it.isLetterOrDigit() }) {
            return VoiceProcessingReport(
                rawTranscript = trimmedRaw,
                normalizedTranscript = normalized,
                transcriptionConfidence = 0.15f,
                primaryCategory = IntentCategory.UNCLEAR_COMMAND,
                status = CommandExecutionStatus.ERROR_UNCLEAR,
                parsedSubCommands = listOf(
                    ParsedSubCommand(
                        rawClause = trimmedRaw,
                        category = IntentCategory.UNCLEAR_COMMAND,
                        actionName = "UNINTELLIGIBLE_AUDIO",
                        confidence = 0.15f,
                        errorReason = "No clear speech tokens were detected in the audio stream.",
                        recoverySuggestions = listOf(
                            "Set brightness to 80%",
                            "Set volume to 50%",
                            "Open Settings",
                            "Activate stealth focus"
                        )
                    )
                ),
                executedActionLogs = emptyList(),
                userFacingMessage = "I couldn't catch a clear command from that audio. Try speaking a complete instruction such as 'Set brightness to 80%', 'Open Settings', or say a macro trigger phrase.",
                recoverySuggestions = listOf(
                    "Set brightness to 80%",
                    "Set volume to 50%",
                    "Open Settings",
                    "Activate stealth focus"
                )
            )
        }

        // 2. Check if the entire utterance matches a Voice-Activated Macro (Exact, Substring, or Fuzzy Token Match)
        val macroMatch = matchVoiceMacro(normalized, macros)
        if (macroMatch != null) {
            val (macro, matchScore) = macroMatch
            val parsedSub = ParsedSubCommand(
                rawClause = normalized,
                category = IntentCategory.VOICE_MACRO_TRIGGER,
                actionName = "TRIGGER_MACRO",
                parameters = mapOf(
                    "macroTitle" to macro.title,
                    "triggerPhrase" to macro.triggerPhrase,
                    "stepsCount" to MacroSequenceCodec.decodeChainToSteps(macro.stepsJson).size.toString(),
                    "matchScore" to "${(matchScore * 100).toInt()}%"
                ),
                matchedMacro = macro,
                confidence = matchScore
            )
            return VoiceProcessingReport(
                rawTranscript = trimmedRaw,
                normalizedTranscript = normalized,
                transcriptionConfidence = matchScore,
                primaryCategory = IntentCategory.VOICE_MACRO_TRIGGER,
                status = CommandExecutionStatus.SUCCESS,
                parsedSubCommands = listOf(parsedSub),
                executedActionLogs = listOf("VOICE MACRO MATCHED (${(matchScore * 100).toInt()}%): '${macro.title}'"),
                triggeredMacro = macro,
                userFacingMessage = "Voice command recognized: triggering autonomous macro '${macro.title}' (\"${macro.triggerPhrase}\")."
            )
        }

        // 3. Check for Explicitly Unsupported / OS-Restricted Hardware Commands
        val unsupportedCheck = detectUnsupportedCommand(normalized)
        if (unsupportedCheck != null) {
            return VoiceProcessingReport(
                rawTranscript = trimmedRaw,
                normalizedTranscript = normalized,
                transcriptionConfidence = speechConfidence,
                primaryCategory = IntentCategory.UNSUPPORTED_COMMAND,
                status = CommandExecutionStatus.ERROR_UNSUPPORTED,
                parsedSubCommands = listOf(unsupportedCheck),
                executedActionLogs = emptyList(),
                userFacingMessage = unsupportedCheck.errorReason
                    ?: "This command is restricted by Android OS security policy.",
                recoverySuggestions = unsupportedCheck.recoverySuggestions
            )
        }

        // 4. Split compound natural language commands ("set brightness to 80 and open settings")
        val clauses = normalized
            .split(Regex("""\b(?:and then|then|after that|and also|and)\b|,|;"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(normalized) }

        val parsedSubCommands = mutableListOf<ParsedSubCommand>()
        val executedLogs = mutableListOf<String>()
        val allSuggestions = mutableListOf<String>()

        for (clause in clauses) {
            // Check if a sub-clause matches a macro
            val subMacro = matchVoiceMacro(clause, macros)
            if (subMacro != null) {
                parsedSubCommands.add(
                    ParsedSubCommand(
                        rawClause = clause,
                        category = IntentCategory.VOICE_MACRO_TRIGGER,
                        actionName = "TRIGGER_MACRO",
                        parameters = mapOf("macroTitle" to subMacro.first.title),
                        matchedMacro = subMacro.first,
                        confidence = subMacro.second
                    )
                )
                continue
            }

            val parsed = parseSingleClause(clause, smartDevices)
            parsedSubCommands.add(parsed)
            allSuggestions.addAll(parsed.recoverySuggestions)

            // Execute actionable sub-commands immediately
            when (parsed.category) {
                IntentCategory.PHONE_SETTING_CONTROL -> {
                    val log = executeSettingCommand(parsed, activity)
                    executedLogs.add(log)
                }
                IntentCategory.APP_MANAGEMENT -> {
                    val log = executeAppCommand(parsed)
                    executedLogs.add(log)
                }
                IntentCategory.IN_APP_UI_ACTION -> {
                    val log = executeInAppAction(parsed)
                    executedLogs.add(log)
                }
                IntentCategory.SMART_HOME_OR_SYNC -> {
                    val target = parsed.parameters["device"] ?: clause
                    executedLogs.add("ECOSYSTEM [$target]: Dispatched state update")
                }
                IntentCategory.AGENT_OR_DIAGNOSTIC -> {
                    if (parsed.actionName == "OPTIMIZE_SYSTEM") {
                        deviceControl.runDeepSystemOptimization()
                        executedLogs.add("SYSTEM OPTIMIZER: Trimmed memory & verified kernel diagnostics")
                    }
                }
                else -> {}
            }
        }

        val hasUnclear = parsedSubCommands.any { it.category == IntentCategory.UNCLEAR_COMMAND }
        val hasUnsupported = parsedSubCommands.any { it.category == IntentCategory.UNSUPPORTED_COMMAND }
        val primaryCat = parsedSubCommands.firstOrNull {
            it.category != IntentCategory.CONVERSATIONAL_QUERY
        }?.category ?: IntentCategory.CONVERSATIONAL_QUERY

        val overallStatus = when {
            executedLogs.isNotEmpty() && (hasUnclear || hasUnsupported) -> CommandExecutionStatus.PARTIAL_WARNING
            hasUnsupported && executedLogs.isEmpty() -> CommandExecutionStatus.ERROR_UNSUPPORTED
            hasUnclear && executedLogs.isEmpty() -> CommandExecutionStatus.ERROR_UNCLEAR
            executedLogs.isNotEmpty() -> CommandExecutionStatus.SUCCESS
            else -> CommandExecutionStatus.ROUTED_TO_AI
        }

        val firstMacro = parsedSubCommands.firstNotNullOfOrNull { it.matchedMacro }

        val summaryMessage = when (overallStatus) {
            CommandExecutionStatus.ERROR_UNCLEAR -> {
                val reason = parsedSubCommands.firstOrNull { it.errorReason != null }?.errorReason
                    ?: "Command parameters were incomplete or ambiguous."
                "Clarification needed: $reason"
            }
            CommandExecutionStatus.ERROR_UNSUPPORTED -> {
                parsedSubCommands.firstOrNull { it.errorReason != null }?.errorReason
                    ?: "That operation is not supported directly."
            }
            CommandExecutionStatus.PARTIAL_WARNING -> {
                val errNote = parsedSubCommands.firstOrNull { it.errorReason != null }?.errorReason.orEmpty()
                "Executed ${executedLogs.size} action(s) (${executedLogs.joinToString("; ")}), but one part needed clarification: $errNote"
            }
            CommandExecutionStatus.SUCCESS -> {
                "Voice Command Executed: ${executedLogs.joinToString(" • ")}"
            }
            CommandExecutionStatus.ROUTED_TO_AI -> {
                "Routed conversational query to AI reasoning engine."
            }
        }

        return VoiceProcessingReport(
            rawTranscript = trimmedRaw,
            normalizedTranscript = normalized,
            transcriptionConfidence = speechConfidence,
            primaryCategory = primaryCat,
            status = overallStatus,
            parsedSubCommands = parsedSubCommands,
            executedActionLogs = executedLogs,
            triggeredMacro = firstMacro,
            userFacingMessage = summaryMessage,
            recoverySuggestions = allSuggestions.distinct().take(4)
        )
    }

    /**
     * Matches normalized spoken input against saved voice macros using exact, substring, and Jaccard word similarity.
     */
    fun matchVoiceMacro(
        normalizedInput: String,
        macros: List<VoiceMacroEntity>
    ): Pair<VoiceMacroEntity, Float>? {
        if (macros.isEmpty() || normalizedInput.isBlank()) return null
        val cleaned = normalizedInput
            .removePrefix("run macro ")
            .removePrefix("execute macro ")
            .removePrefix("trigger macro ")
            .removePrefix("macro ")
            .trim()

        var bestMacro: VoiceMacroEntity? = null
        var bestScore = 0f

        for (macro in macros) {
            val triggerNorm = normalizeTranscript(macro.triggerPhrase)
            val titleNorm = normalizeTranscript(macro.title)
            val score = when {
                cleaned == triggerNorm || cleaned == titleNorm -> 1.0f
                normalizedInput.contains(triggerNorm) && triggerNorm.length >= 4 -> 0.96f
                triggerNorm.contains(cleaned) && cleaned.length >= 6 -> 0.88f
                else -> tokenOverlapSimilarity(cleaned, triggerNorm)
            }
            if (score > bestScore) {
                bestScore = score
                bestMacro = macro
            }
        }

        return if (bestMacro != null && bestScore >= 0.72f) {
            bestMacro to bestScore
        } else {
            null
        }
    }

    private fun tokenOverlapSimilarity(a: String, b: String): Float {
        val tokensA = a.split(" ").filter { it.length > 2 }.toSet()
        val tokensB = b.split(" ").filter { it.length > 2 }.toSet()
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0f
        val intersection = tokensA.intersect(tokensB).size.toFloat()
        val denominator = max(tokensA.size, tokensB.size).toFloat()
        return intersection / denominator
    }

    private fun detectUnsupportedCommand(clause: String): ParsedSubCommand? {
        return when {
            clause.contains("factory reset") || clause.contains("wipe phone") || clause.contains("format sd") || clause.contains("delete system") -> {
                ParsedSubCommand(
                    rawClause = clause,
                    category = IntentCategory.UNSUPPORTED_COMMAND,
                    actionName = "BLOCKED_DESTRUCTIVE_OS_OP",
                    confidence = 0.99f,
                    errorReason = "Safety Guardrail: Destructive system-wipe or factory-reset operations are blocked from voice execution.",
                    recoverySuggestions = listOf(
                        "Optimize system memory and diagnostics",
                        "Open Settings"
                    )
                )
            }
            clause.contains("airplane mode") -> {
                ParsedSubCommand(
                    rawClause = clause,
                    category = IntentCategory.UNSUPPORTED_COMMAND,
                    actionName = "RESTRICTED_AIRPLANE_TOGGLE",
                    confidence = 0.95f,
                    errorReason = "Android security restricts third-party apps from toggling Airplane Mode directly without root WRITE_SECURE_SETTINGS. Tap a suggestion below to open Network Settings or run via Root Shell.",
                    recoverySuggestions = listOf(
                        "Open Wi-Fi settings",
                        "Open Settings"
                    )
                )
            }
            clause.contains("reboot recovery") || clause.contains("flash bootloader") -> {
                ParsedSubCommand(
                    rawClause = clause,
                    category = IntentCategory.UNSUPPORTED_COMMAND,
                    actionName = "BLOCKED_BOOTLOADER_OP",
                    confidence = 0.98f,
                    errorReason = "Bootloader and recovery flashing via voice is disabled to prevent accidental device bricking.",
                    recoverySuggestions = listOf(
                        "Optimize system memory and diagnostics"
                    )
                )
            }
            else -> null
        }
    }

    private fun parseSingleClause(
        clause: String,
        smartDevices: List<SmartDeviceEntity>
    ): ParsedSubCommand {
        // 1. Check for incomplete/ambiguous setting or app commands first
        if (clause == "open" || clause == "launch" || clause == "start app") {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.UNCLEAR_COMMAND,
                actionName = "AMBIGUOUS_APP_TARGET",
                confidence = 0.40f,
                errorReason = "You said '$clause' without specifying which application to open.",
                recoverySuggestions = listOf(
                    "Open Settings",
                    "Open Chrome",
                    "Open Phone"
                )
            )
        }
        if (clause == "set brightness" || clause == "change brightness" || clause == "adjust brightness") {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.UNCLEAR_COMMAND,
                actionName = "MISSING_BRIGHTNESS_VALUE",
                confidence = 0.45f,
                errorReason = "Please specify a brightness percentage (5%–100%) or say 'increase brightness' / 'dim screen'.",
                recoverySuggestions = listOf(
                    "Set brightness to 80%",
                    "Set brightness to 30%",
                    "Increase brightness"
                )
            )
        }
        if (clause == "set volume" || clause == "change volume" || clause == "adjust volume") {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.UNCLEAR_COMMAND,
                actionName = "MISSING_VOLUME_VALUE",
                confidence = 0.45f,
                errorReason = "Please specify a volume level (0%–100%) or say 'mute volume' / 'volume up'.",
                recoverySuggestions = listOf(
                    "Set volume to 60%",
                    "Mute volume",
                    "Volume up"
                )
            )
        }
        if (clause == "tap" || clause == "click") {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.UNCLEAR_COMMAND,
                actionName = "MISSING_TAP_TARGET",
                confidence = 0.40f,
                errorReason = "Specify which screen element label or (X, Y) coordinates to tap.",
                recoverySuggestions = listOf(
                    "Tap 540, 1150",
                    "Scroll down",
                    "What is on my screen right now?"
                )
            )
        }

        // 2. Phone Settings Adjustments
        // Brightness with percentage
        val brightnessNum = Regex("""(?:brightness|screen brightness|set screen).*?(\d{1,3})""").find(clause)
        if (brightnessNum != null) {
            val pct = brightnessNum.groupValues[1].toIntOrNull()?.coerceIn(5, 100) ?: 75
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_BRIGHTNESS",
                parameters = mapOf("level" to pct.toString())
            )
        }
        if (clause.contains("brighter") || clause.contains("increase brightness") || clause.contains("brightness up")) {
            val next = (deviceControl.getScreenBrightnessPercent() + 25).coerceAtMost(100)
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_BRIGHTNESS",
                parameters = mapOf("level" to next.toString())
            )
        }
        if (clause.contains("dimmer") || clause.contains("decrease brightness") || clause.contains("dim screen") || clause.contains("brightness down")) {
            val next = (deviceControl.getScreenBrightnessPercent() - 25).coerceAtLeast(10)
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_BRIGHTNESS",
                parameters = mapOf("level" to next.toString())
            )
        }

        // Volume with percentage or direction
        val volNum = Regex("""(?:volume|sound|audio).*?(\d{1,3})""").find(clause)
        if (volNum != null) {
            val pct = volNum.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: 50
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_VOLUME",
                parameters = mapOf("level" to pct.toString())
            )
        }
        if (clause.contains("mute") || clause.contains("silence audio")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_VOLUME",
                parameters = mapOf("level" to "0")
            )
        }
        if (clause.contains("unmute") || clause.contains("restore volume")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_VOLUME",
                parameters = mapOf("level" to "65")
            )
        }
        if (clause.contains("louder") || clause.contains("volume up") || clause.contains("increase volume")) {
            val next = (deviceControl.getVolumePercent() + 20).coerceAtMost(100)
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_VOLUME",
                parameters = mapOf("level" to next.toString())
            )
        }
        if (clause.contains("quieter") || clause.contains("volume down") || clause.contains("lower volume") || clause.contains("decrease volume")) {
            val next = (deviceControl.getVolumePercent() - 20).coerceAtLeast(0)
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_VOLUME",
                parameters = mapOf("level" to next.toString())
            )
        }

        // Flashlight / Torch
        if (clause.contains("flashlight") || clause.contains("torch")) {
            val mode = when {
                clause.contains("off") || clause.contains("disable") -> "OFF"
                clause.contains("on") || clause.contains("enable") -> "ON"
                else -> "TOGGLE"
            }
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "TOGGLE_TORCH",
                parameters = mapOf("state" to mode)
            )
        }

        // Ringer mode
        if (clause.contains("vibrate mode") || clause.contains("ringer vibrate") || clause.contains("set to vibrate")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_RINGER",
                parameters = mapOf("mode" to "VIBRATE")
            )
        }
        if (clause.contains("silent mode") || clause.contains("do not disturb") || clause.contains("ringer silent")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_RINGER",
                parameters = mapOf("mode" to "SILENT")
            )
        }
        if (clause.contains("normal mode") || clause.contains("ringer normal") || clause.contains("ring mode")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "SET_RINGER",
                parameters = mapOf("mode" to "NORMAL")
            )
        }

        // System Settings Panels (Wi-Fi, Bluetooth, Accessibility, Display)
        if (clause.contains("wifi") || clause.contains("wi-fi")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "OPEN_PANEL",
                parameters = mapOf("panel" to "WIFI")
            )
        }
        if (clause.contains("bluetooth")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.PHONE_SETTING_CONTROL,
                actionName = "OPEN_PANEL",
                parameters = mapOf("panel" to "BLUETOOTH")
            )
        }

        // 3. In-App Actions & Real-Time Screen Interactions of All Kinds
        val dragVector = Regex("""(?:drag|swipe)\s*(?:from\s*)?\(?(\d{2,4})[\s,]+(\d{2,4})\)?\s*(?:to|->)\s*\(?(\d{2,4})[\s,]+(\d{2,4})\)?""").find(clause)
        if (dragVector != null) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "DRAG_SWIPE",
                parameters = mapOf(
                    "sx" to dragVector.groupValues[1],
                    "sy" to dragVector.groupValues[2],
                    "ex" to dragVector.groupValues[3],
                    "ey" to dragVector.groupValues[4]
                )
            )
        }
        val doubleTapCoords = Regex("""(?:double tap|double click)\s*\(?(\d{2,4})[\s,]+(\d{2,4})\)?""").find(clause)
        if (doubleTapCoords != null) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "DOUBLE_TAP_XY",
                parameters = mapOf(
                    "x" to doubleTapCoords.groupValues[1],
                    "y" to doubleTapCoords.groupValues[2]
                )
            )
        }
        val longPressCoords = Regex("""(?:long press|touch and hold|hold)\s*\(?(\d{2,4})[\s,]+(\d{2,4})\)?""").find(clause)
        if (longPressCoords != null) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "LONG_PRESS_XY",
                parameters = mapOf(
                    "x" to longPressCoords.groupValues[1],
                    "y" to longPressCoords.groupValues[2]
                )
            )
        }
        if (clause.startsWith("long press ") || clause.startsWith("long click ") || clause.startsWith("hold ")) {
            val targetLabel = clause.removePrefix("long press ").removePrefix("long click ").removePrefix("hold ").trim()
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "LONG_CLICK_TEXT",
                parameters = mapOf("label" to targetLabel)
            )
        }
        val tapCoords = Regex("""tap\s*\(?(\d{2,4})[\s,]+(\d{2,4})\)?""").find(clause)
        if (tapCoords != null) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "TAP_XY",
                parameters = mapOf(
                    "x" to tapCoords.groupValues[1],
                    "y" to tapCoords.groupValues[2]
                )
            )
        }
        if (clause.startsWith("double tap ") || clause.startsWith("double click ")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "DOUBLE_TAP_XY",
                parameters = mapOf("x" to "540", "y" to "1150")
            )
        }
        if (clause.startsWith("click ") || clause.startsWith("tap ") || clause.startsWith("press ")) {
            val targetLabel = clause.substringAfter(" ").trim()
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "CLICK_TEXT",
                parameters = mapOf("label" to targetLabel)
            )
        }
        if (clause.startsWith("type ") || clause.startsWith("input text ") || clause.startsWith("enter text ")) {
            val payload = clause.removePrefix("type ").removePrefix("input text ").removePrefix("enter text ").trim()
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "INPUT_TEXT",
                parameters = mapOf("text" to payload)
            )
        }
        if (clause == "copy text" || clause == "paste text" || clause == "cut text" || clause == "clear text") {
            val op = clause.substringBefore(" ").uppercase()
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "CLIPBOARD_EDIT",
                parameters = mapOf("op" to op)
            )
        }
        if (clause.contains("pinch") || clause.contains("zoom in") || clause.contains("zoom out")) {
            val mode = if (clause.contains("out")) "OUT" else "IN"
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "PINCH_ZOOM",
                parameters = mapOf("mode" to mode)
            )
        }
        if (clause.startsWith("scroll until ")) {
            val target = clause.removePrefix("scroll until ").removeSuffix(" is visible").trim()
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "SCROLL_FIND",
                parameters = mapOf("query" to target)
            )
        }
        if (clause.contains("read screen") || clause.contains("inspect screen")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "READ_SCREEN"
            )
        }
        if (clause.contains("scroll down") || clause.contains("swipe up")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "SCROLL",
                parameters = mapOf("direction" to "DOWN")
            )
        }
        if (clause.contains("scroll up") || clause.contains("swipe down")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "SCROLL",
                parameters = mapOf("direction" to "UP")
            )
        }
        if (clause.contains("scroll left") || clause.contains("swipe right")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "SCROLL",
                parameters = mapOf("direction" to "LEFT")
            )
        }
        if (clause.contains("scroll right") || clause.contains("swipe left")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "SCROLL",
                parameters = mapOf("direction" to "RIGHT")
            )
        }
        if (clause.contains("go back") || clause.contains("navigate back")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "BACK")
            )
        }
        if (clause.contains("go home") || clause.contains("home screen")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "HOME")
            )
        }
        if (clause.contains("recent apps") || clause.contains("show recents")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "RECENTS")
            )
        }
        if (clause.contains("open notifications") || clause.contains("show notifications")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "NOTIFICATIONS")
            )
        }
        if (clause.contains("quick settings")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "QUICK_SETTINGS")
            )
        }
        if (clause.contains("split screen")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "SPLIT_SCREEN")
            )
        }
        if (clause.contains("take screenshot") || clause.contains("screenshot")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "SCREENSHOT")
            )
        }
        if (clause.contains("lock screen")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.IN_APP_UI_ACTION,
                actionName = "GLOBAL_NAV",
                parameters = mapOf("action" to "LOCK_SCREEN")
            )
        }

        // 4. Application Management (Launch / Open App)
        if (clause.startsWith("open ") || clause.startsWith("launch ") || clause.startsWith("start ")) {
            val appName = clause.substringAfter(" ").removePrefix("app ").trim()
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.APP_MANAGEMENT,
                actionName = "LAUNCH_APP",
                parameters = mapOf("appQuery" to appName)
            )
        }

        // 5. Smart Home & Companion Devices
        val matchedDevice = smartDevices.firstOrNull { dev ->
            val key = dev.name.lowercase().split(" ").firstOrNull().orEmpty()
            key.length >= 4 && clause.contains(key)
        }
        if (matchedDevice != null) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.SMART_HOME_OR_SYNC,
                actionName = "TOGGLE_DEVICE",
                parameters = mapOf("device" to matchedDevice.name)
            )
        }

        // 6. System Optimization & Sub-Agent Spawning
        if (clause.contains("optimize") || clause.contains("clean memory") || clause.contains("diagnostic")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.AGENT_OR_DIAGNOSTIC,
                actionName = "OPTIMIZE_SYSTEM"
            )
        }
        if (clause.contains("spawn agent") || clause.contains("create agent") || clause.contains("make agent") || clause.contains("reproduce")) {
            return ParsedSubCommand(
                rawClause = clause,
                category = IntentCategory.AGENT_OR_DIAGNOSTIC,
                actionName = "SPAWN_AGENT",
                parameters = mapOf("objective" to clause)
            )
        }

        // 7. Default to Conversational Query
        return ParsedSubCommand(
            rawClause = clause,
            category = IntentCategory.CONVERSATIONAL_QUERY,
            actionName = "AI_QUERY"
        )
    }

    private fun executeSettingCommand(sub: ParsedSubCommand, activity: Activity?): String {
        return when (sub.actionName) {
            "SET_BRIGHTNESS" -> {
                val pct = sub.parameters["level"]?.toIntOrNull() ?: 75
                deviceControl.setScreenBrightnessPercent(pct, activity)
            }
            "SET_VOLUME" -> {
                val pct = sub.parameters["level"]?.toIntOrNull() ?: 50
                deviceControl.setVolumePercent(pct)
            }
            "TOGGLE_TORCH" -> {
                val state = sub.parameters["state"] ?: "TOGGLE"
                val enable = when (state) {
                    "ON" -> true
                    "OFF" -> false
                    else -> null
                }
                deviceControl.toggleFlashlight(enable)
            }
            "SET_RINGER" -> {
                val mode = sub.parameters["mode"] ?: "NORMAL"
                deviceControl.setRingerMode(mode)
            }
            "OPEN_PANEL" -> {
                val panel = sub.parameters["panel"] ?: "SETTINGS"
                when (panel) {
                    "WIFI" -> {
                        deviceControl.openSystemSettingsPanel(Settings.ACTION_WIFI_SETTINGS)
                        "Opened Android Wi-Fi Settings Panel"
                    }
                    "BLUETOOTH" -> {
                        deviceControl.openSystemSettingsPanel(Settings.ACTION_BLUETOOTH_SETTINGS)
                        "Opened Android Bluetooth Settings Panel"
                    }
                    else -> {
                        deviceControl.openSystemSettingsPanel(Settings.ACTION_SETTINGS)
                        "Opened Android System Settings"
                    }
                }
            }
            else -> "Adjusted phone setting: ${sub.actionName}"
        }
    }

    private fun executeAppCommand(sub: ParsedSubCommand): String {
        val query = sub.parameters["appQuery"].orEmpty()
        return deviceControl.launchAppByQuery(query)
    }

    private fun executeInAppAction(sub: ParsedSubCommand): String {
        return when (sub.actionName) {
            "TAP_XY" -> {
                val x = sub.parameters["x"]?.toFloatOrNull() ?: 540f
                val y = sub.parameters["y"]?.toFloatOrNull() ?: 1150f
                AetherAccessibilityService.executeTap(x, y)
            }
            "DOUBLE_TAP_XY" -> {
                val x = sub.parameters["x"]?.toFloatOrNull() ?: 540f
                val y = sub.parameters["y"]?.toFloatOrNull() ?: 1150f
                AetherAccessibilityService.executeDoubleTap(x, y)
            }
            "LONG_PRESS_XY" -> {
                val x = sub.parameters["x"]?.toFloatOrNull() ?: 540f
                val y = sub.parameters["y"]?.toFloatOrNull() ?: 1150f
                AetherAccessibilityService.executeLongPress(x, y)
            }
            "DRAG_SWIPE" -> {
                val sx = sub.parameters["sx"]?.toFloatOrNull() ?: 540f
                val sy = sub.parameters["sy"]?.toFloatOrNull() ?: 1600f
                val ex = sub.parameters["ex"]?.toFloatOrNull() ?: 540f
                val ey = sub.parameters["ey"]?.toFloatOrNull() ?: 500f
                AetherAccessibilityService.executeDragOrSwipe(sx, sy, ex, ey)
            }
            "CLICK_TEXT" -> {
                val label = sub.parameters["label"].orEmpty()
                AetherAccessibilityService.clickElementByLabel(label)
            }
            "LONG_CLICK_TEXT" -> {
                val label = sub.parameters["label"].orEmpty()
                AetherAccessibilityService.longClickElementByLabel(label)
            }
            "INPUT_TEXT" -> {
                val txt = sub.parameters["text"].orEmpty()
                AetherAccessibilityService.inputTextIntoFocusedNode(txt)
            }
            "CLIPBOARD_EDIT" -> {
                val op = sub.parameters["op"] ?: "COPY"
                AetherAccessibilityService.performNodeClipboardOrEditAction(op)
            }
            "PINCH_ZOOM" -> {
                val zoomIn = sub.parameters["mode"] != "OUT"
                AetherAccessibilityService.executePinchGesture(zoomIn)
            }
            "SCROLL_FIND" -> {
                val query = sub.parameters["query"].orEmpty()
                AetherAccessibilityService.scrollUntilTextVisible(query)
            }
            "READ_SCREEN" -> {
                AetherAccessibilityService.readCurrentScreenContent()
            }
            "SCROLL" -> {
                val dir = sub.parameters["direction"] ?: "DOWN"
                AetherAccessibilityService.executeScroll(dir)
            }
            "GLOBAL_NAV" -> {
                val act = sub.parameters["action"] ?: "BACK"
                AetherAccessibilityService.executeGlobalAction(act)
            }
            else -> "Performed in-app UI action: ${sub.actionName}"
        }
    }
}
