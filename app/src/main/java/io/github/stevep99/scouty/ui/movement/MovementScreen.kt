package io.github.stevep99.scouty.ui.movement

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stevep99.scouty.core.Action
import io.github.stevep99.scouty.core.Motion
import io.github.stevep99.scouty.core.MovementMode

private val Motion.label: String
    get() = when (this) {
        Motion.Forward -> "Forward"
        Motion.Backward -> "Back"
        Motion.Left -> "Left"
        Motion.Right -> "Right"
        Motion.Wiggle -> "Wiggle"
        Motion.Dance -> "Dance"
    }

private val Motion.icon: String
    get() = when (this) {
        Motion.Forward -> "\u25B2"
        Motion.Backward -> "\u25BC"
        Motion.Left -> "\u25C0"
        Motion.Right -> "\u25B6"
        Motion.Wiggle -> "~"
        Motion.Dance -> "\u266B"
    }

private val Motion.containerColor: Color
    get() = when (this) {
        Motion.Wiggle -> Color(0xFFFF9800)
        Motion.Dance -> Color(0xFF9C27B0)
        else -> Color(0xFF455A64)
    }

private fun Action.label(motion: Motion): String = when (this) {
    Action.Stop -> "Stop"
    is Action.MovementAction -> motion.label
    else -> ""
}

private fun Action.icon(motion: Motion): String = when (this) {
    Action.Stop -> "\u25A0"
    is Action.MovementAction -> motion.icon
    else -> ""
}

@Composable
fun MovementScreen(
    onAction: (Action) -> Unit,
    onBack: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    isListening: Boolean,
    voiceText: String?,
    onVoiceTextConsumed: () -> Unit,
    movementMode: MovementMode,
    onMovementModeChange: (MovementMode) -> Unit,
    queue: List<Action>,
    executing: Boolean,
    onExecute: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Movement", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onBack) { Text("Menu") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = movementMode == MovementMode.QUEUE,
                onClick = { onMovementModeChange(MovementMode.QUEUE) },
                enabled = !executing,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Queue") }
            SegmentedButton(
                selected = movementMode == MovementMode.IMMEDIATE,
                onClick = { onMovementModeChange(MovementMode.IMMEDIATE) },
                enabled = !executing,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Immediate") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // D-Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ActionButton(
                Action.MovementAction(Motion.Forward),
                enabled = !executing
            ) { onAction(it) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionButton(
                    Action.MovementAction(Motion.Left),
                    enabled = !executing
                ) { onAction(it) }
                ActionButton(Action.Stop, enabled = true) { onAction(it) }
                ActionButton(
                    Action.MovementAction(Motion.Right),
                    enabled = !executing
                ) { onAction(it) }
            }
            ActionButton(
                Action.MovementAction(Motion.Backward),
                enabled = !executing
            ) { onAction(it) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Special actions + Voice
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                Action.MovementAction(Motion.Wiggle),
                enabled = !executing
            ) { onAction(it) }
            ActionButton(
                Action.MovementAction(Motion.Dance),
                enabled = !executing
            ) { onAction(it) }
            // Voice button
            Button(
                onClick = { if (isListening) onStopListening() else onStartListening() },
                enabled = !executing,
                modifier = Modifier.width(90.dp).height(72.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFF44336) else Color(0xFF00897B)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83C\uDF99", fontSize = 18.sp)
                    Text(
                        if (isListening) "Listening" else "Voice",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Voice hint
        if (isListening) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (movementMode == MovementMode.IMMEDIATE) {
                    "Say: forward, left, right, back, stop, dance... (executed instantly)"
                } else {
                    "Say: forward, left, then forward (queued, then say execute)"
                },
                fontSize = 11.sp,
                color = Color(0xFF80CBC4),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (movementMode == MovementMode.IMMEDIATE) {
            Text(
                "Immediate mode - commands run right away",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
        } else {
            // Queue display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Queue (${queue.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = queue.isNotEmpty() && !executing
                    ) { Text("Undo") }
                    OutlinedButton(
                        onClick = onClear,
                        enabled = queue.isNotEmpty() && !executing
                    ) { Text("Clear") }
                    Button(
                        onClick = onExecute,
                        enabled = queue.isNotEmpty() && !executing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Execute (${queue.size})") }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(queue) { action ->
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .border(1.dp, Color(0xFF888888), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (action is Action.MovementAction) {
                            Text(
                                action.motions.joinToString(" ") { it.icon + " " + it.label },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFCCCCCC)
                            )
                        } else {
                            Text(
                                action.label(Motion.Forward),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }
                }
            }

            if (queue.isEmpty() && !executing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tap buttons or use voice to queue actions",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: Action,
    enabled: Boolean,
    onClick: (Action) -> Unit
) {
    val motion = (action as? Action.MovementAction)?.motions?.firstOrNull()
    Button(
        onClick = { onClick(action) },
        enabled = enabled,
        modifier = Modifier.width(90.dp).height(72.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = motion?.containerColor ?: Color(0xFFF44336)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(action.icon(motion ?: Motion.Forward), fontSize = 18.sp)
            Text(action.label(motion ?: Motion.Forward), fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}
