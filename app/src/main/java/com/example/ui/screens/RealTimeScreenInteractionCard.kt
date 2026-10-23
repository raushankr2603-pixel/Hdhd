package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.service.ScreenVisionSnapshot
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary

private enum class CanvasTouchMode(val label: String) {
    SINGLE_TAP("Single Tap"),
    DOUBLE_TAP("Double-Tap"),
    LONG_PRESS("Long-Press"),
    DRAG_VECTOR("Drag / Swipe"),
    MULTI_BURST("3x Burst Tap")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RealTimeScreenInteractionCard(
    screenVision: ScreenVisionSnapshot,
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    var canvasMode by remember { mutableStateOf(CanvasTouchMode.SINGLE_TAP) }
    var activeCategoryTab by remember { mutableIntStateOf(0) } // 0=Touch & Drag, 1=Scroll & Zoom, 2=Text & Clipboard, 3=OS Global
    var customX by remember { mutableStateOf(screenVision.lastTouchX.toString()) }
    var customY by remember { mutableStateOf(screenVision.lastTouchY.toString()) }
    var dragEndXInput by remember { mutableStateOf("540") }
    var dragEndYInput by remember { mutableStateOf("500") }
    var textToInject by remember { mutableStateOf("") }
    var scrollFindQuery by remember { mutableStateOf("") }

    var dragStartX by remember { mutableFloatStateOf(540f) }
    var dragStartY by remember { mutableFloatStateOf(1500f) }

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .testTag("realtime_screen_interaction_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Real-Time Screen Interaction Hub",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "REAL-TIME SCREEN INTERACTION ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Text(
                            text = screenVision.activePackage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.performReadScreenContent() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("gesture_read_screen")
                    ) {
                        Text("Inspect", style = MaterialTheme.typography.labelSmall)
                    }
                    if (!screenVision.isServiceConnected) {
                        OutlinedButton(
                            onClick = { viewModel.openAccessibilitySettings() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("enable_accessibility_button")
                        ) {
                            Text("OS Service", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Screen Content Summary Bar
            Surface(
                color = ObsidianSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Live Hierarchy (${screenVision.windowCount} Win • ${screenVision.clickableCount} Clickable • ${screenVision.scrollableCount} Scrollable • ${screenVision.editableCount} Editable) • Last: [${screenVision.lastInteractionKind}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screenVision.fullScreenTextSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Screen Viewport & Touchpad Mode Selector
            Text(
                text = "INTERACTIVE LIVE SCREEN TOUCHPAD (Tap or Drag on Viewport below):",
                style = MaterialTheme.typography.labelSmall,
                color = QuantumEmerald
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CanvasTouchMode.entries.forEach { mode ->
                    FilterChip(
                        selected = canvasMode == mode,
                        onClick = { canvasMode = mode },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QuantumEmerald.copy(alpha = 0.22f),
                            selectedLabelColor = QuantumEmerald
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Scaled Screen Map Canvas (1080x2400 coordinate space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(ObsidianSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .testTag("interactive_screen_canvas")
                    .pointerInput(canvasMode) {
                        if (canvasMode == CanvasTouchMode.DRAG_VECTOR) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStartX = (offset.x / size.width.coerceAtLeast(1)) * 1080f
                                    dragStartY = (offset.y / size.height.coerceAtLeast(1)) * 2400f
                                },
                                onDragEnd = {},
                                onDrag = { change, _ ->
                                    change.consume()
                                    val ex = (change.position.x / size.width.coerceAtLeast(1)) * 1080f
                                    val ey = (change.position.y / size.height.coerceAtLeast(1)) * 2400f
                                    customX = dragStartX.toInt().toString()
                                    customY = dragStartY.toInt().toString()
                                    dragEndXInput = ex.toInt().toString()
                                    dragEndYInput = ey.toInt().toString()
                                }
                            )
                        } else {
                            detectTapGestures(
                                onTap = { offset ->
                                    val devX = (offset.x / size.width.coerceAtLeast(1)) * 1080f
                                    val devY = (offset.y / size.height.coerceAtLeast(1)) * 2400f
                                    customX = devX.toInt().toString()
                                    customY = devY.toInt().toString()
                                    when (canvasMode) {
                                        CanvasTouchMode.SINGLE_TAP -> viewModel.performScreenTap(devX, devY)
                                        CanvasTouchMode.DOUBLE_TAP -> viewModel.performScreenDoubleTap(devX, devY)
                                        CanvasTouchMode.LONG_PRESS -> viewModel.performScreenLongPress(devX, devY)
                                        CanvasTouchMode.MULTI_BURST -> viewModel.performScreenMultiTapBurst(devX, devY, 3)
                                        CanvasTouchMode.DRAG_VECTOR -> viewModel.performScreenTap(devX, devY)
                                    }
                                },
                                onDoubleTap = { offset ->
                                    val devX = (offset.x / size.width.coerceAtLeast(1)) * 1080f
                                    val devY = (offset.y / size.height.coerceAtLeast(1)) * 2400f
                                    customX = devX.toInt().toString()
                                    customY = devY.toInt().toString()
                                    viewModel.performScreenDoubleTap(devX, devY)
                                },
                                onLongPress = { offset ->
                                    val devX = (offset.x / size.width.coerceAtLeast(1)) * 1080f
                                    val devY = (offset.y / size.height.coerceAtLeast(1)) * 2400f
                                    customX = devX.toInt().toString()
                                    customY = devY.toInt().toString()
                                    viewModel.performScreenLongPress(devX, devY)
                                }
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 1080f
                    val scaleY = size.height / 2400f

                    // Subtle coordinate grid lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (gx in listOf(270f, 540f, 810f)) {
                        drawLine(
                            color = BorderSubtle.copy(alpha = 0.45f),
                            start = Offset(gx * scaleX, 0f),
                            end = Offset(gx * scaleX, size.height),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }
                    for (gy in listOf(600f, 1200f, 1800f)) {
                        drawLine(
                            color = BorderSubtle.copy(alpha = 0.45f),
                            start = Offset(0f, gy * scaleY),
                            end = Offset(size.width, gy * scaleY),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // Render live UI element bounding boxes
                    screenVision.visibleElements.take(16).forEach { elem ->
                        val boxColor = when {
                            elem.isEditable -> NeuralViolet
                            elem.isScrollable -> QuantumEmerald
                            elem.isClickable -> CyberCyan
                            else -> TextSecondary
                        }
                        val leftPx = (elem.left * scaleX).coerceIn(0f, size.width - 4f)
                        val topPx = (elem.top * scaleY).coerceIn(0f, size.height - 4f)
                        val widthPx = ((elem.right - elem.left) * scaleX).coerceAtLeast(12f)
                        val heightPx = ((elem.bottom - elem.top) * scaleY).coerceAtLeast(10f)

                        drawRoundRect(
                            color = boxColor.copy(alpha = 0.14f),
                            topLeft = Offset(leftPx, topPx),
                            size = Size(widthPx, heightPx),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = boxColor.copy(alpha = 0.75f),
                            topLeft = Offset(leftPx, topPx),
                            size = Size(widthPx, heightPx),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Draw drag/swipe vector line if present
                    val endX = screenVision.lastDragEndX
                    val endY = screenVision.lastDragEndY
                    val touchPxX = (screenVision.lastTouchX * scaleX).coerceIn(0f, size.width)
                    val touchPxY = (screenVision.lastTouchY * scaleY).coerceIn(0f, size.height)

                    if (endX != null && endY != null) {
                        val endPxX = (endX * scaleX).coerceIn(0f, size.width)
                        val endPxY = (endY * scaleY).coerceIn(0f, size.height)
                        drawLine(
                            color = AmberAlert,
                            start = Offset(touchPxX, touchPxY),
                            end = Offset(endPxX, endPxY),
                            strokeWidth = 3.5f
                        )
                        drawCircle(
                            color = AmberAlert,
                            radius = 7f,
                            center = Offset(endPxX, endPxY)
                        )
                    }

                    // Draw active touch crosshair & pulse ring
                    drawCircle(
                        color = QuantumEmerald.copy(alpha = 0.28f),
                        radius = 18f,
                        center = Offset(touchPxX, touchPxY)
                    )
                    drawCircle(
                        color = CyberCyan,
                        radius = 6f,
                        center = Offset(touchPxX, touchPxY)
                    )
                }

                // Coordinate HUD Overlay Badge
                Surface(
                    color = ObsidianCard.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Target: (${screenVision.lastTouchX}, ${screenVision.lastTouchY}) • ${canvasMode.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = screenVision.lastGestureLog,
                style = MaterialTheme.typography.labelSmall,
                color = QuantumEmerald
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interaction Category Tabs (All Kinds of Real-Time Screen Interactions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categoryLabels = listOf(
                    "1. Tap, Hold & Drag",
                    "2. Scroll & Pinch Zoom",
                    "3. Text & Clipboard",
                    "4. OS Global Actions"
                )
                categoryLabels.forEachIndexed { idx, title ->
                    FilterChip(
                        selected = activeCategoryTab == idx,
                        onClick = { activeCategoryTab = idx },
                        label = { Text(title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                            selectedLabelColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("screen_interaction_tab_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (activeCategoryTab) {
                0 -> {
                    // Tab 0: Single Tap, Double-Tap, Long-Press, Multi-Tap Burst, and Custom Drag/Swipe Vector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tx = customX.toFloatOrNull() ?: 540f
                        val ty = customY.toFloatOrNull() ?: 1150f
                        AssistChip(
                            onClick = { viewModel.performScreenTap(tx, ty) },
                            label = { Text("Tap (${tx.toInt()}, ${ty.toInt()})") },
                            modifier = Modifier.testTag("gesture_tap_center")
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenDoubleTap(tx, ty) },
                            label = { Text("Double-Tap") },
                            modifier = Modifier.testTag("gesture_double_tap")
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenLongPress(tx, ty) },
                            label = { Text("Long-Press (650ms)") },
                            modifier = Modifier.testTag("gesture_long_press")
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenMultiTapBurst(tx, ty, 3) },
                            label = { Text("3x Burst Tap") },
                            modifier = Modifier.testTag("gesture_multi_tap")
                        )
                        AssistChip(
                            onClick = {
                                val ex = dragEndXInput.toFloatOrNull() ?: 540f
                                val ey = dragEndYInput.toFloatOrNull() ?: 500f
                                viewModel.performScreenDragOrSwipe(tx, ty, ex, ey)
                            },
                            label = { Text("Drag Vector -> ($dragEndXInput,$dragEndYInput)") },
                            modifier = Modifier.testTag("gesture_drag_vector")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customX,
                            onValueChange = { customX = it.filter { c -> c.isDigit() } },
                            label = { Text("X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customY,
                            onValueChange = { customY = it.filter { c -> c.isDigit() } },
                            label = { Text("Y") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dragEndXInput,
                            onValueChange = { dragEndXInput = it.filter { c -> c.isDigit() } },
                            label = { Text("End X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dragEndYInput,
                            onValueChange = { dragEndYInput = it.filter { c -> c.isDigit() } },
                            label = { Text("End Y") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                1 -> {
                    // Tab 1: Directional Scrolls, 2-Finger Pinch Zoom, and Scroll-Until-Visible
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.performScreenScroll("DOWN") },
                            label = { Text("Scroll Down") },
                            modifier = Modifier.testTag("gesture_scroll_down")
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenScroll("UP") },
                            label = { Text("Scroll Up") },
                            modifier = Modifier.testTag("gesture_scroll_up")
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenScroll("LEFT") },
                            label = { Text("Swipe Left") }
                        )
                        AssistChip(
                            onClick = { viewModel.performScreenScroll("RIGHT") },
                            label = { Text("Swipe Right") }
                        )
                        AssistChip(
                            onClick = { viewModel.performPinchGesture(true) },
                            leadingIcon = {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Pinch Zoom-In") },
                            modifier = Modifier.testTag("gesture_pinch_in")
                        )
                        AssistChip(
                            onClick = { viewModel.performPinchGesture(false) },
                            leadingIcon = {
                                Icon(Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Pinch Zoom-Out") },
                            modifier = Modifier.testTag("gesture_pinch_out")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = scrollFindQuery,
                            onValueChange = { scrollFindQuery = it },
                            label = { Text("Scroll Until Text/Element Visible (e.g. Security)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                viewModel.performScrollUntilVisible(scrollFindQuery.ifBlank { "Live Voice Orb" })
                            }
                        ) {
                            Text("Find")
                        }
                    }
                }

                2 -> {
                    // Tab 2: Real-Time Text Injection & Clipboard / Edit Actions on Focused Node
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textToInject,
                            onValueChange = { textToInject = it },
                            label = { Text("Type / Inject Text into Active Screen Field") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("screen_text_inject_input")
                        )
                        Button(
                            onClick = {
                                if (textToInject.isNotBlank()) {
                                    viewModel.performTextInputInjection(textToInject)
                                    textToInject = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumEmerald,
                                contentColor = Color(0xFF002114)
                            ),
                            modifier = Modifier.testTag("screen_text_inject_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Type")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.performClipboardOrEditAction("COPY") },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text("Copy Selection") }
                        )
                        AssistChip(
                            onClick = { viewModel.performClipboardOrEditAction("PASTE") },
                            leadingIcon = {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text("Paste into Field") }
                        )
                        AssistChip(
                            onClick = { viewModel.performClipboardOrEditAction("CUT") },
                            label = { Text("Cut") }
                        )
                        AssistChip(
                            onClick = { viewModel.performClipboardOrEditAction("CLEAR_TEXT") },
                            label = { Text("Clear Input Field") }
                        )
                    }
                }

                3 -> {
                    // Tab 3: System-Level Global OS Screen Actions
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val globalActions = listOf(
                            "BACK" to "OS Back",
                            "HOME" to "OS Home",
                            "RECENTS" to "Recents",
                            "NOTIFICATIONS" to "Notifications",
                            "QUICK_SETTINGS" to "Quick Settings",
                            "SPLIT_SCREEN" to "Split-Screen",
                            "SCREENSHOT" to "Take Screenshot",
                            "LOCK_SCREEN" to "Lock Screen"
                        )
                        globalActions.forEach { (code, label) ->
                            AssistChip(
                                onClick = { viewModel.performGlobalNav(code) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Detected Interactive Nodes (Tap to Click, Long-Click icon option)
            if (screenVision.visibleElements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detected Screen Nodes (Tap to ACTION_CLICK • Tap [Hold] for ACTION_LONG_CLICK):",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    screenVision.visibleElements.take(6).forEach { elem ->
                        Surface(
                            color = ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${elem.label.take(18)} (${elem.centerX},${elem.centerY})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        viewModel.performClickByLabel(elem.label)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[Hold]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.performLongClickByLabel(elem.label)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Live Screen Interaction Feed (Recent real-time gestures & node interactions)
            if (screenVision.interactionHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Real-Time Interaction Log (${screenVision.interactionHistory.size} recent events):",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                screenVision.interactionHistory.take(3).forEach { ev ->
                    Text(
                        text = "• [${ev.kind}] ${ev.detail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
