package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OrbBlue
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbIndigo
import com.example.ui.theme.OrbMagenta
import com.example.ui.theme.OrbPurple
import com.example.ui.theme.TextSecondaryDark
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * An animated sound wave visualizer component in Jetpack Compose that renders
 * real-time audio waveforms and frequency spectrum bars when Java is listening or speaking,
 * delivering immediate sensory feedback to the user.
 */
@Composable
fun SoundWaveVisualizer(
    state: AssistantState,
    audioRms: Float,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    barCount: Int = 32,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave_oscillations")

    // Continuous phase progression for sinusoidal wave rendering
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "soundwave_phase"
    )

    // Secondary harmonic phase for multi-frequency interference
    val harmonicPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (4 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "harmonic_phase"
    )

    // Breathing pulse for idle resting audio level
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Smooth transition of audio RMS level to prevent jitter while maintaining instant feedback
    val smoothedRms by animateFloatAsState(
        targetValue = audioRms.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "smoothed_rms"
    )

    val isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.PROCESSING

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sound_wave_visualizer_container")
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = when (state) {
                        AssistantState.LISTENING -> listOf(OrbCyan.copy(alpha = 0.5f), OrbBlue.copy(alpha = 0.3f), DarkBorder)
                        AssistantState.SPEAKING -> listOf(OrbMagenta.copy(alpha = 0.5f), OrbPurple.copy(alpha = 0.3f), DarkBorder)
                        AssistantState.PROCESSING -> listOf(OrbPurple.copy(alpha = 0.5f), OrbCyan.copy(alpha = 0.3f), DarkBorder)
                        else -> listOf(DarkBorder, DarkBorder)
                    }
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header feedback badge (shows live state and audio energy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when (state) {
                                        AssistantState.LISTENING -> OrbCyan
                                        AssistantState.SPEAKING -> OrbMagenta
                                        AssistantState.PROCESSING -> OrbPurple
                                        else -> Color(0xFF64748B)
                                    },
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state) {
                                AssistantState.LISTENING -> "LIVE AUDIO INPUT"
                                AssistantState.SPEAKING -> "VOICE WAVEFORM OUTPUT"
                                AssistantState.PROCESSING -> "FREQUENCY ANALYSIS"
                                else -> "AUDIO STANDBY"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when (state) {
                                    AssistantState.LISTENING -> OrbCyan
                                    AssistantState.SPEAKING -> OrbMagenta
                                    AssistantState.PROCESSING -> OrbPurple
                                    else -> TextSecondaryDark
                                },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            )
                        )
                    }

                    // Live percentage/level meter for immediate visual feedback
                    if (isActive) {
                        val percentage = if (state == AssistantState.LISTENING) {
                            (smoothedRms * 100).toInt().coerceIn(12, 99)
                        } else if (state == AssistantState.SPEAKING) {
                            ((0.45f + sin(wavePhase) * 0.3f) * 100).toInt().coerceIn(20, 95)
                        } else {
                            42
                        }
                        Text(
                            text = "$percentage% LVL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondaryDark,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Canvas rendering the dynamic sound wave & frequency spectrum
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .testTag("sound_wave_canvas")
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerY = canvasHeight / 2f
                    val totalBars = barCount

                    // 1. Equalizer Bars Calculation & Rendering
                    val totalSpacing = (totalBars - 1) * 3.dp.toPx()
                    val barWidth = ((canvasWidth - totalSpacing) / totalBars).coerceIn(2.dp.toPx(), 7.dp.toPx())
                    val step = (canvasWidth - barWidth) / (totalBars - 1)

                    val barGradients = when (state) {
                        AssistantState.LISTENING -> Brush.verticalGradient(
                            colors = listOf(OrbCyan, OrbBlue, OrbIndigo.copy(alpha = 0.4f))
                        )
                        AssistantState.SPEAKING -> Brush.verticalGradient(
                            colors = listOf(OrbMagenta, OrbPurple, OrbBlue.copy(alpha = 0.4f))
                        )
                        AssistantState.PROCESSING -> Brush.verticalGradient(
                            colors = listOf(OrbPurple, OrbCyan, Color.Transparent)
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
                        )
                    }

                    for (i in 0 until totalBars) {
                        val normalizedIdx = (i.toFloat() / (totalBars - 1)) // 0.0 to 1.0
                        // Gaussian bell curve centered at 0.5 so bars naturally peak in the middle
                        val centerFactor = exp(-((normalizedIdx - 0.5f) * (normalizedIdx - 0.5f)) / 0.08f)

                        val barHeightFraction = when (state) {
                            AssistantState.LISTENING -> {
                                val sinVariation = sin(wavePhase + i * 0.45f) * 0.35f
                                val harmonicVariation = sin(harmonicPhase - i * 0.3f) * 0.2f
                                val energy = (0.2f + smoothedRms * 0.8f + sinVariation + harmonicVariation)
                                    .coerceIn(0.12f, 1.0f)
                                (energy * centerFactor).coerceIn(0.08f, 1.0f)
                            }
                            AssistantState.SPEAKING -> {
                                val rhythm = (sin(wavePhase * 1.5f + i * 0.5f) * 0.4f +
                                        sin(harmonicPhase + i * 0.25f) * 0.3f).coerceIn(-0.4f, 0.6f)
                                val vocalEnergy = (0.45f + rhythm) * centerFactor
                                vocalEnergy.coerceIn(0.12f, 0.95f)
                            }
                            AssistantState.PROCESSING -> {
                                val sweep = sin(wavePhase * 2f - i * 0.35f) * 0.35f
                                (0.3f + sweep * 0.5f) * centerFactor.coerceIn(0.1f, 0.8f)
                            }
                            else -> {
                                (0.12f * idlePulse) * centerFactor.coerceAtLeast(0.06f)
                            }
                        }

                        val barH = (canvasHeight * barHeightFraction).coerceIn(4.dp.toPx(), canvasHeight * 0.95f)
                        val x = i * step
                        val y = centerY - (barH / 2f)

                        drawRoundRect(
                            brush = barGradients,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }

                    // 2. Fluid Waveform Ribbon Overlaid Across the Bars
                    if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                        val wavePath = Path()
                        val steps = 60
                        val dx = canvasWidth / steps
                        val waveAmplitude = if (state == AssistantState.LISTENING) {
                            (canvasHeight * 0.35f) * (0.3f + smoothedRms * 0.7f)
                        } else {
                            canvasHeight * 0.28f
                        }

                        wavePath.moveTo(0f, centerY)
                        for (j in 0..steps) {
                            val x = j * dx
                            val progress = j.toFloat() / steps
                            // Taper down at both ends to stay neat within the canvas
                            val envelope = sin(progress * PI).toFloat()
                            val angle = (progress * 4 * PI + wavePhase).toFloat()
                            val y = centerY + (sin(angle) * waveAmplitude * envelope)
                            wavePath.lineTo(x, y)
                        }

                        // Draw glowing fluid wave line
                        drawPath(
                            path = wavePath,
                            color = if (state == AssistantState.LISTENING) OrbCyan.copy(alpha = 0.85f) else OrbMagenta.copy(alpha = 0.85f),
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Secondary counter-harmonic wave ribbon
                        val wavePath2 = Path()
                        wavePath2.moveTo(0f, centerY)
                        for (j in 0..steps) {
                            val x = j * dx
                            val progress = j.toFloat() / steps
                            val envelope = sin(progress * PI).toFloat()
                            val angle = (progress * 3 * PI - wavePhase * 0.9f).toFloat()
                            val y = centerY + (sin(angle) * waveAmplitude * 0.65f * envelope)
                            wavePath2.lineTo(x, y)
                        }

                        drawPath(
                            path = wavePath2,
                            color = if (state == AssistantState.LISTENING) OrbBlue.copy(alpha = 0.65f) else OrbPurple.copy(alpha = 0.65f),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }
    }
}
