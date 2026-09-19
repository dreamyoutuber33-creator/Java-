package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.OrbBlue
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbIndigo
import com.example.ui.theme.OrbMagenta
import com.example.ui.theme.OrbPurple
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SiriVoiceOrb(
    state: AssistantState,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_animations")

    // Breathing pulse for idle & speaking
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Fluid rotation for processing & aura flow
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.PROCESSING) 1200 else 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Wave oscillation phase for speaking
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Effective scale reactive to state and audio RMS
    val dynamicScale = when (state) {
        AssistantState.LISTENING -> 1.0f + (audioRms * 0.35f)
        AssistantState.PROCESSING -> 1.02f
        AssistantState.SPEAKING -> 1.03f + (sin(wavePhase) * 0.05f)
        else -> breathScale
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("voice_orb_button")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * dynamicScale

            // 1. Outer ambient glow halo
            val glowRadius = baseRadius * 1.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        OrbCyan.copy(alpha = if (state == AssistantState.LISTENING) 0.5f else 0.25f),
                        OrbPurple.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Multi-color glowing Siri orb core
            val orbColors = when (state) {
                AssistantState.LISTENING -> listOf(OrbCyan, OrbBlue, OrbIndigo, OrbPurple)
                AssistantState.PROCESSING -> listOf(OrbPurple, OrbMagenta, OrbCyan, OrbBlue)
                AssistantState.SPEAKING -> listOf(OrbCyan, OrbMagenta, OrbPurple, OrbBlue)
                AssistantState.ERROR -> listOf(Color(0xFFEF4444), OrbMagenta, OrbPurple)
                else -> listOf(OrbBlue, OrbIndigo, OrbPurple, OrbCyan)
            }

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = orbColors,
                    center = center
                ),
                radius = baseRadius * 0.78f,
                center = center
            )

            // 3. Inner deep luminescent overlay
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        OrbCyan.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.15f, center.y - baseRadius * 0.15f),
                    radius = baseRadius * 0.7f
                ),
                radius = baseRadius * 0.75f,
                center = center
            )

            // 4. Dynamic voice waves inside the orb when speaking or listening
            if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
                val waveAmplitude = if (state == AssistantState.LISTENING) {
                    (baseRadius * 0.22f) * (0.3f + audioRms * 0.7f)
                } else {
                    baseRadius * 0.18f
                }

                val wavePath = Path()
                val startX = center.x - baseRadius * 0.65f
                val endX = center.x + baseRadius * 0.65f
                val steps = 30
                val dx = (endX - startX) / steps

                wavePath.moveTo(startX, center.y)
                for (i in 0..steps) {
                    val x = startX + i * dx
                    val angle = (i.toFloat() / steps) * 4 * PI + wavePhase
                    val y = center.y + sin(angle).toFloat() * waveAmplitude
                    wavePath.lineTo(x, y)
                }

                drawPath(
                    path = wavePath,
                    color = Color.White.copy(alpha = 0.95f),
                    style = Stroke(width = 3.5.dp.toPx())
                )

                // Secondary harmonic wave
                val wavePath2 = Path()
                wavePath2.moveTo(startX, center.y)
                for (i in 0..steps) {
                    val x = startX + i * dx
                    val angle = (i.toFloat() / steps) * 3 * PI - wavePhase
                    val y = center.y + sin(angle).toFloat() * (waveAmplitude * 0.65f)
                    wavePath2.lineTo(x, y)
                }

                drawPath(
                    path = wavePath2,
                    color = OrbCyan.copy(alpha = 0.8f),
                    style = Stroke(width = 2.dp.toPx())
                )
            } else {
                // Calm glowing audio center dot
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 5.dp.toPx() * breathScale,
                    center = center
                )
            }
        }
    }
}
