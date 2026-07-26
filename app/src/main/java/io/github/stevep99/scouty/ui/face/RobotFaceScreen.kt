package io.github.stevep99.scouty.ui.face

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stevep99.scouty.core.ScoutyState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RobotFaceScreen(
    currentState: ScoutyState,
    ready: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blink = rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4_200
                0f at 0
                0f at 3_700
                1f at 3_860
                0f at 4_020
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blinkPhase"
    )

    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulsePhase"
    )

    val dartX = rememberInfiniteTransition(label = "dartX").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dartX"
    )
    val dartY = rememberInfiniteTransition(label = "dartY").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dartY"
    )

    val eq = rememberInfiniteTransition(label = "eq").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Restart),
        label = "eqPhase"
    )

    val bgColor = Color(0xFF546E7A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(ready, currentState) {
                detectTapGestures {
                    when {
                        ready && currentState == ScoutyState.Idle -> onStartListening()
                        currentState == ScoutyState.Listening -> onStopListening()
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Button(
                onClick = onOpenMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0E1116),
                    contentColor = Color.White
                )
            ) { Text("Menu", fontSize = 12.sp) }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawScoutyFace(
                state = currentState,
                blinkPhase = blink.value,
                pulsePhase = pulse.value,
                dartX = dartX.value,
                dartY = dartY.value,
                eqPhase = eq.value
            )
        }

        val hint = when {
            !ready -> "Waiting for services…"
            currentState == ScoutyState.Idle -> "Tap me and say hello!"
            currentState == ScoutyState.Listening -> "I'm listening…"
            currentState == ScoutyState.Thinking -> "Hmm…"
            currentState == ScoutyState.Speaking -> ""
            else -> ""
        }
        if (hint.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hint,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }
}

private fun DrawScope.drawScoutyFace(
    state: ScoutyState,
    blinkPhase: Float,
    pulsePhase: Float,
    dartX: Float,
    dartY: Float,
    eqPhase: Float
) {
    val accent = when (state) {
        ScoutyState.Listening -> Color(0xFF4CAF50)
        ScoutyState.Thinking -> Color(0xFFFFCB6B)
        ScoutyState.Speaking -> Color(0xFF89DDFF)
        ScoutyState.Moving -> Color(0xFFC792EA)
        ScoutyState.Idle -> Color(0xFF546E7A)
    }

    val cx = size.width / 2f
    val faceW = size.width
    val faceH = size.height
    val padX = size.width * 0.05f

    // Listening halo
    if (state == ScoutyState.Listening) {
        val ringR = size.minDimension * (0.45f + 0.08f * pulsePhase)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.30f * (1f - pulsePhase)), Color.Transparent),
                center = Offset(cx, size.height * 0.42f),
                radius = ringR
            ),
            radius = ringR,
            center = Offset(cx, size.height * 0.42f)
        )
    }

    // Eyes
    val eyeColor = Color(0xFF89DDFF)
    val eyeGlow = Color(0xFF89DDFF).copy(alpha = 0.35f)
    val vcy = size.height * 0.40f
    val eyeSpacing = faceW * 0.28f
    val eyeR = (faceW * 0.13f).coerceAtMost(size.height * 0.14f)

    var eyeScaleY = 1f - blinkPhase
    var pupilDx = 0f
    var pupilDy = 0f
    var happyArcs = false

    when (state) {
        ScoutyState.Listening -> eyeScaleY *= 1.12f
        ScoutyState.Thinking -> {
            pupilDx = dartX * eyeR * 0.45f
            pupilDy = dartY * eyeR * 0.30f
        }
        ScoutyState.Moving -> happyArcs = true
        else -> {}
    }

    if (happyArcs) {
        val arcW = eyeR * 2f
        listOf(-eyeSpacing, eyeSpacing).forEach { dx ->
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx + dx - arcW / 2f, vcy - eyeR * 0.55f),
                size = Size(arcW, arcW * 1.1f),
                style = Stroke(width = eyeR * 0.38f, cap = StrokeCap.Round)
            )
        }
    } else {
        listOf(-eyeSpacing, eyeSpacing).forEach { dx ->
            val ex = cx + dx
            scale(scaleX = 1f, scaleY = eyeScaleY.coerceAtLeast(0.06f), pivot = Offset(ex, vcy)) {
                drawCircle(color = eyeGlow, radius = eyeR * 1.35f, center = Offset(ex, vcy))
                drawCircle(color = eyeColor, radius = eyeR, center = Offset(ex, vcy))
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = eyeR * 0.28f,
                    center = Offset(ex - eyeR * 0.32f + pupilDx, vcy - eyeR * 0.32f + pupilDy)
                )
            }
        }
    }

    // Mouth
    val mouthCy = size.height * 0.68f

    when (state) {
        ScoutyState.Speaking -> {
            val bars = 5
            val barW = faceW * 0.055f
            val gap = barW * 1.15f
            val totalW = bars * barW + (bars - 1) * gap
            val startX = cx - totalW / 2f
            for (i in 0 until bars) {
                val phase = eqPhase * 2f * PI.toFloat() + i * 1.7f
                val amp = 0.35f + 0.65f * abs(sin(phase)) * abs(cos(phase * 0.63f + i))
                val barH = size.height * 0.20f * amp
                drawRoundRect(
                    color = eyeColor,
                    topLeft = Offset(startX + i * (barW + gap), mouthCy - barH / 2f),
                    size = Size(barW, barH.coerceAtLeast(barW)),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }
        ScoutyState.Listening -> {
            val r = faceW * 0.035f
            drawCircle(color = eyeColor, radius = r, center = Offset(cx, mouthCy))
        }
        ScoutyState.Thinking -> {
            drawLine(
                color = eyeColor,
                start = Offset(cx - padX, mouthCy),
                end = Offset(cx + padX, mouthCy),
                strokeWidth = faceW * 0.02f,
                cap = StrokeCap.Round
            )
        }
        else -> {
            // Gentle smile
            drawArc(
                color = eyeColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - faceW * 0.16f, mouthCy - size.height * 0.06f),
                size = Size(faceW * 0.32f, size.height * 0.14f),
                style = Stroke(width = faceW * 0.02f, cap = StrokeCap.Round)
            )
        }
    }
}
