package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OrbBlue
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun SettingsDialog(
    isAutoExecuteEnabled: Boolean,
    isBackgroundServiceActive: Boolean,
    speechRate: Float,
    speechPitch: Float,
    currentApiKey: String,
    onToggleAutoExecute: (Boolean) -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempKey by remember { mutableStateOf(currentApiKey) }
    var rate by remember { mutableFloatStateOf(speechRate) }
    var pitch by remember { mutableFloatStateOf(speechPitch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("settings_dialog"),
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = OrbCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Java Assistant Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Policy Compliance Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Play Policy Compliant",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Java utilizes native Android intents, accessible live captions, zero-storage photo picker, and respects accessibility guidelines.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-execute toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Execute Intents",
                            style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark)
                        )
                        Text(
                            text = "Automatically launch intents after a 3-second countdown",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                        )
                    }
                    Switch(
                        checked = isAutoExecuteEnabled,
                        onCheckedChange = onToggleAutoExecute,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrbCyan,
                            checkedTrackColor = OrbPurple.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("auto_execute_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Service Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Hands-Free Service",
                            style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark)
                        )
                        Text(
                            text = "Stay listening for “Hey Java” with an ongoing notification even when app is minimized",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                        )
                    }
                    Switch(
                        checked = isBackgroundServiceActive,
                        onCheckedChange = onToggleBackgroundService,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrbCyan,
                            checkedTrackColor = OrbPurple.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("background_service_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speech Speed Slider
                Text(
                    text = "Speech Rate: ${"%.1f".format(rate)}x",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark)
                )
                Slider(
                    value = rate,
                    onValueChange = {
                        rate = it
                        onSpeechRateChange(it)
                    },
                    valueRange = 0.75f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = OrbCyan,
                        activeTrackColor = OrbBlue
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Speech Pitch Slider
                Text(
                    text = "Speech Pitch: ${"%.1f".format(pitch)}x",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark)
                )
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        onSpeechPitchChange(it)
                    },
                    valueRange = 0.8f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = OrbCyan,
                        activeTrackColor = OrbBlue
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini API Key Input
                Text(
                    text = "Gemini API Key (Optional)",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark)
                )
                Text(
                    text = "Leave empty to use high-speed offline NLP engine for all actions",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempKey,
                    onValueChange = { tempKey = it },
                    placeholder = { Text("Configured via Secrets panel or enter here") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Clear History Button
                Button(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Conversation History")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateApiKey(tempKey)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrbBlue, contentColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
