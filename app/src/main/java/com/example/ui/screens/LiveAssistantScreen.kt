package com.example.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CommandExecutionStatus
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveAssistantScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val activeProvider = providers.firstOrNull { it.isActive } ?: providers.firstOrNull()
    val offlineLock by viewModel.offlinePrivacyLock.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val screenVision by viewModel.screenVision.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessingCommand.collectAsStateWithLifecycle()
    val lastVoiceReport by viewModel.lastVoiceReport.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()

    var commandInput by remember { mutableStateOf("") }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.voiceManager.startListening()
        } else {
            viewModel.postToast("Microphone permission declined; use Quick Voice Chips or command bar")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Status & Mode Toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AETHER LIVE CONVERSATIONAL CORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = if (offlineLock) {
                                    "100% Offline Neural Engine (Zero Cloud)"
                                } else {
                                    activeProvider?.let { "${it.providerName} • ${it.modelName}" }
                                        ?: "Aether Local Core"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = if (offlineLock) QuantumEmerald.copy(alpha = 0.16f) else CyberCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (offlineLock) "OFFLINE LOCKED" else "E2EE ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (offlineLock) QuantumEmerald else CyberCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = offlineLock,
                            onClick = { viewModel.toggleOfflinePrivacyLock(!offlineLock) },
                            label = { Text("100% Offline Privacy") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Offline Privacy Mode",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = QuantumEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = QuantumEmerald
                            ),
                            modifier = Modifier.testTag("toggle_offline_privacy")
                        )

                        FilterChip(
                            selected = voiceState.continuousLiveMode,
                            onClick = {
                                viewModel.voiceManager.toggleContinuousLiveMode(!voiceState.continuousLiveMode)
                            },
                            label = { Text("Live Partner Bg") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Continuous Live Mode",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("toggle_continuous_live")
                        )

                        FilterChip(
                            selected = voiceState.liveScreenVisionEnabled,
                            onClick = {
                                viewModel.voiceManager.toggleScreenVision(!voiceState.liveScreenVisionEnabled)
                            },
                            label = { Text("Live Screen Vision") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RemoveRedEye,
                                    contentDescription = "Live Screen Vision",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuralViolet.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("toggle_screen_vision")
                        )
                    }
                }
            }
        }

        // 2. Interactive Acoustic & Neural Live Orb
        item {
            LiveNeuralOrbCard(
                isListening = voiceState.isListening,
                isSpeaking = voiceState.isSpeaking,
                isProcessing = isProcessing,
                statusBanner = voiceState.statusBanner,
                partialTranscript = voiceState.partialTranscript,
                errorDiagnostic = voiceState.lastErrorDiagnostic,
                onOrbToggle = {
                    if (voiceState.isListening) {
                        viewModel.voiceManager.stopListening()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopSpeech = { viewModel.voiceManager.stopSpeaking() }
            )
        }

        // 3. Voice Command Processing & Intent Disambiguation Telemetry Card
        lastVoiceReport?.let { report ->
            item {
                val isErrOrWarn = report.status == CommandExecutionStatus.ERROR_UNCLEAR ||
                    report.status == CommandExecutionStatus.ERROR_UNSUPPORTED ||
                    report.status == CommandExecutionStatus.PARTIAL_WARNING
                val borderCol = if (isErrOrWarn) AmberAlert else CyberCyan.copy(alpha = 0.6f)

                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, borderCol, RoundedCornerShape(20.dp))
                        .testTag("voice_nlu_report_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isErrOrWarn) Icons.Default.ErrorOutline else Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (isErrOrWarn) AmberAlert else CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "VOICE INTENT PARSER & NLU TELEMETRY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isErrOrWarn) AmberAlert else CyberCyan
                                    )
                                    Text(
                                        text = "Normalized: \"${report.normalizedTranscript}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Surface(
                                color = (if (isErrOrWarn) AmberAlert else QuantumEmerald).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${(report.transcriptionConfidence * 100).toInt()}% CONF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isErrOrWarn) AmberAlert else QuantumEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            report.parsedSubCommands.forEach { sub ->
                                Surface(
                                    color = ObsidianSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val paramsText = if (sub.parameters.isEmpty()) ""
                                    else " " + sub.parameters.entries.joinToString(",") { "${it.key}=${it.value}" }
                                    Text(
                                        text = "[${sub.category.badgeLabel}] ${sub.actionName}$paramsText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sub.errorReason != null) AmberAlert else QuantumEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (report.recoverySuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Clarification / Supported Command Suggestions (Tap to execute):",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberAlert
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                report.recoverySuggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = {
                                            viewModel.sendCommand(suggestion, activity = activity, speakReply = true)
                                        },
                                        label = { Text(suggestion) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = AmberAlert.copy(alpha = 0.14f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Real-Time Screen Interaction Engine of All Kinds (Interactive Touchpad, Node Map, Gestures & OS Actions)
        if (voiceState.liveScreenVisionEnabled) {
            item {
                RealTimeScreenInteractionCard(
                    screenVision = screenVision,
                    viewModel = viewModel
                )
            }
        }

        // 5. Instant Voice Macro & Multi-Step Command Simulator Chips
        item {
            Column {
                Text(
                    text = "VOICE MACRO TRIGGERS & NATURAL LANGUAGE COMMANDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dynamicMacroPrompts = macros.take(3).map { it.triggerPhrase }
                    val quickPrompts = dynamicMacroPrompts + listOf(
                        "Set brightness eighty percent and volume fifty",
                        "Open Settings and scroll down",
                        "Set brightness", // Tests ambiguous command disambiguation!
                        "Turn on airplane mode" // Tests unsupported OS restriction recovery!
                    )
                    quickPrompts.forEachIndexed { idx, q ->
                        AssistChip(
                            onClick = { viewModel.sendCommand(q, activity = activity, speakReply = true) },
                            label = { Text(q) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = ObsidianCard
                            ),
                            modifier = Modifier.testTag("quick_voice_chip_$idx")
                        )
                    }
                }
            }
        }

        // 6. Natural Language Command Input Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_command_input"),
                    placeholder = {
                        Text("Speak or type macro phrase, setting adjustment, or app command...")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendCommand(commandInput, activity = activity, speakReply = true)
                            commandInput = ""
                        }
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("send_command_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute Command"
                        )
                    }
                }
            }
        }

        // 7. Real-Time Conversational & Executed Tool Feed
        items(conversation, key = { it.id }) { turn ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (turn.isErrorOrClarification) AmberAlert else BorderSubtle,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = turn.intentBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (turn.isErrorOrClarification) AmberAlert else QuantumEmerald
                            )
                            Text(
                                text = "YOU • \"${turn.userPrompt}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberCyan
                            )
                        }
                        IconButton(
                            onClick = { viewModel.voiceManager.speakResponse(turn.agentReply) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak response aloud",
                                tint = QuantumEmerald
                            )
                        }
                    }

                    if (turn.executedTools.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            turn.executedTools.forEach { toolLog ->
                                Surface(
                                    color = QuantumEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "⚡ $toolLog",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QuantumEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = turn.agentReply,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (turn.recoverySuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            turn.recoverySuggestions.forEach { rec ->
                                AssistChip(
                                    onClick = {
                                        viewModel.sendCommand(rec, activity = activity, speakReply = true)
                                    },
                                    label = { Text(rec) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Engine: ${turn.providerBadge} • ${turn.latencyMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveNeuralOrbCard(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    statusBanner: String,
    partialTranscript: String,
    errorDiagnostic: String?,
    onOrbToggle: () -> Unit,
    onStopSpeech: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (isListening || isSpeaking) {
                        listOf(CyberCyan, QuantumEmerald, NeuralViolet)
                    } else {
                        listOf(BorderSubtle, CyberCyan.copy(alpha = 0.4f))
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .clickable { onOrbToggle() }
                    .testTag("voice_live_orb")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.minDimension * 0.32f
                    val activeBoost = if (isListening || isSpeaking || isProcessing) 1.18f else 1.0f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isListening) QuantumEmerald.copy(alpha = 0.40f)
                                else CyberCyan.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.48f * activeBoost
                        ),
                        radius = size.minDimension * 0.48f
                    )

                    for (i in 0 until 24) {
                        val angle = (i * 15f) * (Math.PI / 180.0) + pulsePhase
                        val wave = sin(pulsePhase * 2f + i).toFloat() *
                            (if (isListening || isSpeaking) 14f else 5f)
                        val rStart = baseRadius + 6f
                        val rEnd = baseRadius + 18f + wave
                        val startPos = Offset(
                            x = center.x + (rStart * cos(angle)).toFloat(),
                            y = center.y + (rStart * sin(angle)).toFloat()
                        )
                        val endPos = Offset(
                            x = center.x + (rEnd * cos(angle)).toFloat(),
                            y = center.y + (rEnd * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = if (i % 2 == 0) CyberCyan else QuantumEmerald,
                            start = startPos,
                            end = endPos,
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }

                    drawCircle(
                        color = if (isListening) QuantumEmerald else CyberCyan,
                        radius = baseRadius,
                        style = Stroke(width = 4f)
                    )
                }

                Surface(
                    color = if (isListening) QuantumEmerald.copy(alpha = 0.22f) else ObsidianSurfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Toggle Live Voice Assistant",
                            tint = if (isListening) QuantumEmerald else CyberCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusBanner,
                style = MaterialTheme.typography.titleMedium,
                color = if (isListening) QuantumEmerald else CyberCyan
            )

            if (partialTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"$partialTranscript\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            if (!errorDiagnostic.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = AmberAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = errorDiagnostic,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberAlert,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOrbToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) QuantumEmerald else CyberCyan,
                        contentColor = Color(0xFF001F24)
                    ),
                    modifier = Modifier.testTag("start_voice_live_button")
                ) {
                    Text(if (isListening) "Listening Live (Tap to Pause)" else "Start Live Voice Partner")
                }

                if (isSpeaking) {
                    OutlinedButton(onClick = onStopSpeech) {
                        Text("Mute TTS")
                    }
                }
            }
        }
    }
}
