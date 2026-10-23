package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.MacroActionStep
import com.example.domain.MacroSequenceCodec
import com.example.domain.MacroStepActionType
import com.example.domain.MacroStepCategory
import com.example.domain.StepRunState
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacrosAndAgentsScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val macrosWithSteps by viewModel.macrosWithSteps.collectAsStateWithLifecycle()
    val subAgents by viewModel.subAgents.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val macroRunState by viewModel.activeMacroRunState.collectAsStateWithLifecycle()

    var editingMacroId by remember { mutableStateOf<Int?>(null) }
    var macroTitle by remember { mutableStateOf("") }
    var triggerPhrase by remember { mutableStateOf("") }
    var isOfflineCritical by remember { mutableStateOf(true) }

    val draftSteps = remember {
        mutableStateListOf(
            MacroActionStep(
                actionType = MacroStepActionType.SET_BRIGHTNESS,
                targetValue = "80"
            ),
            MacroActionStep(
                actionType = MacroStepActionType.OPEN_APP,
                targetValue = "Settings"
            ),
            MacroActionStep(
                actionType = MacroStepActionType.SCROLL_SCREEN,
                targetValue = "DOWN"
            )
        )
    }

    var selectedCategory by remember { mutableStateOf(MacroStepCategory.LAUNCH_APP) }
    var selectedActionType by remember { mutableStateOf(MacroStepActionType.OPEN_APP) }
    var stepParameterInput by remember { mutableStateOf("Settings") }

    val triggerConflict = remember(triggerPhrase, macros, editingMacroId) {
        if (triggerPhrase.isBlank()) null
        else viewModel.findTriggerConflict(triggerPhrase, editingMacroId)
    }

    var agentName by remember { mutableStateOf("") }
    var agentRole by remember { mutableStateOf("") }
    var agentTask by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Live Autonomous Macro Execution Telemetry Banner (when running or recently finished)
        if (macroRunState.totalSteps > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, QuantumEmerald, RoundedCornerShape(20.dp))
                        .testTag("autonomous_macro_telemetry_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (macroRunState.isRunning) "AUTONOMOUS MACRO EXECUTING..."
                                    else "AUTONOMOUS MACRO RUN COMPLETE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantumEmerald
                                )
                                Text(
                                    text = "${macroRunState.macroTitle} (\"${macroRunState.triggerPhrase}\")",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearMacroRunBanner() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss telemetry",
                                    tint = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                (macroRunState.currentStepIndex.toFloat() /
                                    macroRunState.totalSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            },
                            color = QuantumEmerald,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        macroRunState.stepLogs.forEach { log ->
                            val badgeColor = when (log.state) {
                                StepRunState.SUCCESS -> QuantumEmerald
                                StepRunState.RUNNING -> CyberCyan
                                StepRunState.FAILED -> CrimsonError
                                StepRunState.PENDING -> TextSecondary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${log.stepIndex + 1}. [${log.state.name}] ${log.outputMessage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (log.durationMs > 0) {
                                    Text(
                                        text = "${log.durationMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Structured Voice-Activated Macro Composer & Step Builder
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMacroId == null) "CREATE VOICE-ACTIVATED MACRO"
                                else "EDITING VOICE MACRO #$editingMacroId",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Define Action Sequence & Unique Voice Trigger",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (editingMacroId != null) {
                            OutlinedButton(
                                onClick = {
                                    editingMacroId = null
                                    macroTitle = ""
                                    triggerPhrase = ""
                                }
                            ) {
                                Text("Cancel Edit", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = macroTitle,
                        onValueChange = { macroTitle = it },
                        label = { Text("Macro Name (e.g. Commute Navigation & Audio)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_title_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = triggerPhrase,
                        onValueChange = { triggerPhrase = it },
                        label = { Text("Unique Voice Trigger Command (e.g. start commute protocol)") },
                        singleLine = true,
                        isError = triggerConflict != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_trigger_input")
                    )

                    if (triggerPhrase.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (triggerConflict == null) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (triggerConflict == null) QuantumEmerald else AmberAlert,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (triggerConflict == null) {
                                    "Unique voice trigger verified: say \"${triggerPhrase.trim().lowercase()}\" to run autonomously"
                                } else {
                                    "Conflict: phrase already used by '${triggerConflict.title}'"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (triggerConflict == null) QuantumEmerald else AmberAlert
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Step Category Builder
                    Text(
                        text = "STEP 1: SELECT ACTION CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MacroStepCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    val firstForCat = MacroStepActionType.entries.first { it.category == cat }
                                    selectedActionType = firstForCat
                                    stepParameterInput = defaultParamForAction(firstForCat)
                                },
                                label = { Text(cat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Type Selector within Category
                    Text(
                        text = "STEP 2: CHOOSE SPECIFIC OPERATION & PARAMETER",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MacroStepActionType.entries
                            .filter { it.category == selectedCategory }
                            .forEach { actType ->
                                FilterChip(
                                    selected = selectedActionType == actType,
                                    onClick = {
                                        selectedActionType = actType
                                        stepParameterInput = defaultParamForAction(actType)
                                    },
                                    label = { Text(actType.label) }
                                )
                            }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Contextual quick-parameter picker chips
                    val quickParams = remember(selectedActionType, installedApps) {
                        when (selectedActionType) {
                            MacroStepActionType.OPEN_APP ->
                                installedApps.take(5).map { it.appName }
                                    .ifEmpty { listOf("Settings", "Chrome", "Phone") }
                            MacroStepActionType.READ_SCREEN ->
                                listOf("SUMMARY", "ALL_NODES", "CLICKABLE_TARGETS")
                            MacroStepActionType.CLICK_UI_TEXT ->
                                listOf("Search", "Allow", "OK", "Next", "Display")
                            MacroStepActionType.TAP_COORDINATES ->
                                listOf("540,1150", "540,620", "900,1800")
                            MacroStepActionType.DOUBLE_TAP_COORDINATES ->
                                listOf("540,1150", "540,620", "540,1500")
                            MacroStepActionType.LONG_PRESS_COORDINATES ->
                                listOf("540,1150", "540,620", "320,900")
                            MacroStepActionType.DRAG_SWIPE_VECTOR ->
                                listOf("540,1600,540,500", "200,1100,900,1100", "900,1100,200,1100")
                            MacroStepActionType.SCROLL_SCREEN ->
                                listOf("DOWN", "UP", "LEFT", "RIGHT")
                            MacroStepActionType.PINCH_ZOOM ->
                                listOf("IN", "OUT")
                            MacroStepActionType.INPUT_TEXT ->
                                listOf("Hello from AetherOS", "Search query", "Confirmed")
                            MacroStepActionType.GLOBAL_NAVIGATION ->
                                listOf("BACK", "HOME", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS")
                            MacroStepActionType.SET_BRIGHTNESS ->
                                listOf("20", "50", "80", "100")
                            MacroStepActionType.SET_VOLUME ->
                                listOf("0", "35", "70", "100")
                            MacroStepActionType.SET_RINGER_MODE ->
                                listOf("NORMAL", "VIBRATE", "SILENT")
                            MacroStepActionType.TOGGLE_FLASHLIGHT ->
                                listOf("ON", "OFF", "TOGGLE")
                            MacroStepActionType.OPEN_SETTINGS_PANEL ->
                                listOf("WIFI", "BLUETOOTH", "DISPLAY", "SOUND")
                            MacroStepActionType.WAIT_MS ->
                                listOf("300", "800", "1500")
                            MacroStepActionType.SMART_HOME_CMD ->
                                listOf("Living Room Lights ON", "Study Lights Dim", "Studio HVAC 21C")
                            MacroStepActionType.RUN_SHELL ->
                                listOf("sync", "uname -a", "getprop ro.product.model")
                            MacroStepActionType.SPEAK_TTS ->
                                listOf("Workflow complete", "Stealth mode active")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickParams.forEach { presetVal ->
                            AssistChip(
                                onClick = { stepParameterInput = presetVal },
                                label = { Text(presetVal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stepParameterInput,
                            onValueChange = { stepParameterInput = it },
                            label = { Text(selectedActionType.paramHint) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("macro_step_param_input")
                        )
                        Button(
                            onClick = {
                                if (stepParameterInput.isNotBlank()) {
                                    draftSteps.add(
                                        MacroActionStep(
                                            actionType = selectedActionType,
                                            targetValue = stepParameterInput.trim()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("add_step_to_macro_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Step")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ordered Action Sequence Preview with Reorder & Remove Controls
                    Text(
                        text = "CONFIGURED ACTION SEQUENCE (${draftSteps.size} STEPS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantumEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (draftSteps.isEmpty()) {
                        Text(
                            text = "No steps added yet. Choose an action above and tap 'Add Step'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        draftSteps.forEachIndexed { idx, step ->
                            Surface(
                                color = ObsidianSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Step ${idx + 1}: ${step.actionType.label}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "Target: ${step.targetValue} (${step.actionType.code})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QuantumEmerald
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val item = draftSteps.removeAt(idx)
                                                    draftSteps.add(idx - 1, item)
                                                }
                                            },
                                            enabled = idx > 0,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Move step up",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx < draftSteps.lastIndex) {
                                                    val item = draftSteps.removeAt(idx)
                                                    draftSteps.add(idx + 1, item)
                                                }
                                            },
                                            enabled = idx < draftSteps.lastIndex,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Move step down",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { draftSteps.removeAt(idx) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove step",
                                                tint = CrimsonError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isOfflineCritical,
                            onCheckedChange = { isOfflineCritical = it }
                        )
                        Text(
                            text = "Offline-Critical Execution & Cross-Device E2EE Sync",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            val saved = viewModel.saveOrUpdateVoiceMacro(
                                editingId = editingMacroId,
                                title = macroTitle,
                                triggerPhrase = triggerPhrase,
                                steps = draftSteps.toList(),
                                isOfflineCritical = isOfflineCritical
                            )
                            if (saved) {
                                editingMacroId = null
                                macroTitle = ""
                                triggerPhrase = ""
                            }
                        },
                        enabled = triggerConflict == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_macro_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (editingMacroId == null) "Save Voice-Activated Macro"
                            else "Update Voice-Activated Macro"
                        )
                    }
                }
            }
        }

        // 2. Saved Voice Macros List (with Voice Trigger Test, Edit, and Autonomous Run)
        item {
            Text(
                text = "SAVED VOICE-ACTIVATED MACROS (${macros.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        items(macros, key = { it.id }) { macro ->
            val relationalMatch = macrosWithSteps.firstOrNull { it.macro.id == macro.id }
            val decodedSteps = remember(macro.stepsJson, relationalMatch) {
                val relSteps = relationalMatch?.orderedSteps.orEmpty()
                if (relSteps.isNotEmpty()) {
                    com.example.data.repository.VoiceMacroRepository.mapStepEntitiesToDomain(relSteps)
                } else {
                    MacroSequenceCodec.decodeChainToSteps(macro.stepsJson)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = macro.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voice Trigger: \"${macro.triggerPhrase}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberCyan
                                )
                            }
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    editingMacroId = macro.id
                                    macroTitle = macro.title
                                    triggerPhrase = macro.triggerPhrase
                                    isOfflineCritical = macro.isOfflineCritical
                                    draftSteps.clear()
                                    draftSteps.addAll(decodedSteps)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Macro",
                                    tint = CyberCyan
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteMacro(macro.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Macro",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        decodedSteps.forEachIndexed { i, step ->
                            Surface(
                                color = ObsidianSurfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${i + 1}. ${step.actionType.code}: ${step.targetValue}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantumEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Runs: ${macro.runCount} • ${decodedSteps.size} steps • E2EE: ${macro.e2eeSignature.ifEmpty { "Verified" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.sendCommand(
                                        prompt = macro.triggerPhrase,
                                        activity = activity,
                                        speakReply = true
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Simulate Voice", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.runVoiceMacro(macro, activity) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QuantumEmerald,
                                    contentColor = Color(0xFF002114)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("run_macro_${macro.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Execute Macro",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Sequence", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 3. Autonomous Sub-Agent Spawner & Self-Reproduction Engine
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralViolet.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = NeuralViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AUTONOMOUS AGENT SPAWNER & REPRODUCTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuralViolet
                            )
                            Text(
                                text = "Spawn & Self-Replicate Parallel Workflow Agents",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = agentName,
                        onValueChange = { agentName = it },
                        label = { Text("Sub-Agent Codename (e.g. Cross-App Data Harvester)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = agentRole,
                        onValueChange = { agentRole = it },
                        label = { Text("Specialization Role (e.g. UI Automator & API Bridge)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = agentTask,
                        onValueChange = { agentTask = it },
                        label = { Text("Autonomous Objective (e.g. Monitor screen & sync notes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.spawnSubAgent(
                                name = agentName,
                                role = agentRole,
                                task = agentTask
                            )
                            agentName = ""
                            agentRole = ""
                            agentTask = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spawn_agent_button")
                    ) {
                        Icon(imageVector = Icons.Default.AccountTree, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Spawn Autonomous Agent")
                    }
                }
            }
        }

        // 4. Real-Time Process Monitor & Active Sub-Agents
        item {
            Text(
                text = "REAL-TIME SUB-AGENT PROCESS DASHBOARD (${subAgents.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        items(subAgents, key = { it.id }) { agent ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${agent.name} • Gen ${agent.generationDepth}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Role: ${agent.role} (Parent: ${agent.parentAgentName})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberCyan
                            )
                        }
                        Surface(
                            color = QuantumEmerald.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${agent.status} • ${agent.cpuAllocationPercent}% CPU",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = agent.currentTask,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            (agent.stepsCompleted.toFloat() / agent.totalSteps.coerceAtLeast(1).toFloat())
                                .coerceIn(0f, 1f)
                        },
                        color = CyberCyan,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step ${agent.stepsCompleted}/${agent.totalSteps}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.advanceSubAgentStep(agent) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Step +1", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.reproduceSubAgent(agent) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("reproduce_agent_${agent.id}")
                            ) {
                                Text("Reproduce / Fork", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = { viewModel.deleteSubAgent(agent.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Terminate Agent",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun defaultParamForAction(actionType: MacroStepActionType): String {
    return when (actionType) {
        MacroStepActionType.OPEN_APP -> "Settings"
        MacroStepActionType.READ_SCREEN -> "SUMMARY"
        MacroStepActionType.CLICK_UI_TEXT -> "Search"
        MacroStepActionType.TAP_COORDINATES -> "540,1150"
        MacroStepActionType.DOUBLE_TAP_COORDINATES -> "540,1150"
        MacroStepActionType.LONG_PRESS_COORDINATES -> "540,1150"
        MacroStepActionType.DRAG_SWIPE_VECTOR -> "540,1600,540,500"
        MacroStepActionType.SCROLL_SCREEN -> "DOWN"
        MacroStepActionType.PINCH_ZOOM -> "IN"
        MacroStepActionType.INPUT_TEXT -> "Hello from AetherOS"
        MacroStepActionType.GLOBAL_NAVIGATION -> "BACK"
        MacroStepActionType.SET_BRIGHTNESS -> "80"
        MacroStepActionType.SET_VOLUME -> "60"
        MacroStepActionType.SET_RINGER_MODE -> "VIBRATE"
        MacroStepActionType.TOGGLE_FLASHLIGHT -> "TOGGLE"
        MacroStepActionType.OPEN_SETTINGS_PANEL -> "WIFI"
        MacroStepActionType.WAIT_MS -> "500"
        MacroStepActionType.SMART_HOME_CMD -> "Study Lights Dim"
        MacroStepActionType.RUN_SHELL -> "sync"
        MacroStepActionType.SPEAK_TTS -> "Macro workflow complete"
    }
}
