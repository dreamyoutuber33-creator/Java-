package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbMagenta
import com.example.ui.theme.OrbPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun LiveCaptionsCard(
    state: AssistantState,
    userQuery: String,
    partialSpeech: String,
    speechOutput: String,
    detectedLanguage: String,
    onReplaySpeech: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_captions_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated.copy(alpha = 0.92f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(OrbCyan.copy(alpha = 0.4f), OrbPurple.copy(alpha = 0.2f), DarkBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Assistant Status + Detected Language Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (state) {
                                    AssistantState.LISTENING -> OrbCyan
                                    AssistantState.PROCESSING -> OrbPurple
                                    AssistantState.SPEAKING -> OrbMagenta
                                    else -> Color(0xFF10B981)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (state) {
                            AssistantState.LISTENING -> "Listening…"
                            AssistantState.PROCESSING -> "Thinking…"
                            AssistantState.SPEAKING -> "Java is speaking"
                            AssistantState.ERROR -> "Action required"
                            else -> "Java Ready"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                if (detectedLanguage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OrbPurple.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrbPurple.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = OrbCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = detectedLanguage,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OrbCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Utterance / Live Partial Subtitles
            if (userQuery.isNotBlank() || partialSpeech.isNotBlank()) {
                Text(
                    text = if (state == AssistantState.LISTENING && partialSpeech.isNotBlank()) {
                        "“$partialSpeech”"
                    } else if (userQuery.isNotBlank()) {
                        "“$userQuery”"
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.testTag("user_utterance_text")
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Java's Clean Spoken Line (SPEECH protocol)
            if (speechOutput.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = speechOutput,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.testTag("speech_output_text")
                        )
                    }

                    // TTS Controls (Replay / Stop)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (state == AssistantState.SPEAKING) {
                            IconButton(
                                onClick = onStopSpeaking,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("stop_speech_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop speaking",
                                    tint = OrbMagenta
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onReplaySpeech,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("replay_speech_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Replay spoken line",
                                    tint = OrbCyan
                                )
                            }
                        }
                    }
                }
            } else if (state == AssistantState.IDLE) {
                Text(
                    text = "Tap the orb or say “Hey Java, Play Store par WhatsApp search karo”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
